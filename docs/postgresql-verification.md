# #19 PostgreSQL 전환 검증

2026-10-13, `feat/19-postgresql-전환`에서 실제 실행했다. 새 Compose 볼륨으로 시작했고 PostgreSQL은 17.11, Hibernate는 7.4.5.Final이었다. 테스트 설정·테스트 코드·API 계약은 바꾸지 않았다.

## 연결·기존 테스트

저장소 루트에서 실행했다.

```sh
docker compose up -d
docker compose ps
```

`postgres:17` 이미지를 정상으로 받았고 `codex-19-postgres-1`은 `healthy`, 포트는 `127.0.0.1:5433->5432/tcp`였다.

```sh
cd moasseugi-api
sh gradlew build
SPRING_PROFILES_ACTIVE=local,postgres sh gradlew test --rerun-tasks
```

- H2: `BUILD SUCCESSFUL in 11s`, 39개, 실패·오류·건너뜀 모두 0.
- PostgreSQL: `BUILD SUCCESSFUL in 11s`, 39개, 실패·오류·건너뜀 모두 0. XML 테스트 결과의 개수를 합산했다.
- `CoreFlowIntegrationTest` 로그: 활성 프로파일 `local`, `postgres`, JDBC URL `jdbc:postgresql://localhost:5433/moasseugi`, dialect `PostgreSQLDialect`, DB 버전 `17.11`.
- 기존 테스트에서 진행자·소유자·단계 제한, 다른 안건의 키·아이디어 거부, 평가 없는 결과·채택을 함께 확인했다. 아이디어 중복 직접 저장 테스트는 Hibernate가 제공하는 `uk_idea_participant` 이름도 확인한다.
- 컴파일 시 요청한 Hibernate `@Check` 사용에 대한 deprecated API 경고가 있었다. 빌드·DDL 생성에는 문제가 없었다.

## 스키마·DB 거부 동작

저장소 루트에서 실행했다.

```sh
docker compose exec -T postgres psql -U moasseugi -d moasseugi -c '\d participants' -c '\d ideas' -c '\d evaluations' -c '\d sessions' -c 'SELECT version();'
```

실제 `\d` 출력의 제약 부분은 다음과 같다.

```text
participants:
  "uk_participant_session_nickname" UNIQUE CONSTRAINT, btree (session_id, nickname)
  "uk_participant_token" UNIQUE CONSTRAINT, btree (participant_token)
  "fk_participant_session" FOREIGN KEY (session_id) REFERENCES sessions(id)
ideas:
  "uk_idea_participant" UNIQUE CONSTRAINT, btree (participant_id)
  "fk_idea_participant" FOREIGN KEY (participant_id) REFERENCES participants(id)
evaluations:
  "uk_evaluation_participant_idea" UNIQUE CONSTRAINT, btree (participant_id, idea_id)
  "ck_evaluation_value" CHECK (value = ANY (ARRAY['-1'::integer, 0, 1]))
  "fk_evaluation_idea" FOREIGN KEY (idea_id) REFERENCES ideas(id)
  "fk_evaluation_participant" FOREIGN KEY (participant_id) REFERENCES participants(id)
sessions:
  "fk_session_selected_idea" FOREIGN KEY (selected_idea_id) REFERENCES ideas(id)
```

네 테이블의 ID와 참조 ID는 `uuid`, 평가값은 `smallint`다. 서비스가 소문자로 비교하는 세 UNIQUE 이름이 PostgreSQL에서도 그대로 존재한다. SQL 위반 메시지에도 동일한 이름이 나왔으므로 기존 이름 판별 코드는 변경하지 않았다.

콜레이션은 다음 명령으로 확인했다. 최초 `SHOW lc_collate`는 이 DB에서 `unrecognized configuration parameter`로 실패해 카탈로그 조회로 확인했다.

```sh
docker compose exec -T postgres psql -U moasseugi -d moasseugi -c "SELECT datname, datcollate, datctype FROM pg_database WHERE datname = current_database();"
```

`moasseugi | en_US.utf8 | en_US.utf8`. 실제 API로 ` Kim `은 `Kim`, ` kim `은 `kim`으로 저장됐고 같은 안건에서 둘 다 201이었다. ` Kim ` 재참가는 409였다. ` A  B `는 `A  B`로 저장되고 `A B`도 별도 참가가 가능했다. 따라서 앞뒤 공백 제거·대소문자 구분·내부 공백 유지 정책을 확인했다. 콜레이션을 변경할 때에는 닉네임 비교 정책을 다시 확인한다.

