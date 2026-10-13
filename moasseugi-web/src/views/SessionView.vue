<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { ApiError } from '../api/http'
import { getStoredParticipant, type StoredParticipant } from '../api/participant-storage'
import {
  closeEvaluation,
  closeWriting,
  getSession,
  joinSession,
  type SessionDetail,
  type SessionStatus,
} from '../api/sessions'
import EvaluationPanel from '../components/EvaluationPanel.vue'
import SharePanel from '../components/SharePanel.vue'
import WritingPanel from '../components/WritingPanel.vue'

/**
 * 공유 링크와 QR의 도착점.
 * 안건은 참가 키 없이 공개 조회하고, 이 브라우저에 보관된 참가 정보가 있으면 참가자로 보여준다.
 * 화면의 분기는 안내일 뿐이며 허용 여부는 각 API가 서버에서 다시 판단한다.
 */
const route = useRoute()
const sessionId = computed(() => String(route.params.sessionId))

const session = ref<SessionDetail | null>(null)
const loading = ref(true)
const loadError = ref('')
const stored = ref<StoredParticipant | null>(null)

const nickname = ref('')
const joining = ref(false)
const joinError = ref('')

const isHost = computed(() => stored.value?.participant.isHost === true)

/** 작성 → 평가 → 결과. 되돌릴 수 없는 순서이므로 번호로 현재 위치를 보여준다. */
const PHASES: { status: SessionStatus; label: string }[] = [
  { status: 'WRITING', label: '작성' },
  { status: 'EVALUATING', label: '평가' },
  { status: 'RESULT', label: '결과' },
]

/**
 * 다른 브라우저에서 진행자가 단계를 바꾸면 이 화면도 따라가야 하므로 3초마다 안건을 다시 조회한다.
 * 회의 중 몇 초 안의 반영이면 충분하고 타이머·WebSocket 없이 단순하게 유지할 수 있어 폴링을 택했다.
 */
const POLL_INTERVAL_MS = 3000

let pollTimer: ReturnType<typeof setInterval> | undefined
let polling = false

const transitionConfirm = ref<'closeWriting' | 'closeEvaluation' | null>(null)
const transitioning = ref(false)
const transitionError = ref('')

const phaseIndex = computed(() =>
  PHASES.findIndex((phase) => phase.status === session.value?.status),
)

/** 조회 실패 상태를 사용자에게 구분해 안내한다. */
function describeLoadError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return '안건을 찾을 수 없습니다. 링크가 맞는지 확인해 주세요.'
    }

    if (error.status === 400) {
      return '올바르지 않은 링크입니다. 주소를 다시 확인해 주세요.'
    }

    return error.message
  }

  return '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
}

