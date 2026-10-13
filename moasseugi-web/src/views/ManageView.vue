<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { ApiError } from '../api/http'
import { getStoredParticipant, type ParticipantSummary } from '../api/participant-storage'
import {
  closeEvaluation,
  closeWriting,
  getSession,
  getSessionParticipants,
  type SessionDetail,
} from '../api/sessions'
import SharePanel from '../components/SharePanel.vue'

/**
 * 진행자 관리 화면.
 * 참가자를 부르고(QR·링크), 누가 들어왔는지 확인하고, 다음 단계로 넘기는 일만 모았다.
 * 진행자도 참가자라 글을 쓰고 평가해야 하는데, 그 화면에 진행 조작이 섞이면
 * 회의 중 실수로 단계를 넘기기 쉬우므로 안건 화면(/s/:sessionId)과 분리했다.
 * 완료 인원 집계나 타이머는 두지 않는다. 언제 넘길지는 진행자가 회의 상황을 보고 정한다.
 */
const route = useRoute()
const sessionId = computed(() => String(route.params.sessionId))

const session = ref<SessionDetail | null>(null)
const participants = ref<ParticipantSummary[]>([])
const loading = ref(true)
const loadError = ref('')

/** 보관된 참가 정보로 진행자 여부를 안내한다. 실제 권한은 서버가 참가 키로 다시 확인한다. */
const isHost = ref(false)

const transitionConfirm = ref<'closeWriting' | 'closeEvaluation' | null>(null)
const transitioning = ref(false)
const transitionError = ref('')

/** 다른 화면과 같은 3초 간격. 새 참가자와 단계 변화를 진행자가 바로 볼 수 있으면 충분하다. */
const POLL_INTERVAL_MS = 3000

let pollTimer: ReturnType<typeof setInterval> | undefined
let polling = false

function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return '안건을 찾을 수 없습니다. 링크가 맞는지 확인해 주세요.'
    }

    return error.message
  }

  return '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
}

/** 안건과 참가자 목록을 불러온다. 진행자가 아니면 진행자 전용 API는 부르지 않는다. */
async function load(id: string) {
  loading.value = true
  loadError.value = ''
  session.value = null
  participants.value = []
  transitionError.value = ''
  isHost.value = getStoredParticipant(id)?.participant.isHost === true

  try {
    const detail = await getSession(id)

    if (id !== sessionId.value) {
      return
    }

    session.value = detail

    if (isHost.value) {
      participants.value = await getSessionParticipants(id)
    }
  } catch (error) {
    if (id === sessionId.value) {
      loadError.value = describeError(error)
    }
  } finally {
    if (id === sessionId.value) {
      loading.value = false
    }
  }
}

watch(sessionId, load, { immediate: true })

function stopPolling() {
  clearInterval(pollTimer)
  pollTimer = undefined
}

/**
 * 단계와 참가자 목록을 조용히 갱신한다. 이전 조회가 끝나기 전의 주기는 건너뛰고,
 * 일시적인 실패는 다음 주기에 다시 시도한다. 화면이 바뀐 뒤 늦게 온 응답은 버린다.
 * 참가는 작성 단계에서만 가능하므로 평가가 시작되면 목록이 더 늘지 않아 목록 조회를 멈춘다.
 */
async function poll(id: string) {
  if (polling) {
    return
  }

  polling = true

  try {
    const detail = await getSession(id)

    if (id !== sessionId.value) {
      return
    }

    session.value = detail

    if (detail.status === 'WRITING') {
      const latest = await getSessionParticipants(id)

      if (id === sessionId.value) {
        participants.value = latest
      }
    }
  } catch {
    // 다음 주기에 다시 조회한다.
  } finally {
    polling = false
  }
}

/** 진행자 화면이 열려 있는 동안만 갱신한다. 결과 단계는 더 바뀌지 않으므로 멈춘다. */
watch(
  [sessionId, isHost, () => session.value !== null && session.value.status !== 'RESULT'],
  ([id, host, open]) => {
    stopPolling()

    if (host && open) {
      pollTimer = setInterval(() => poll(id), POLL_INTERVAL_MS)
    }
  },
  { immediate: true },
)

onUnmounted(stopPolling)

watch(
  () => session.value?.status,
  () => {
    transitionConfirm.value = null
    transitionError.value = ''
  },
)

/**
 * 단계를 넘긴다. 되돌릴 수 없어 화면에서 한 번 더 확인받은 뒤에만 호출한다.
 * 거부(예: 아이디어가 하나도 없는 평가 시작)는 서버 문구를 그대로 보여준다.
 */