DB에 직접 잘못된 행을 넣는 SQL은 각각 `BEGIN`/`ROLLBACK`으로 감싸 실행했다. 아래 SQL의 UUID는 실제 curl 실행에서 발급된 값이다.

```sh
docker compose exec -T postgres psql -U moasseugi -d moasseugi < /tmp/moasseugi-19-constraints.sql
```

실행한 SQL:

```sql
-- 평가값 CHECK
BEGIN;
INSERT INTO evaluations(id,participant_id,idea_id,"value") VALUES(gen_random_uuid(),'48ce12c2-8530-4f09-b652-999d657687a0','381bda37-9d3e-4e3e-997e-33bc70dfd5e3',2);
ROLLBACK;
-- 닉네임 UNIQUE
BEGIN;
INSERT INTO participants(id,session_id,nickname,participant_token,is_host) VALUES(gen_random_uuid(),'74039f3c-16c7-4ad7-915b-2464807f7cdd','Kim','sql-check-token',false);
ROLLBACK;
-- 아이디어 UNIQUE
BEGIN;
INSERT INTO ideas(id,participant_id,content,created_at) VALUES(gen_random_uuid(),'48ce12c2-8530-4f09-b652-999d657687a0','중복',now());
ROLLBACK;
-- 평가 UNIQUE
BEGIN;
INSERT INTO evaluations(id,participant_id,idea_id,"value") VALUES(gen_random_uuid(),'85d2645a-5962-4895-b0f1-f9e4345b74c4','4370dfcd-765d-4d7a-9449-4e59520c9db7',0);
ROLLBACK;
-- 참가자 FK
BEGIN;
INSERT INTO participants(id,session_id,nickname,participant_token,is_host) VALUES(gen_random_uuid(),gen_random_uuid(),'외래 키 검증','sql-fk-token',false);
ROLLBACK;
-- 아이디어 FK
BEGIN;
INSERT INTO ideas(id,participant_id,content,created_at) VALUES(gen_random_uuid(),gen_random_uuid(),'외래 키 검증',now());
ROLLBACK;
-- 평가 참가자 FK
BEGIN;
INSERT INTO evaluations(id,participant_id,idea_id,"value") VALUES(gen_random_uuid(),gen_random_uuid(),'4370dfcd-765d-4d7a-9449-4e59520c9db7',0);
ROLLBACK;
-- 평가 아이디어 FK
BEGIN;
INSERT INTO evaluations(id,participant_id,idea_id,"value") VALUES(gen_random_uuid(),'48ce12c2-8530-4f09-b652-999d657687a0',gen_random_uuid(),0);
ROLLBACK;
-- 채택 FK
BEGIN;
UPDATE sessions SET selected_idea_id=gen_random_uuid() WHERE id='74039f3c-16c7-4ad7-915b-2464807f7cdd' ;
ROLLBACK;
```

각 시도가 거부된 출력(모두 롤백):

```text
ERROR:  23514: new row for relation "evaluations" violates check constraint "ck_evaluation_value"
ERROR:  23505: duplicate key value violates unique constraint "uk_participant_session_nickname"
ERROR:  23505: duplicate key value violates unique constraint "uk_idea_participant"
ERROR:  23505: duplicate key value violates unique constraint "uk_evaluation_participant_idea"
ERROR:  23503: insert or update on table "participants" violates foreign key constraint "fk_participant_session"
ERROR:  23503: insert or update on table "ideas" violates foreign key constraint "fk_idea_participant"
ERROR:  23503: insert or update on table "evaluations" violates foreign key constraint "fk_evaluation_participant"
ERROR:  23503: insert or update on table "evaluations" violates foreign key constraint "fk_evaluation_idea"
ERROR:  23503: insert or update on table "sessions" violates foreign key constraint "fk_session_selected_idea"
```

## 실제 curl 흐름·재시작

API를 다음 명령으로 실행했다.

```sh
cd moasseugi-api
sh gradlew bootRun --args='--spring.profiles.active=local,postgres'
```

아래는 실제 실행한 curl 명령과 상태 코드다. 참가 키만 `$HOST_TOKEN`·`$GUEST_TOKEN`으로 치환했다. 각 변수는 생성·참가 응답의 `participantToken`이다. UUID는 당시 발급값이므로 새 실행에서는 새 응답값으로 바꾼다.

