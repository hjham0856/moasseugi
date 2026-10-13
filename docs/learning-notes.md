# 학습 노트

#1~#13 구현에는 나오지 않았고, 이후 이슈에서 처음 등장한 개념 중 다시 볼 가치가 큰 것만 모은다.
각 항목은 "어디에 나오는가 → 무엇인가 → 왜 중요한가" 순서로 적는다.

## #9 결과 분포·1회 채택

### 랜덤 UUID에는 순서가 없다 → 생성순은 따로 기록해야 한다
- 위치: `IdeaEntity.createdAt`, `IdeaRepository.findByParticipant_Session_IdOrderByCreatedAtAscIdAsc`
- 아이디어의 ID는 랜덤 UUID라서 ID로 정렬해도 만든 순서가 나오지 않는다. 결과를 "생성순"으로 보여주려면 생성 시각처럼 순서를 담은 값을 함께 저장해야 한다.
- 같은 시각에 두 행이 생기는 드문 경우를 위해 `id`를 두 번째 정렬 기준으로 붙여 결과 순서가 요청마다 흔들리지 않게 했다.

### 서로를 가리키는 외래 키(순환 FK)
- 위치: `SessionEntity.selectedIdea`(sessions → ideas), 기존 `ideas → participants → sessions`
- 안건은 채택 아이디어를, 아이디어는 (참가자를 거쳐) 안건을 가리킨다. 테이블 A를 만들 때 B가 아직 없으면 FK를 걸 수 없기 때문에, Hibernate는 테이블을 먼저 모두 만든 뒤 `ALTER TABLE … ADD CONSTRAINT`로 FK를 나중에 붙인다.
- 지울 때도 순서 문제가 생긴다. 채택된 안건이 남아 있으면 그 아이디어를 먼저 지울 수 없으므로, `ResultControllerTest`의 정리 코드는 `selected_idea_id`를 먼저 비운 뒤 행을 지운다.

### 지연 로딩(LAZY) 연관에서 ID만 꺼내기
- 위치: `SessionEntity.getSelectedIdeaId()`
- `@ManyToOne(fetch = LAZY)`는 연관 객체를 실제로 쓸 때까지 DB에서 읽지 않고 대리 객체(proxy)를 둔다. Hibernate의 proxy는 ID를 이미 알고 있어서 `getId()`만 부르면 추가 조회가 일어나지 않는다. 그래서 공개 안건 조회는 기존처럼 ID만 싸게 반환한다.
- 반대로 작성자 닉네임처럼 연관을 더 따라가야 하는 값은 트랜잭션 안(서비스 메서드)에서 읽어야 한다. 트랜잭션이 끝난 뒤 접근하면 `LazyInitializationException`이 난다.

### 집계를 DB에서 할까, Java에서 할까
- 위치: `ResultService.countByIdea`
- SQL `GROUP BY`로 개수만 받아오는 방법도 있지만, 회의 한 번의 평가는 많아야 수백 건이라 안건의 평가를 한 번에 읽어 Java에서 세는 쪽을 택했다. 쿼리 한 번, 코드가 단순하고 테스트로 확인하기 쉽다. 데이터가 커지면 DB 집계로 옮기는 것이 맞다.
- "평가 없음"을 0으로 채우는 처리는 어느 쪽이든 필요하다. 평가 행이 없는 아이디어는 집계 결과에 아예 나타나지 않기 때문이다.

### 확인 후 실행(check-then-act)의 경쟁 조건
- 위치: `ResultService.selectIdea`
- "이미 채택했는지 확인 → 채택"은 두 요청이 거의 동시에 들어오면 둘 다 확인을 통과할 수 있다. 진행자가 한 명이고 화면에서 요청 중 버튼을 잠그므로 현재 범위에서는 받아들인 제약이다.
- 엄밀히 막으려면 조건부 UPDATE(`… WHERE selected_idea_id IS NULL`의 영향 행 수 확인)나 낙관적 잠금(`@Version`)을 쓴다. 기존 평가 저장이 DB UNIQUE 위반을 409로 바꾸는 것과 같은 계열의 문제다.
