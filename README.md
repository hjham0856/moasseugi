# 모아쓰기

회의에서 참가자가 남의 아이디어를 보기 전에 각자 생각을 쓰고, 작성자를 모른 채 서로 평가한 뒤, 진행자가 평가 분포를 참고해 하나를 채택하도록 돕는 도구다. 계정 없이 링크나 QR로 참가한다. 제품의 목적과 원칙은 [docs/PRODUCT.md](docs/PRODUCT.md)에 있다.

## 준비물

- Java 21
- Node.js `^22.18.0 || >=24.12.0` (`moasseugi-web/package.json`의 `engines`)

## 로컬 실행

터미널 두 개에서 각각 실행한다.

```sh
# 1. API (http://localhost:8080/api, local 프로파일 + H2 메모리 DB)
cd moasseugi-api
sh gradlew bootRun

# 2. 웹 (http://localhost:5173)
cd moasseugi-web
npm ci
npm run dev
```

`gradlew`에 실행 권한이 없어 `./gradlew` 대신 `sh gradlew`로 실행한다.

### API 주소 설정

웹은 기본으로 `http://localhost:8080/api`를 호출한다. 바꾸려면 `moasseugi-web/.env.local`에 다음처럼 적는다. API의 context-path인 `/api`까지 포함해야 한다.

```
VITE_API_BASE_URL=http://localhost:8080/api
```

API는 `local` 프로파일에서만 `http://localhost:5173`의 브라우저 요청을 허용한다(CORS). 따라서 웹은 `localhost:5173`으로 열어야 하며, 다른 주소·포트로 열면 API 호출이 브라우저에서 막힌다.

### 개발용 H2 콘솔

API 실행 중 `http://localhost:8080/api/h2-console`에서 데이터를 볼 수 있다. JDBC URL은 `jdbc:h2:mem:moasseugi`, 사용자 `sa`, 비밀번호는 비운다.

## 빌드와 검사

```sh
cd moasseugi-api && sh gradlew build        # 백엔드 빌드 + 테스트

cd moasseugi-web
npm run type-check
npm run lint
npm run build
```

## 현재 제한

- 로그인이 없다. 참가자는 안건마다 발급된 참가 키를 브라우저에 저장해 본인을 구분한다.
- 키 복구가 없다. 브라우저에 저장된 키를 잃으면 그 참가자로 다시 행동할 수 없다. 작성 중에는 다른 닉네임으로 새로 참가할 수 있지만 이전 기록은 옮겨지지 않는다.
- 진행자 키를 잃으면 그 안건을 관리할 수 없다.
- H2는 `create-drop`이라 API를 재시작하면 모든 데이터가 사라진다. PostgreSQL 전환은 개발 마지막 단계에서 한다.
- 배포 설정은 없다. 로컬 개발 환경만 다룬다.

## 문서

- [docs/PRODUCT.md](docs/PRODUCT.md) — 제품 목적·문제·원칙
- [docs/moasseugi-overview.md](docs/moasseugi-overview.md) — 제품 범위와 구현 순서
- [docs/moasseugi-api.yml](docs/moasseugi-api.yml) — API 계약(OpenAPI)
- [docs/moasseugi-frontend.md](docs/moasseugi-frontend.md) — 프론트 설계
- [docs/moasseugi-db.dbml](docs/moasseugi-db.dbml) — DB 설계
