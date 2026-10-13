<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import { ApiError } from '../api/http'
import { getResults, selectIdea, type ResultResponse } from '../api/results'

/**
 * 결과 단계의 아이디어 목록.
 * 점수나 순위로 줄이지 않고 좋음·괜찮음·별로가 어떻게 나뉘었는지를 그대로 보여 주며,
 * 어느 것을 고를지는 사람이 정한다. 그래서 정렬도 평가가 아닌 생성순을 따른다.
 * 결과는 링크를 가진 누구에게나 공개되므로 참가 키 없이 조회하고,
 * 채택 버튼은 진행자에게만 보인다(허용 여부는 서버가 다시 판단한다).
 */
const props = defineProps<{
  sessionId: string
  isHost: boolean
  /** 폴링으로 알게 된 채택 여부. 값이 바뀌면 다른 브라우저의 채택이므로 결과를 다시 불러온다. */
  selectedIdeaId: string | null
}>()

const result = ref<ResultResponse | null>(null)
const loading = ref(true)
const loadError = ref('')

const confirmingId = ref<string | null>(null)
const selecting = ref(false)
const selectError = ref('')

function describeError(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
}

/** 결과를 불러온다. 처음 한 번만 로딩 표시를 쓰고, 이후 갱신은 화면을 그대로 둔 채 바꾼다. */
async function load() {
  loadError.value = ''

  try {
    result.value = await getResults(props.sessionId)
  } catch (error) {
    // 이미 보여 줄 결과가 있으면 갱신 실패로 화면을 지우지 않는다.
    if (!result.value) {
      loadError.value = describeError(error)
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(
  () => props.selectedIdeaId,
  (selectedIdeaId) => {
    if (selectedIdeaId !== (result.value?.selectedIdeaId ?? null)) {
      confirmingId.value = null
      load()
    }
  },
)

/**
 * 진행자가 아이디어 하나를 채택한다. 되돌릴 수 없어 확인을 한 번 더 받은 뒤에만 호출한다.
 * 이미 채택됐거나 권한이 없다는 등의 거부는 서버 문구를 보여 주고 최신 결과로 맞춘다.
 */
async function adopt(ideaId: string) {
  if (selecting.value) {
    return
  }

  selecting.value = true
  selectError.value = ''

  try {
    result.value = await selectIdea(props.sessionId, ideaId)
    confirmingId.value = null
  } catch (error) {
    selectError.value = describeError(error)
    confirmingId.value = null
    await load()
  } finally {
    selecting.value = false
  }
}

function startConfirm(ideaId: string) {
  selectError.value = ''
  confirmingId.value = ideaId
}
</script>

<template>
  <section class="result" aria-labelledby="result-title">
    <h2 id="result-title" class="result__title">결과</h2>

    <p v-if="loading" class="result__status" role="status">결과를 불러오는 중…</p>

    <template v-else-if="loadError">
      <p class="field-error" role="alert">{{ loadError }}</p>
      <button type="button" class="button button--quiet result__retry" @click="load">
        다시 불러오기
      </button>
    </template>

    <template v-else-if="result">
      <p class="result__guide">
        {{
          result.selectedIdeaId
            ? '진행자가 아이디어를 채택했습니다.'
            : '평가가 어떻게 나뉘었는지 보고 이야기해 보세요.'
        }}
      </p>

      <p v-if="selectError" class="field-error" role="alert">{{ selectError }}</p>

      <ul class="ideas">
        <li
          v-for="idea in result.ideas"
          :key="idea.ideaId"
          class="idea slip"
          :class="{ 'idea--selected': idea.selected }"
        >
          <p v-if="idea.selected" class="idea__stamp">채택</p>

          <p class="idea__content">{{ idea.content }}</p>

          <p v-if="idea.selected && idea.authorNickname" class="idea__author">
            작성: {{ idea.authorNickname }}
          </p>

          <p v-if="idea.distribution.total === 0" class="idea__empty">평가 없음</p>

          <template v-else>
            <div class="bar" aria-hidden="true">
              <span
                v-if="idea.distribution.positive > 0"
                class="bar__segment bar__segment--positive"
                :style="{ flexGrow: idea.distribution.positive }"
              ></span>
              <span
                v-if="idea.distribution.neutral > 0"
                class="bar__segment bar__segment--neutral"
                :style="{ flexGrow: idea.distribution.neutral }"
              ></span>
              <span
                v-if="idea.distribution.negative > 0"
                class="bar__segment bar__segment--negative"
                :style="{ flexGrow: idea.distribution.negative }"
              ></span>
            </div>

            <p class="idea__counts mono">
              <span>좋음 {{ idea.distribution.positive }}</span>
              <span>괜찮음 {{ idea.distribution.neutral }}</span>
              <span>별로 {{ idea.distribution.negative }}</span>
              <span>총 {{ idea.distribution.total }}</span>
            </p>
          </template>

          <div v-if="isHost && result.selectedIdeaId === null" class="adopt">
            <template v-if="confirmingId !== idea.ideaId">
              <button
                type="button"
                class="button button--quiet"
                :disabled="selecting"
                @click="startConfirm(idea.ideaId)"
              >
                채택하기
              </button>
            </template>

            <template v-else>
              <p class="adopt__warning">채택은 바꿀 수 없습니다. 이 아이디어를 채택할까요?</p>

              <div class="adopt__actions">
                <button type="button" class="button" :disabled="selecting" @click="adopt(idea.ideaId)">
                  {{ selecting ? '채택하는 중…' : '확인' }}
                </button>
                <button
                  type="button"
                  class="button button--quiet"
                  :disabled="selecting"
                  @click="confirmingId = null"
                >
                  취소
                </button>
              </div>
            </template>
          </div>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.result {
  display: grid;
  gap: var(--space-4);
}

.result__title {
  font-size: var(--text-xl);
}

.result__status,
.result__guide {
  color: var(--color-ink-2);
}

.result__retry {
  justify-self: start;
}

.ideas {
  display: grid;
  gap: var(--space-5);
  list-style: none;
  padding: 0;
}

.idea {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-5);
}

/* 채택작은 테두리 두께가 아닌 형광펜 면과 라벨로 구분한다. */
.idea--selected {
  background: var(--color-marker);
}

.idea__stamp {
  justify-self: start;
  padding: 0 var(--space-2);
  border: var(--rule-width) solid var(--color-accent-ink);
  border-radius: var(--radius-s);
  color: var(--color-accent-ink);
  font-size: var(--text-s);
  font-weight: 700;
  white-space: nowrap;
}

.idea__content {
  max-width: none;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.idea__author {
  max-width: none;
  font-size: var(--text-s);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.idea__empty {
  color: var(--color-ink-2);
  font-size: var(--text-s);
}

/* 막대는 비율만 보여 주고, 정확한 수는 아래 글자가 전한다. */
.bar {
  display: flex;
  height: var(--space-3);
  overflow: hidden;
  border: var(--rule-width) solid var(--color-ink);
  border-radius: var(--radius-s);
  background: var(--color-paper-2);
}

.bar__segment {
  flex-basis: 0;
  min-width: 0;
}

.bar__segment + .bar__segment {
  border-left: var(--rule-width) solid var(--color-ink);
}

.bar__segment--positive {
  background: var(--color-accent);
}

.bar__segment--neutral {
  background: var(--color-marker);
}

.bar__segment--negative {
  background: var(--color-cool);
}

.idea__counts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1) var(--space-4);
  max-width: none;
  font-size: var(--text-s);
}

.idea__counts span {
  white-space: nowrap;
}

.adopt {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.adopt__warning {
  font-weight: 500;
}

.adopt__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}
</style>
