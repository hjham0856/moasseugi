<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { ApiError } from '../api/http'
import { getMyIdea, putIdea } from '../api/ideas'

/**
 * 작성 단계에서 내 아이디어 하나를 쓰고 저장하는 영역.
 * 다른 참가자의 아이디어는 불러오지도 보여주지도 않아 각자 먼저 생각할 수 있게 한다.
 * 자동 저장은 두지 않는다. 사용자가 저장을 눌러야만 서버에 남으며 화면의 상태 표시가 그 사실을 그대로 드러낸다.
 */
const props = defineProps<{ sessionId: string }>()

const text = ref('')
/** 서버에 확정된 내용. 입력과 다르면 아직 저장되지 않은 변경이 있는 것이다. */
const savedContent = ref<string | null>(null)

const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const saveError = ref('')

const hasSaved = computed(() => savedContent.value !== null)
const dirty = computed(() => text.value !== (savedContent.value ?? ''))
const blank = computed(() => text.value.trim() === '')

/** 저장 상태를 조용한 한 줄로 알린다. 저장 중에는 버튼 문구가 대신 알리므로 비운다. */
const statusText = computed(() => {
  if (saving.value) {
    return ''
  }

  if (dirty.value) {
    return hasSaved.value ? '저장되지 않은 수정이 있습니다' : '아직 저장하지 않았습니다'
  }

  return hasSaved.value ? '저장됨' : ''
})

function describeError(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : '서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.'
}

/** 재접속해도 저장한 내용이 돌아오도록 마운트 때 내 아이디어를 조회한다. */
async function load() {
  loading.value = true
  loadError.value = ''

  try {
    const idea = await getMyIdea(props.sessionId)

    savedContent.value = idea ? idea.content : null
    text.value = idea ? idea.content : ''
  } catch (error) {
    loadError.value = describeError(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

/** 저장 실패 시 입력은 그대로 두어 사용자가 같은 내용을 다시 저장할 수 있게 한다. */
async function save() {
  if (saving.value || blank.value) {
    return
  }

  const submitted = text.value

  saving.value = true
  saveError.value = ''

  try {
    const idea = await putIdea(props.sessionId, submitted)

    savedContent.value = idea.content

    // 저장 중에 더 입력했다면 그 입력은 덮어쓰지 않고 미저장 변경으로 남긴다.
    if (text.value === submitted) {
      text.value = idea.content
    }
  } catch (error) {
    saveError.value = describeError(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="writing" aria-labelledby="writing-title">
    <h2 id="writing-title" class="writing__title">내 아이디어</h2>
    <p class="writing__hint">다른 참가자의 아이디어는 보이지 않습니다. 하나만 쓸 수 있고, 평가가 시작되기 전까지 고칠 수 있습니다.</p>

    <p v-if="loading" class="writing__state" role="status">내 아이디어를 불러오는 중…</p>

    <div v-else-if="loadError" class="writing__load-error">
      <p class="field-error" role="alert">{{ loadError }}</p>
      <button type="button" class="button button--quiet" @click="load">다시 불러오기</button>
    </div>

    <form v-else class="writing__form" @submit.prevent="save">
      <div class="writing__field">
        <label for="idea-content">아이디어 (필수)</label>
        <textarea
          id="idea-content"
          v-model="text"
          class="writing__slip"
          required
          rows="6"
          :aria-invalid="saveError ? 'true' : undefined"
          :aria-describedby="saveError ? 'idea-error' : undefined"
        ></textarea>
      </div>

      <p v-if="saveError" id="idea-error" class="field-error" role="alert">{{ saveError }}</p>

      <div class="writing__actions">
        <button type="submit" class="button" :disabled="saving || blank">
          {{ saving ? '저장하는 중…' : hasSaved ? '수정 저장' : '저장' }}
        </button>

        <p class="writing__state" role="status">{{ statusText }}</p>
      </div>
    </form>
  </section>
</template>

<style scoped>
.writing {
  display: grid;
  gap: var(--space-4);
}

.writing__title {
  font-size: var(--text-xl);
}

.writing__hint,
.writing__state {
  color: var(--color-ink-2);
  font-size: var(--text-s);
}

.writing__form,
.writing__field,
.writing__load-error {
  display: grid;
  gap: var(--space-3);
}

.writing__field {
  gap: var(--space-2);
}

/*
 * 괘선 노트 한 장. 줄 간격이 글줄 높이와 같아 글이 줄 위에 앉는다.
 * 장식이 아니라 "쓰는 자리"를 알리는 실선 무늬이며, local 고정으로 긴 글을 스크롤해도 줄이 글과 함께 움직인다.
 */
.writing__slip {
  --line: calc(var(--leading-body) * 1em);

  min-height: calc(var(--line) * 6 + var(--space-6));
  padding-block: var(--space-3);
  line-height: var(--leading-body);
  background-color: var(--color-slip);
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent calc(var(--line) - var(--rule-width)),
    var(--color-rule) calc(var(--line) - var(--rule-width)),
    var(--color-rule) var(--line)
  );
  background-attachment: local;
  background-origin: content-box;
  background-clip: padding-box;
  box-shadow: var(--offset-shadow);
}

.writing__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3) var(--space-4);
}
</style>
