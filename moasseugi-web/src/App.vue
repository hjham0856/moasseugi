<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="shell">
    <header class="shell__header">
      <RouterLink to="/" class="shell__wordmark">모아쓰기</RouterLink>
      <RouterLink to="/create" class="shell__create">안건 만들기</RouterLink>
    </header>

    <main class="shell__main">
      <RouterView />
    </main>

    <footer class="shell__footer">계정 없이, 링크 하나로 모아 씁니다.</footer>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}

.shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-bottom: var(--rule-width) solid var(--color-rule);
}

.shell__wordmark {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 400;
  color: var(--color-ink);
  text-decoration: none;
  white-space: nowrap;
}

.shell__create {
  color: var(--color-ink);
  font-weight: 500;
  white-space: nowrap;
}

.shell__main {
  flex: 1;
}

.shell__footer {
  padding: var(--space-8) var(--space-5);
  border-top: var(--rule-width) solid var(--color-rule);
  color: var(--color-ink-2);
  font-size: var(--text-s);
}

@media (min-width: 48rem) {
  .shell__header,
  .shell__footer {
    padding-inline: var(--space-10);
  }
}
</style>