async function transition() {
  const action = transitionConfirm.value

  if (!action || transitioning.value) {
    return
  }

  const id = sessionId.value

  transitioning.value = true
  transitionError.value = ''

  try {
    const detail = action === 'closeWriting' ? await closeWriting(id) : await closeEvaluation(id)

    if (id === sessionId.value) {
      session.value = detail
      transitionConfirm.value = null
    }

    // 작성 마감 직전에 들어온 참가자도 목록에 남도록 마지막으로 한 번 더 조회한다.
    // 이 조회의 실패는 전환 실패가 아니므로 오류로 알리지 않는다.
    if (action === 'closeWriting') {
      getSessionParticipants(id)
        .then((latest) => {
          if (id === sessionId.value) {
            participants.value = latest
          }
        })
        .catch(() => {})
    }
  } catch (error) {
    if (id === sessionId.value) {
      transitionError.value = describeError(error)
    }
  } finally {
    transitioning.value = false
  }
}
</script>

<template>
  <div class="manage">
    <p v-if="loading" class="manage__status" role="status">불러오는 중…</p>

    <p v-else-if="loadError" class="field-error manage__problem" role="alert">{{ loadError }}</p>

    <template v-else-if="session">
      <header class="manage__head">
        <h1 class="manage__title">{{ session.title }}</h1>
        <RouterLink class="button button--quiet manage__back" :to="`/s/${sessionId}`">
          안건 화면으로
        </RouterLink>
      </header>

      <section v-if="!isHost" class="denied slip">
        <h2 class="denied__title">진행자만 볼 수 있는 화면입니다</h2>
        <p>안건 화면에서 참가하거나 결과를 확인할 수 있습니다.</p>
      </section>

      <template v-else>
        <SharePanel :session-id="sessionId" large />

        <section class="people" aria-labelledby="people-title">
          <h2 id="people-title" class="people__title">참가자 {{ participants.length }}명</h2>

          <ul class="people__list">
            <li v-for="person in participants" :key="person.id" class="people__item">
              <span class="people__name">{{ person.nickname }}</span>
              <span v-if="person.isHost" class="people__badge">진행자</span>
            </li>
          </ul>
        </section>

        <section class="control" aria-labelledby="control-title">
          <h2 id="control-title" class="control__title">단계 전환</h2>

          <p v-if="session.status === 'RESULT'" class="control__done">
            결과 단계입니다. 안건 화면에서 결과를 보고 아이디어를 채택하세요.
          </p>

          <template v-else-if="transitionConfirm === null">
            <button
              type="button"
              class="button button--quiet"
              @click="transitionConfirm = session.status === 'WRITING' ? 'closeWriting' : 'closeEvaluation'"
            >
              {{ session.status === 'WRITING' ? '평가 시작' : '평가 종료' }}
            </button>
          </template>

          <template v-else>
            <p class="control__warning">
              {{
                transitionConfirm === 'closeWriting'
                  ? '되돌릴 수 없습니다. 평가를 시작할까요?'
                  : '되돌릴 수 없습니다. 평가를 끝내고 결과 단계로 넘어갈까요?'
              }}
            </p>

            <div class="control__actions">
              <button type="button" class="button" :disabled="transitioning" @click="transition">
                {{ transitioning ? '전환하는 중…' : '확인' }}
              </button>
              <button
                type="button"
                class="button button--quiet"
                :disabled="transitioning"
                @click="transitionConfirm = null"
              >
                취소
              </button>
            </div>
          </template>

          <p v-if="transitionError" class="field-error" role="alert">{{ transitionError }}</p>
        </section>
      </template>
    </template>
  </div>
</template>

<style scoped>
.manage {
  display: grid;
  gap: var(--space-6);
  max-width: 44rem;
  padding: var(--space-8) var(--space-5) var(--space-16);
}

.manage__status {
  color: var(--color-ink-2);
}

.manage__problem {
  font-size: var(--text-m);
}

.manage__head {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

/* 제목은 사용자 입력이므로 본문 글꼴로 쓰고 줄바꿈을 허용한다. */
.manage__title {
  font-size: var(--text-2xl);
  overflow-wrap: anywhere;
}

.denied {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
}

.denied__title {
  font-size: var(--text-l);
}

.people {
  display: grid;
  gap: var(--space-3);
}

.people__title,
.control__title {
  font-size: var(--text-l);
}

.people__list {
  display: grid;
  gap: var(--space-2);
  list-style: none;
  padding: 0;
}

.people__item {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  padding-bottom: var(--space-2);
  border-bottom: var(--rule-width) solid var(--color-rule);
}

.people__name {
  min-width: 0;
  overflow-wrap: anywhere;
}

.people__badge {
  padding: 0 var(--space-2);
  background: var(--color-marker);
  font-size: var(--text-s);
  white-space: nowrap;
}

.control {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.control__done {
  color: var(--color-ink-2);
}

.control__warning {
  font-weight: 500;
}

.control__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

@media (min-width: 48rem) {
  .manage {
    padding: var(--space-12) var(--space-10) var(--space-20);
  }
}
</style>
