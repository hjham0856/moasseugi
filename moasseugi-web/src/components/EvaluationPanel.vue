<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  getEvaluationItems,
  putEvaluation,
  type EvaluationItem,
  type EvaluationValue,
} from '../api/evaluations'
import { ApiError } from '../api/http'

/**
 * 평가 단계에서 작성자를 가린 아이디어에 좋음·괜찮음·별로를 고르는 영역.
 * 작성자, 다른 사람의 평가, 중간 집계는 서버가 보내지 않으며 이 화면도 만들지 않는다.
 * 제출 버튼 없이 고르는 즉시 항목별로 저장한다.
 */
const props = defineProps<{ sessionId: string }>()

interface ItemState extends EvaluationItem {
  /** 이 항목의 저장 요청이 진행 중인지 */
  saving: boolean
  error: string
}

const CHOICES: { value: EvaluationValue; label: string; modifier: string }[] = [
  { value: 1, label: '좋음', modifier: 'good' },
  { value: 0, label: '괜찮음', modifier: 'okay' },
  { value: -1, label: '별로', modifier: 'poor' },
]

const items = ref<ItemState[]>([])
const loading = ref(true)
const loadError = ref('')

function describeError(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
}

async function load() {
  loading.value = true
  loadError.value = ''

  try {
    const loaded = await getEvaluationItems(props.sessionId)

    items.value = loaded.map((item) => ({ ...item, saving: false, error: '' }))
  } catch (error) {
    loadError.value = describeError(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

/**
 * 고른 값을 저장하고, 서버가 돌려준 값을 선택 상태로 반영한다.
 * 저장이 끝나기 전에는 선택 표시를 바꾸지 않아 저장되지 않은 선택을 저장된 것으로 오인하지 않게 한다.
 * 진행 중인 항목의 버튼만 막아, 같은 항목에 요청이 겹쳐 나중 응답이 앞선 선택을 덮는 일을 없애고
 * 다른 항목은 계속 평가할 수 있게 한다.
 */
async function choose(item: ItemState, value: EvaluationValue) {
  if (item.saving) {
    return
  }

  item.saving = true
  item.error = ''

  try {
    const saved = await putEvaluation(props.sessionId, item.ideaId, value)

    item.myEvaluation = saved.value
  } catch (error) {
    item.error = describeError(error)
  } finally {
    item.saving = false
  }
}
</script>

<template>
  <section class="evaluation" aria-labelledby="evaluation-title">
    <h2 id="evaluation-title" class="evaluation__title">아이디어 평가</h2>
    <p class="evaluation__hint">누가 썼는지는 보이지 않습니다. 고르면 바로 저장되고, 결과 단계 전까지 바꿀 수 있습니다.</p>

    <p v-if="loading" class="evaluation__state" role="status">평가할 아이디어를 불러오는 중…</p>

    <div v-else-if="loadError" class="evaluation__load-error">
      <p class="field-error" role="alert">{{ loadError }}</p>
      <button type="button" class="button button--quiet" @click="load">다시 불러오기</button>
    </div>

    <p v-else-if="items.length === 0" class="evaluation__state">
      평가할 아이디어가 없습니다. 결과 단계로 넘어갈 때까지 기다려 주세요.
    </p>

    <ol v-else class="evaluation__list">
      <li v-for="(item, index) in items" :key="item.ideaId" class="idea slip">
        <p class="idea__no mono">{{ index + 1 }}</p>
        <p class="idea__content">{{ item.content }}</p>

        <div class="idea__choices" role="group" :aria-label="`아이디어 ${index + 1} 평가`">
          <button
            v-for="choice in CHOICES"
            :key="choice.value"
            type="button"
            class="choice"
            :class="`choice--${choice.modifier}`"
            :aria-pressed="item.myEvaluation === choice.value"
            :disabled="item.saving"
            @click="choose(item, choice.value)"
          >
            <span v-if="item.myEvaluation === choice.value" class="choice__check" aria-hidden="true">✓</span>
            {{ choice.label }}
          </button>
        </div>

        <p v-if="item.saving" class="idea__note" role="status">저장하는 중…</p>
        <p v-else-if="item.error" class="field-error idea__note" role="alert">{{ item.error }}</p>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.evaluation {
  display: grid;
  gap: var(--space-4);
}

.evaluation__title {
  font-size: var(--text-xl);
}

.evaluation__hint,
.evaluation__state {
  color: var(--color-ink-2);
  font-size: var(--text-s);
}

.evaluation__load-error {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.evaluation__list {
  display: grid;
  gap: var(--space-5);
}

.idea {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-5);
}

.idea__no {
  color: var(--color-ink-3);
  font-size: var(--text-s);
}

.idea__content {
  max-width: none;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* 세 선택지는 같은 폭의 열로 두어 320px에서도 한 줄에 들어간다. */
.idea__choices {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.idea__note {
  margin: 0;
  color: var(--color-ink-2);
  font-size: var(--text-s);
}

.choice {
  padding: var(--space-2) var(--space-1);
  background: var(--color-slip);
  color: var(--color-ink);
  font: inherit;
  font-size: var(--text-s);
  font-weight: 500;
  white-space: nowrap;
  border: var(--rule-width) solid var(--color-ink-3);
  border-radius: var(--radius-s);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.choice:hover {
  border-color: var(--color-ink);
}

.choice:active {
  transform: translateY(1px);
}

/* 선택 상태는 색 채움에 더해 굵은 글자와 ✓ 표시로도 구분한다. */
.choice[aria-pressed='true'] {
  border-color: var(--color-ink);
  font-weight: 700;
}

.choice--good[aria-pressed='true'] {
  background: var(--color-accent);
}

.choice--okay[aria-pressed='true'] {
  background: var(--color-marker);
}

.choice--poor[aria-pressed='true'] {
  background: var(--color-cool);
}

/* 저장 중에는 막히되, 이미 확정된 선택은 알아볼 수 있게 채움을 유지한다. */
.choice:disabled {
  cursor: progress;
  color: var(--color-ink-2);
  transform: none;
}

.choice:disabled:hover {
  border-color: var(--color-ink-3);
}

.choice[aria-pressed='true']:disabled {
  color: var(--color-ink);
  border-color: var(--color-ink);
}
</style>
