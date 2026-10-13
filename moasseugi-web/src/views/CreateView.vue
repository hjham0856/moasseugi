<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { ApiError } from '../api/http'
import { createSession } from '../api/sessions'

const router = useRouter()

const title = ref('')
const description = ref('')
const nickname = ref('')
const pending = ref(false)
const errorMessage = ref('')

/**
 * 안건을 만들고 공유 화면으로 이동한다.
 * 실패하면 입력을 그대로 두고 서버 안내 문구만 보여준다.
 */
async function submit() {
  if (pending.value) {
    return
  }

  pending.value = true
  errorMessage.value = ''

  try {
    const response = await createSession({
      title: title.value,
      description: description.value.trim() ? description.value : null,
      nickname: nickname.value,
    })

    await router.push(`/s/${encodeURIComponent(response.session.id)}`)
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError
        ? error.message
        : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="create">
    <h1 class="create__title">안건 만들기</h1>
    <p class="create__lead">
      모아서 정할 안건을 적어 주세요. 만든 뒤 나오는 링크나 QR로 참가자를 부릅니다.
    </p>

    <form class="create__form slip" @submit.prevent="submit">
      <div class="field">
        <label for="title">제목 (필수)</label>
        <input
          id="title"
          v-model="title"
          type="text"
          required
          maxlength="200"
          autocomplete="off"
          :aria-invalid="errorMessage ? 'true' : undefined"
          :aria-describedby="errorMessage ? 'create-error' : undefined"
        />
      </div>

      <div class="field">
        <label for="description">설명 (선택)</label>
        <textarea id="description" v-model="description" />
      </div>

      <div class="field">
        <label for="nickname">진행자 닉네임 (필수)</label>
        <input
          id="nickname"
          v-model="nickname"
          type="text"
          required
          maxlength="30"
          autocomplete="nickname"
          :aria-invalid="errorMessage ? 'true' : undefined"
          :aria-describedby="errorMessage ? 'create-error' : undefined"
        />
      </div>

      <p v-if="errorMessage" id="create-error" class="field-error" role="alert">
        {{ errorMessage }}
      </p>

      <button type="submit" class="button create__submit" :disabled="pending">
        {{ pending ? '만드는 중…' : '안건 만들기' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.create {
  max-width: 40rem;
  padding: var(--space-8) var(--space-5) var(--space-16);
}

.create__title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--text-2xl);
}

.create__lead {
  margin-top: var(--space-3);
  color: var(--color-ink-2);
}

.create__form {
  display: grid;
  gap: var(--space-5);
  margin-top: var(--space-8);
  padding: var(--space-6);
}

.field {
  display: grid;
  gap: var(--space-2);
}

.create__submit {
  justify-self: start;
}

@media (min-width: 48rem) {
  .create {
    padding: var(--space-12) var(--space-10) var(--space-20);
  }
}
</style>
