<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { ApiError } from '../api/http'
import { getStoredParticipant, type StoredParticipant } from '../api/participant-storage'
import { getSession, joinSession, type SessionDetail, type SessionStatus } from '../api/sessions'
import SharePanel from '../components/SharePanel.vue'

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

/** 단계별 안내. 실제 작성·평가·결과 기능은 이후 이슈에서 이 자리에 붙는다. */
const phaseNotice: Record<SessionStatus, string> = {
  WRITING: '작성 단계입니다. 다른 참가자의 아이디어는 보이지 않으며, 곧 여기서 자기 아이디어를 쓰게 됩니다.',
  EVALUATING: '평가 단계입니다. 작성자를 가린 아이디어에 반응을 남기는 단계입니다.',
  RESULT: '결과 단계입니다. 평가가 끝났고 진행자가 하나를 채택합니다.',
}

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

      <section v-if="stored" class="member slip" aria-labelledby="member-title">
        <h2 id="member-title" class="member__name">
          {{ stored.participant.nickname }}님으로 참가 중
          <span v-if="isHost" class="member__badge">진행자</span>
        </h2>
        <p class="member__notice">{{ phaseNotice[session.status] }}</p>
      </section>

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

.member,
.join,
.closed {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
}

.member__name {
  font-size: var(--text-l);
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

.member__notice {
  color: var(--color-ink-2);
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