```sh
# 생성·trim: 201
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions -H 'Content-Type: application/json' -d '{"title": "PostgreSQL 검증 #19", "description": "재시작 후 유지 확인", "nickname": " Kim "}'
# 대소문자 구분 참가: 201
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/participants -H 'Content-Type: application/json' -d '{"nickname": " kim "}'
# 중복 닉네임: 409
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/participants -H 'Content-Type: application/json' -d '{"nickname": " Kim "}'
# 내부 공백 유지: 201
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/participants -H 'Content-Type: application/json' -d '{"nickname": " A  B "}'
# 내부 공백 다른 닉네임: 201
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/participants -H 'Content-Type: application/json' -d '{"nickname": "A B"}'
# 아이디어 없이 평가 시작: 409
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/close-writing -H "X-Participant-Token: $HOST_TOKEN"
# 결과 단계 전 조회: 409
curl -sS -w '\n%{http_code}' -X GET http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/results
# 아이디어 저장: 200
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/ideas -H 'Content-Type: application/json' -d '{"content": "초기 생각"}' -H "X-Participant-Token: $HOST_TOKEN"
# 아이디어 수정: 200
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/ideas -H 'Content-Type: application/json' -d '{"content": "수정한 생각"}' -H "X-Participant-Token: $HOST_TOKEN"
# 무평가 아이디어 저장: 200
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/ideas -H 'Content-Type: application/json' -d '{"content": "평가 없는 생각"}' -H "X-Participant-Token: $GUEST_TOKEN"
# 진행자 외 전환: 403
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/close-writing -H "X-Participant-Token: $GUEST_TOKEN"
# 평가 시작: 200
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/close-writing -H "X-Participant-Token: $HOST_TOKEN"
# 작성 종료 후 참가: 409
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/participants -H 'Content-Type: application/json' -d '{"nickname": "늦은 참가자"}'
# 작성 종료 후 수정: 409
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/ideas -H 'Content-Type: application/json' -d '{"content": "거부할 수정"}' -H "X-Participant-Token: $HOST_TOKEN"
# 본인 아이디어 평가: 403
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/evaluations -H 'Content-Type: application/json' -d '{"ideaId": "4370dfcd-765d-4d7a-9449-4e59520c9db7", "value": 1}' -H "X-Participant-Token: $HOST_TOKEN"
# 평가 저장: 200
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/evaluations -H 'Content-Type: application/json' -d '{"ideaId": "4370dfcd-765d-4d7a-9449-4e59520c9db7", "value": -1}' -H "X-Participant-Token: $GUEST_TOKEN"
# 평가 변경: 200
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/evaluations -H 'Content-Type: application/json' -d '{"ideaId": "4370dfcd-765d-4d7a-9449-4e59520c9db7", "value": 1}' -H "X-Participant-Token: $GUEST_TOKEN"
# 평가 종료: 200
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/close-evaluation -H "X-Participant-Token: $HOST_TOKEN"
# 평가 종료 후 변경: 409
curl -sS -w '\n%{http_code}' -X PUT http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/evaluations -H 'Content-Type: application/json' -d '{"ideaId": "4370dfcd-765d-4d7a-9449-4e59520c9db7", "value": 0}' -H "X-Participant-Token: $GUEST_TOKEN"
# 결과 분포: 200
curl -sS -w '\n%{http_code}' -X GET http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/results
# 진행자 외 채택: 403
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/select-idea -H 'Content-Type: application/json' -d '{"ideaId": "381bda37-9d3e-4e3e-997e-33bc70dfd5e3"}' -H "X-Participant-Token: $GUEST_TOKEN"
# 무평가 아이디어 채택: 200
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/select-idea -H 'Content-Type: application/json' -d '{"ideaId": "381bda37-9d3e-4e3e-997e-33bc70dfd5e3"}' -H "X-Participant-Token: $HOST_TOKEN"
# 재채택: 409
curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/select-idea -H 'Content-Type: application/json' -d '{"ideaId": "4370dfcd-765d-4d7a-9449-4e59520c9db7"}' -H "X-Participant-Token: $HOST_TOKEN"
# 안건 조회: 200
curl -sS -w '\n%{http_code}' -X GET http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd
```

