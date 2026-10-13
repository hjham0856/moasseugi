import { createRouter, createWebHistory } from 'vue-router'

/**
 * 첫 흐름에 필요한 세 화면만 둔다.
 * 단계·권한 검사는 전역 가드가 아니라 서버 응답과 각 화면의 안내로 처리한다.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/create', name: 'create', component: () => import('../views/CreateView.vue') },
    {
      // 공유 링크와 QR이 가리키는 안건 진입점
      path: '/s/:sessionId',
      name: 'session',
      component: () => import('../views/SessionView.vue'),
    },
    {
      // 진행자 전용 관리 화면. 참가자 화면과 분리해 일반 참가자가 진행 조작을 마주치지 않게 한다.
      path: '/s/:sessionId/manage',
      name: 'manage',
      component: () => import('../views/ManageView.vue'),
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
