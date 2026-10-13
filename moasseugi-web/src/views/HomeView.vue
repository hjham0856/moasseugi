<script setup lang="ts">
import { RouterLink } from 'vue-router'

/** 서비스의 핵심 흐름. 순서가 의미를 가지므로 번호를 붙여 보여준다. */
const steps = [
  {
    title: '각자 작성',
    body: '다른 사람의 생각을 보기 전에, 각자 자기 아이디어를 하나 씁니다.',
  },
  {
    title: '익명 평가',
    body: '누가 썼는지 가린 채 좋음·괜찮음·별로로 반응합니다. 중간 집계도 보이지 않습니다.',
  },
  {
    title: '진행자가 채택',
    body: '점수나 순위 대신 반응이 어떻게 갈렸는지 보고, 진행자가 하나를 고릅니다.',
  },
]
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero__text">
        <h1 class="hero__title">흩어진 생각을<br />한 글자로 모읍니다</h1>
        <p class="hero__lead">
          한글의 자음과 모음이 모여 한 글자가 되듯, 따로 쓴 아이디어를 모아 하나의 선택으로
          만듭니다. 계정도 로그인도 없이 링크나 QR로 들어옵니다.
        </p>
        <RouterLink to="/create" class="button hero__cta">안건 만들기</RouterLink>
      </div>

      <!-- ㅁ과 ㅗ가 모여 음절 “모”가 되는 모양. 색은 토큰만 쓴다. -->
      <svg class="syllable" viewBox="0 0 120 120" role="img" aria-label="ㅁ과 ㅗ가 모여 모가 되는 그림">
        <rect class="syllable__marker" x="26" y="18" width="76" height="92" />
        <rect class="syllable__m" x="32" y="12" width="52" height="44" />
        <g class="syllable__o">
          <rect x="53" y="64" width="10" height="26" />
          <rect x="18" y="88" width="80" height="10" />
        </g>
      </svg>
    </section>

    <section class="flow">
      <h2 class="flow__title">이렇게 진행됩니다</h2>
      <ol class="flow__steps">
        <li v-for="(step, index) in steps" :key="step.title" class="flow__step">
          <span class="flow__number mono">{{ index + 1 }}</span>
          <h3 class="flow__step-title">{{ step.title }}</h3>
          <p class="flow__step-body">{{ step.body }}</p>
        </li>
      </ol>
    </section>

    <section class="join">
      <p class="join__text">
        참가자는 링크나 QR로 들어와 닉네임만 정하면 됩니다. 진행자도 같은 참가자로 시작합니다.
      </p>
      <RouterLink to="/create" class="button button--quiet">안건 만들기</RouterLink>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  gap: var(--space-8);
  padding: var(--space-10) var(--space-5) var(--space-12);
}

.hero__text {
  display: grid;
  gap: var(--space-5);
  justify-items: start;
}

.hero__title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--text-hero);
  line-height: 1.1;
}

.hero__lead {
  font-size: var(--text-l);
  color: var(--color-ink-2);
}

.hero__cta {
  margin-top: var(--space-2);
}

.syllable {
  width: min(11rem, 52vw);
  height: auto;
  color: var(--color-ink);
}

.syllable__marker {
  fill: var(--color-marker);
}

.syllable__m {
  fill: none;
  stroke: currentcolor;
  stroke-width: 10;
}

.syllable__o rect {
  fill: var(--color-accent);
}

/* 첫 로드에서 한 번, 흩어진 자모가 제자리로 모인다. */
.syllable__marker,
.syllable__m,
.syllable__o {
  animation: gather 650ms var(--ease-out) both;
}

.syllable__m {
  --from: -18px, -14px;
}

.syllable__o {
  --from: 6px, 22px;
  animation-delay: 120ms;
}

.syllable__marker {
  --from: 10px, 8px;
  animation-delay: 240ms;
}

@keyframes gather {
  from {
    opacity: 0;
    transform: translate(var(--from));
  }

  to {
    opacity: 1;
    transform: translate(0, 0);
  }
}

.flow {
  padding: var(--space-12) var(--space-5);
  background: var(--color-paper-2);
  border-block: var(--rule-width) solid var(--color-rule);
}

.flow__title {
  font-size: var(--text-2xl);
}

.flow__steps {
  display: grid;
  gap: var(--space-8);
  margin-top: var(--space-8);
}

.flow__step {
  display: grid;
  gap: var(--space-2);
  max-width: 34rem;
}

.flow__number {
  font-size: var(--text-s);
  font-weight: 500;
  color: var(--color-accent-ink);
}

.flow__step-title {
  font-size: var(--text-xl);
}

.flow__step-body {
  color: var(--color-ink-2);
}

.join {
  display: grid;
  gap: var(--space-5);
  justify-items: start;
  padding: var(--space-16) var(--space-5) var(--space-20);
}

.join__text {
  font-size: var(--text-l);
}

@media (min-width: 48rem) {
  .hero {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: var(--space-16);
    padding: var(--space-20) var(--space-10) var(--space-16);
  }

  .hero__text {
    max-width: 40rem;
  }

  .syllable {
    width: 13rem;
  }

  .flow {
    padding: var(--space-16) var(--space-10);
  }

  /* 단계마다 들여쓰기를 달리해 위에서 아래로 흐르는 비대칭 배치를 만든다. */
  .flow__step:nth-child(2) {
    margin-left: var(--space-16);
  }

  .flow__step:nth-child(3) {
    margin-left: calc(var(--space-16) * 2);
  }

  .join {
    padding-inline: var(--space-10);
  }
}
</style>