수정 전후 아이디어 ID는 같았다. 평가는 -1에서 1로 변경됐고 결과의 좋음 1·괜찮음 0·별로 0·합계 1이 저장과 일치했다. 두 아이디어 모두 채택 전 작성자가 숨겨졌고, 무평가 아이디어는 합계 0으로 포함되어 채택됐으며 그 작성자 `kim`만 공개됐다. 이 안건의 DB 기록은 참가자 4·아이디어 2·평가 1건이었다.

CORS도 확인했다.

```sh
curl -sS -i -X OPTIONS http://localhost:8080/api/sessions -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: Content-Type,X-Participant-Token'
```

HTTP 200, `Access-Control-Allow-Origin: http://localhost:5173`, 허용 메서드 `GET,POST,PUT`, 허용 헤더 `Content-Type, X-Participant-Token`.

첫 API PID 27147을 `kill -TERM 27147`로 종료한 뒤 같은 bootRun 명령으로 재실행했다(PID 27329). 아래 세 조회가 모두 200이고 재시작 전 값과 일치했다.

```sh
curl -sS -w '\n%{http_code}' http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd -H "X-Participant-Token: $HOST_TOKEN"
curl -sS -w '\n%{http_code}' http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/results -H "X-Participant-Token: $HOST_TOKEN"
curl -sS -w '\n%{http_code}' http://localhost:8080/api/sessions/74039f3c-16c7-4ad7-915b-2464807f7cdd/ideas/me -H "X-Participant-Token: $HOST_TOKEN"
```

안건의 RESULT·채택 UUID, 수정 본문 `수정한 생각`, 평가 분포와 무평가 채택작의 작성자 공개가 유지됐다. 재시작 검증 후 `kill -TERM 27329`, 저장소 루트에서 `docker compose stop`으로 API와 DB를 중지했다. Compose는 `Exited (0)`이며 볼륨은 유지했다. SIGTERM으로 종료한 bootRun의 Gradle 종료값 143은 의도한 종료 결과다.

## 선택 이유와 검토 순서

1. `README.md`: 실행부터 읽는다. 프로파일은 실행 시 선택하는 설정 묶음이다. `local,postgres`는 기존 로컬 CORS를 유지하고 뒤의 PostgreSQL 설정으로 DB만 바꾼다.
2. `compose.yaml`: docker compose는 YAML에 적은 컨테이너를 함께 실행·중지하는 도구다. 여기서는 DB 하나만 실행하고 호스트 5433을 컨테이너 5432에 연결한다. 이름 있는 볼륨은 컨테이너와 별도로 데이터를 보관한다.
3. `application-postgres.yaml` → `application.yaml` → `build.gradle`: 환경 변수는 실행하는 프로그램에 밖에서 전달하는 값이며 연결 URL·계정의 기본값을 덮어쓴다. PostgreSQL 드라이버는 Java와 PostgreSQL의 통신을 맡는다. `ddl-auto: update`는 Hibernate가 엔티티에 맞춰 테이블을 생성·갱신하는 설정이며 삭제·이름 변경이나 변경 이력을 관리하는 도구는 아니다.
4. `EvaluationEntity.java` → `docs/moasseugi-db.dbml`: CHECK 제약은 DB가 각 행의 평가값을 -1·0·1로 제한하는 규칙이다. UNIQUE는 같은 키 조합의 중복, FK는 존재하지 않는 참조를 막는다. 따옴표로 감싼 `"value"`는 H2와 PostgreSQL에서 같은 컬럼을 가리킨다.
5. 이 검증 문서 → `docs/moasseugi-overview.md` → `AGENTS.md`: 실제 결과와 이후 개발 기준을 확인한다. 콜레이션은 DB의 문자열 정렬·비교 규칙이다. 이번 DB에서는 `Kim`과 `kim`을 다른 닉네임으로 확인했으며 대소문자를 없애는 별도 정규화는 하지 않는다.

## 검증 범위의 한계

요청한 로컬 연결·기존 테스트·스키마·제약·curl 핵심 흐름·API 재시작 유지 검증은 완료했다. 브라우저 화면의 재검증·배포·JWT는 이번 변경 범위 밖이다. 동시 HTTP 요청의 경쟁 상황은 별도로 재현하지 않았으며 중복 거부는 기존 테스트와 직접 SQL로 확인했다. 기존 테이블에 새 CHECK를 추가하는 변경과 `down -v` 초기화는 실행하지 않았다(새 볼륨의 최초 스키마와 데이터 유지가 검증 대상). H2 메모리 데이터는 이관 대상이 없다.
