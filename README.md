# 모아쓰기

회의에서 참가자가 남의 아이디어를 보기 전에 각자 생각을 쓰고, 작성자를 모른 채 서로 평가한 뒤, 진행자가 평가 분포를 참고해 하나를 채택하도록 돕는 도구다. 계정 없이 링크나 QR로 참가한다. 제품의 목적과 원칙은 [docs/PRODUCT.md](docs/PRODUCT.md)에 있다.

## 준비물

- Java 21
- Node.js `^22.18.0 || >=24.12.0` (`moasseugi-web/package.json`의 `engines`)
- PostgreSQL로 실행할 때 Docker와 Docker Compose

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

### 로컬 PostgreSQL 실행

저장한 안건을 API 재시작 후에도 유지하려면 다음처럼 실행한다.

```sh
# 저장소 루트에서 DB 시작·상태 확인 (healthy가 된 뒤 API 실행)
docker compose up -d
docker compose ps

cd moasseugi-api
sh gradlew bootRun --args='--spring.profiles.active=local,postgres'
```

`local,postgres` 순서가 중요하다. `local`의 프론트 CORS를 유지하면서 뒤의 `postgres` 프로파일이 datasource와 `ddl-auto`를 덮어쓴다. 기본 프로파일은 여전히 `local`이며 H2 콘솔은 PostgreSQL 실행 시 꺼진다. 웹 실행 방법과 API 주소는 같다.

Compose는 PostgreSQL 17을 `localhost:5433`에 열고 DB·사용자·비밀번호를 모두 `moasseugi`로 만든다. 이 값은 로컬 개발 전용이다. API 연결은 환경 변수로 바꿀 수 있으며 별도의 비밀 파일이나 배포 프로파일은 만들지 않는다. 실제 비밀 값은 저장소에 커밋하지 않는다.

| 환경 변수 | 기본값 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5433/moasseugi` |
| `DB_USERNAME` | `moasseugi` |
| `DB_PASSWORD` | `moasseugi` |

환경 변수는 API 연결을 바꾸며 Compose의 기존 DB·계정을 바꾸지는 않는다. 스키마는 Hibernate의 `ddl-auto: update`로 생성·갱신한다. 마이그레이션 프레임워크는 사용하지 않는다. `update`는 컬럼 삭제·이름 변경을 자동 처리하지 않으므로 모델 변경 시 실제 스키마를 확인하고 필요하면 수동 SQL이나 로컬 DB 초기화를 사용한다. 이미 있는 테이블의 CHECK 추가도 자동 반영을 전제하지 않는다. 이번 CHECK는 새 스키마 생성에서 확인한다.

데이터는 Compose의 이름 있는 볼륨에 남는다. H2는 메모리 DB라 재시작 시 사라지므로 이전 개발 데이터의 이관은 하지 않는다. 아래 명령은 저장소 루트에서 실행한다.

```sh
docker compose stop       # DB 중지, 데이터 유지
docker compose up -d      # 다시 시작
docker compose down -v    # 컨테이너·볼륨 제거, 모든 로컬 DB 데이터 초기화
```

PostgreSQL에서 기존 테스트를 다시 실행할 수 있다. 테스트가 테이블의 기록을 정리하므로 보관할 데이터가 있는 DB에서는 실행하지 않는다. `--rerun-tasks`는 H2 테스트의 이전 성공 결과를 재사용하지 않도록 한다.

```sh
cd moasseugi-api
SPRING_PROFILES_ACTIVE=local,postgres sh gradlew test --rerun-tasks
```

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
- H2는 `create-drop`이라 API를 재시작하면 모든 데이터가 사라진다. PostgreSQL은 볼륨과 `update`로 유지하며 스키마 변경 이력을 자동 관리하지 않는다.
- 배포 설정은 없다. 로컬 개발 환경만 다룬다.

## 문서

- [docs/PRODUCT.md](docs/PRODUCT.md) — 제품 목적·문제·원칙
- [docs/moasseugi-overview.md](docs/moasseugi-overview.md) — 제품 범위와 구현 순서
- [docs/moasseugi-api.yml](docs/moasseugi-api.yml) — API 계약(OpenAPI)
- [docs/moasseugi-frontend.md](docs/moasseugi-frontend.md) — 프론트 설계
- [docs/moasseugi-db.dbml](docs/moasseugi-db.dbml) — DB 설계
- [docs/postgresql-verification.md](docs/postgresql-verification.md) — #19의 실제 명령·검증 결과와 검토 순서
