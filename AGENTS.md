# AGENTS.md

## 구조
- `moasseugi-api/` — Spring Boot 4.1.1, Java 21, Gradle. `server.servlet.context-path: /api`
- `moasseugi-web/` — Vue 3.5 + TS + vue-router + pinia. HTTP는 `fetch` 래퍼, axios 미도입
- `docs/moasseugi-api.yml` — API 단일 진실 원천 (OpenAPI 3.0.3 유지)
- `docs/moasseugi-db.dbml`, `docs/moasseugi-frontend.md` — DB·프론트 설계
- `demo/` — 정적 화면 참고용. 구조만 참고, CSS 이식 금지

## 백엔드 명령 (moasseugi-api/에서)
- `gradlew`에 실행권한 없음. **`sh gradlew`로 실행**
- `sh gradlew build` — 평소 빌드 (generator와 무관)
- `sh gradlew openApiValidate` — yml 수정 후 필수
- `sh gradlew openApiGenerate` — yml 바뀔 때만 수동 실행. 출력은 `build/generated` (git 무시, 커밋 금지)

## yml 수정 규칙
- `description:` 한 줄 값에 콜론+공백(`(code: XXX)` 등)이 들어가면 **반드시 쌍따옴표**로 감쌀 것. 안 그러면 YAML 파서가 `code:`를 중첩 맵으로 오해해 validate 실패
- `enum`에 `null` 넣지 말 것. null 허용은 `nullable: true`로만 표현 (이미 그렇게 되어 있음)
- `operationId`·`tag` 함부로 변경 금지 (generator `interfaceOnly`, `useTags` 기준)
- 생성된 코드(`build/generated`) 직접 수정 금지. yml → validate → generate → 구현체 수정 순서

## 프로파일
- `application.yaml` 공통 + `application-local.yaml`(H2, create-drop). 기본 active는 `local`
- `application-prod.yaml` 없음. 배포 확정 시 추가 예정. JWT 시크릿·만료 설정은 #3(Auth)에서 도입

## 작업 방식
- #3까지 main 직행. 커밋 메시지에 `Closes #n` 포함하면 이슈 자동 종료·링크됨
- 사용자와 대화하면서 진행: 제안 → 확인 → 실행. 묻지 않고 연달아 진행하지 말 것
- 순서: #2 공통 기반 → #3 Auth·User → (#4 전에 #10 단계전환 함수 시그니처 먼저 합의) → #4~#10 → #11 테스트(H2 단일 프로파일, vitest·E2E 없음)
