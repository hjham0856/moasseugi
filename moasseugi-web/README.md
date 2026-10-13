# moasseugi-web

모아쓰기 웹 화면(Vue 3 + TypeScript + Vite). 전체 실행 방법과 현재 제한은 [루트 README](../README.md)를 따른다.

## 명령

```sh
npm ci            # 의존성 설치 (Node ^22.18.0 || >=24.12.0)
npm run dev       # http://localhost:5173
npm run type-check
npm run lint
npm run build
```

## API 주소

`VITE_API_BASE_URL`로 지정하며 `/api`까지 포함한다. 기본값은 `http://localhost:8080/api`다. 바꿀 때는 `.env.local`에 둔다.

## 구조

- `src/views` — 라우트별 화면 (홈, 안건 생성, 안건 진행, 진행자 관리)
- `src/components` — 화면에서 쓰는 패널 (작성, 평가, 결과, 공유)
- `src/api` — `fetch` 래퍼와 API 호출, 참가 키의 브라우저 저장
- `src/share` — 공유 주소와 QR 생성
- `src/styles/tokens.css` — 색·글꼴 등 디자인 토큰