/** 안건을 불러온다. 링크가 바뀌어 같은 화면이 재사용될 때도 다시 호출된다. */
async function load(id: string) {
  loading.value = true
  loadError.value = ''
  session.value = null
  joinError.value = ''
  stored.value = getStoredParticipant(id)

  try {
    const detail = await getSession(id)

    if (id === sessionId.value) {
      session.value = detail
    }
  } catch (error) {
    if (id === sessionId.value) {
      loadError.value = describeLoadError(error)
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
 * 단계만 갱신한다. 화면의 나머지는 그대로 두므로 조회 때마다 로딩 표시가 깜빡이지 않는다.
 * 일시적인 실패는 다음 주기에 다시 시도하면 되므로 조용히 넘기고, 응답이 늦게 와서
 * 이미 다른 안건으로 이동한 뒤라면 버린다. 되돌릴 수 없는 마지막 단계에 닿으면 멈춘다.
 */
async function poll(id: string) {
  if (polling) {
    return
  }

  polling = true

  try {
    const detail = await getSession(id)

    if (id === sessionId.value && session.value) {
      session.value = detail

      if (detail.status === 'RESULT') {
        stopPolling()
      }
    }
  } catch {
    // 다음 주기에 다시 조회한다.
  } finally {
    polling = false
  }
}

/** 안건이 열려 있고 이 브라우저가 참가자일 때만 갱신한다. 안건이 바뀌면 이전 타이머를 정리하고 다시 시작한다. */
watch(
  [sessionId, () => session.value !== null && session.value.status !== 'RESULT', stored],
  ([id, open, participant]) => {
    stopPolling()
    transitionConfirm.value = null
    transitionError.value = ''

    if (open && participant) {
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

/** 닉네임으로 참가한다. 실패하면 입력을 유지하고 서버 안내 문구를 보여준다. */
async function join() {
  if (joining.value) {
    return
  }

  joining.value = true
  joinError.value = ''

  try {
    const response = await joinSession(sessionId.value, nickname.value)
    stored.value = response
  } catch (error) {
    joinError.value =
      error instanceof ApiError
        ? error.message
        : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
  } finally {
    joining.value = false
  }
}

/**
 * 진행자가 단계를 넘긴다. 되돌릴 수 없어 화면에서 한 번 더 확인받은 뒤에만 호출한다.
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
  } catch (error) {
    if (id === sessionId.value) {
      transitionError.value =
        error instanceof ApiError
          ? error.message
          : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
    }
  } finally {
    transitioning.value = false
  }
}
</script>

<template>
  <div class="session">
    <p v-if="loading" class="session__status" role="status">안건을 불러오는 중…</p>

    <p v-else-if="loadError" class="field-error session__problem" role="alert">{{ loadError }}</p>

    <template v-else-if="session">
      <header class="session__head">
        <h1 class="session__title">{{ session.title }}</h1>
        <p v-if="session.description" class="session__description">{{ session.description }}</p>
      </header>

      <template v-if="stored">
        <p class="member">
          <span class="member__name">{{ stored.participant.nickname }}님으로 참가 중</span>
          <span v-if="isHost" class="member__badge">진행자</span>
        </p>

        <ol class="phases" aria-label="진행 단계">
          <li
            v-for="(phase, index) in PHASES"
            :key="phase.status"
            class="phases__item"
            :class="{
              'phases__item--current': index === phaseIndex,
              'phases__item--done': index < phaseIndex,
            }"
            :aria-current="index === phaseIndex ? 'step' : undefined"
          >
            <span class="phases__no mono">{{ index + 1 }}</span>
            {{ phase.label }}
          </li>
        </ol>

        <WritingPanel v-if="session.status === 'WRITING'" :key="sessionId" :session-id="sessionId" />

        <EvaluationPanel
          v-else-if="session.status === 'EVALUATING'"
          :key="sessionId"
          :session-id="sessionId"
        />

        <section v-else class="closed slip" aria-labelledby="result-title">
          <h2 id="result-title" class="closed__title">결과 단계가 시작되었습니다</h2>
          <p>작성과 평가가 끝나 더 이상 입력할 수 없습니다.</p>
        </section>

        <section
          v-if="isHost && session.status !== 'RESULT'"
          class="control"
          aria-labelledby="control-title"
        >
          <h2 id="control-title" class="control__title">진행자 조작</h2>

          <template v-if="transitionConfirm === null">
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

      <form v-else-if="session.status === 'WRITING'" class="join slip" @submit.prevent="join">
        <h2 class="join__title">닉네임으로 참가</h2>

        <div class="join__field">
          <label for="join-nickname">닉네임 (필수)</label>
          <input
            id="join-nickname"
            v-model="nickname"
            type="text"
            required
            maxlength="30"
            autocomplete="nickname"
            :aria-invalid="joinError ? 'true' : undefined"
            :aria-describedby="joinError ? 'join-error' : undefined"
          />
        </div>

        <p v-if="joinError" id="join-error" class="field-error" role="alert">{{ joinError }}</p>

        <button type="submit" class="button join__submit" :disabled="joining">
          {{ joining ? '참가하는 중…' : '참가하기' }}
        </button>
      </form>

      <section v-else class="closed slip">
        <h2 class="closed__title">작성이 끝났습니다</h2>
        <p>작성 단계가 끝나 새로 참가할 수 없습니다.</p>
      </section>

      <SharePanel v-if="isHost" :session-id="sessionId" />
    </template>
  </div>
</template>

<style scoped>
.session {
  display: grid;
  gap: var(--space-6);
  max-width: 44rem;
  padding: var(--space-8) var(--space-5) var(--space-16);
}

.session__status {
  color: var(--color-ink-2);
}

.session__problem {
  font-size: var(--text-m);
}

.session__head {
  display: grid;
  gap: var(--space-3);
}

/* 제목은 사용자 입력(최대 200자)이므로 디스플레이 글꼴 대신 본문 글꼴로 쓰고 줄바꿈을 허용한다. */
.session__title {
  font-size: var(--text-2xl);
  overflow-wrap: anywhere;
}

.session__description {
  color: var(--color-ink-2);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.join,
.closed {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
}

.member {
  max-width: none;
  font-size: var(--text-l);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.member__badge {
  display: inline-block;
  margin-left: var(--space-2);
  padding: 0 var(--space-2);
  background: var(--color-marker);
  font-size: var(--text-s);
  white-space: nowrap;
  vertical-align: middle;
}

/* 지나온 단계는 옅게, 지금 단계는 형광펜 채움과 굵은 글자로 구분한다. */
.phases {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.phases__item {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-1);
  color: var(--color-ink-3);
  font-size: var(--text-s);
  white-space: nowrap;
  border-bottom: 2px solid var(--color-rule);
}

.phases__item--done {
  color: var(--color-ink-2);
}

.phases__item--current {
  background: var(--color-marker);
  color: var(--color-ink);
  font-weight: 700;
  border-bottom-color: var(--color-ink);
}

.phases__no {
  font-size: var(--text-xs);
}

.control {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.control__title {
  font-size: var(--text-s);
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

.join__title,
.closed__title {
  font-size: var(--text-l);
}

.join__field {
  display: grid;
  gap: var(--space-2);
}

.join__submit {
  justify-self: start;
}

@media (min-width: 48rem) {
  .session {
    padding: var(--space-12) var(--space-10) var(--space-20);
  }
}
</style>
