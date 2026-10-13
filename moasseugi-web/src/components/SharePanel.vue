<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createSessionQrDataUrl, createSessionShareUrl } from '../share/session-share'

/**
 * 진행자가 참가자를 부르는 영역. QR과 링크에는 안건 주소만 담기며
 * 참가 키는 어디에도 포함되지 않는다.
 */
const props = defineProps<{ sessionId: string }>()

const shareUrl = createSessionShareUrl(props.sessionId)
const qrDataUrl = ref('')
const urlInput = ref<HTMLInputElement | null>(null)
const copyMessage = ref('')

onMounted(async () => {
  try {
    qrDataUrl.value = await createSessionQrDataUrl(props.sessionId)
  } catch {
    // QR을 못 만들어도 링크 복사로 참가를 안내할 수 있으므로 조용히 둔다.
  }
})

/** 클립보드 접근이 막힌 브라우저에서는 입력칸을 선택해 직접 복사하도록 안내한다. */
async function copyUrl() {
  try {
    await navigator.clipboard.writeText(shareUrl)
    copyMessage.value = '복사했습니다.'
  } catch {
    urlInput.value?.select()
    copyMessage.value = '복사하지 못했습니다. 선택된 주소를 직접 복사해 주세요.'
  }
}
</script>

<template>
  <section class="share slip" aria-labelledby="share-title">
    <h2 id="share-title" class="share__title">참가자 부르기</h2>

    <div class="share__body">
      <div class="share__qr">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="안건 참가 링크 QR 코드" width="176" height="176" />
      </div>

      <div class="share__link">
        <label for="share-url">공유 링크</label>
        <input id="share-url" ref="urlInput" type="text" readonly :value="shareUrl" />
        <button type="button" class="button button--quiet" @click="copyUrl">링크 복사</button>
        <p v-if="copyMessage" class="share__message" role="status">{{ copyMessage }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.share {
  padding: var(--space-6);
}

.share__title {
  font-size: var(--text-l);
}

.share__body {
  display: grid;
  gap: var(--space-6);
  margin-top: var(--space-5);
}

.share__qr {
  width: 11rem;
  height: 11rem;
  background: var(--color-slip);
}

.share__qr img {
  display: block;
  width: 100%;
  height: 100%;
}

.share__link {
  display: grid;
  gap: var(--space-2);
  align-content: start;
  justify-items: start;
}

.share__link input {
  font-family: var(--font-mono);
  font-size: var(--text-s);
}

.share__link .button {
  margin-top: var(--space-2);
}

.share__message {
  font-size: var(--text-s);
  color: var(--color-ink-2);
}

@media (min-width: 40rem) {
  .share__body {
    grid-template-columns: auto minmax(0, 1fr);
    align-items: start;
  }
}
</style>
