<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getStoredParticipant } from '../api/participant-storage'
import { getSession, type SessionDetail } from '../api/sessions'
import SharePanel from '../components/SharePanel.vue'

const route = useRoute()
const sessionId = computed(() => String(route.params.sessionId))

const session = ref<SessionDetail | null>(null)
const stored = ref(getStoredParticipant(sessionId.value))
const isHost = computed(() => stored.value?.participant.isHost === true)

// 임시 표시: 진입·참가 처리는 다음 커밋에서 확장한다.
getSession(sessionId.value).then((detail) => {
  session.value = detail
})
</script>

<template>
  <div class="session">
    <h1 v-if="session" class="session__title">{{ session.title }}</h1>
    <SharePanel v-if="isHost" :session-id="sessionId" />
  </div>
</template>

<style scoped>
.session {
  display: grid;
  gap: var(--space-6);
  max-width: 44rem;
  padding: var(--space-8) var(--space-5) var(--space-16);
}

.session__title {
  font-size: var(--text-2xl);
  overflow-wrap: anywhere;
}
</style>
