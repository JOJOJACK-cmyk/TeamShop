# TeamShop 개인 프로젝트

생활용품 쇼핑몰을 구현한 Spring MVC 기반 개인 프로젝트입니다. 주방·리빙·문구 상품의 탐색부터 장바구니, 주문·취소, 관리자 상품 관리까지 제공합니다. 실제 결제와 배송은 포함하지 않습니다.

## 실행

Java 17 이상, Maven 3.9 이상이 필요합니다.

```bash
mvn test
mvn spring-boot:run
```

브라우저에서 http://localhost:8080 에 접속합니다. 기본 H2 파일 DB가 `data/`에 생성되며 첫 실행 시 상품 10개와 로컬 시연 계정이 생성됩니다.

| 권한 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 회원 | demo@teamshop.local | Demo1234! |
| 관리자 | admin@teamshop.local | Admin1234! |

이 계정은 H2 시연 전용입니다. 공개 서비스로 운영하기 전에 시연 계정 제거, HTTPS, 비밀정보 설정과 별도 운영 검토가 필요합니다.

## MySQL 실행

MySQL 8에서 아래 명령으로 DB를 만든 후 전용 DB 사용자를 생성하고 권한을 부여합니다.

```sql
CREATE DATABASE teamshop CHARACTER SET utf8mb4;
```

환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정하고 실행합니다. `DB_URL` 기본값은 `jdbc:mysql://localhost:3306/teamshop?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`입니다.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

MySQL 프로필에서는 데모 계정과 상품을 자동 생성하지 않습니다. 회원가입 후 DB에서 관리자 대상 계정의 role을 ADMIN으로 지정하면 상품 등록이 가능합니다.

```sql
UPDATE members SET role='ADMIN' WHERE email='본인의 가입 이메일';
```

DDL: [docs/schema.sql](docs/schema.sql). 기본 H2에서 실행 검증했으며 MySQL 별도 서버 연결 검증은 수행하지 않았습니다.

## 기능

- 회원가입, BCrypt 비밀번호 저장, 세션 로그인·로그아웃
- 상품명 검색, 카테고리 필터, 8개 단위 페이지 이동, 상품 상세
- 회원별 장바구니 추가·수량 변경·삭제
- 배송정보 입력, 서버 가격 재계산, 주문내역·주문상세
- 트랜잭션 기반 재고 차감과 주문 취소 시 재고 복원
- 관리자 상품 등록·수정·판매 중지/재개
- CSRF 검사, 관리자 URL 권한, 본인 주문 접근 검사

## 구조

`ShopController`가 요청·입력을 처리하고 `ShopService`가 주문 트랜잭션을 담당합니다. `ShopRepository`는 JdbcTemplate과 파라미터 바인딩으로 SQL을 실행합니다. Thymeleaf가 HTML을 렌더링합니다. Spring Boot 3.5.7 / Spring Framework 6 / Spring Security / Spring JDBC를 사용합니다.

주문 시 회원 행을 잠가 같은 회원의 장바구니 변경을 직렬화합니다. 상품 ID 순서로 상품 행을 잠근 뒤 재고를 확인합니다. 전체 항목 중 하나라도 실패하면 차감·주문 저장을 모두 롤백합니다. 주문 상세에는 주문 당시 상품명과 단가를 보관합니다. 상품은 물리 삭제 대신 판매 중지합니다.

## 개인 프로젝트 산출물

- `docs/설계정의서.md`: 요구사항, 기능·테이블·화면 정의, 사이트맵
- `docs/시연시나리오.md`: 실제 시연 순서와 검증 항목
- `docs/버전관리가이드.md`: 기능별 브랜치와 커밋 관리 방법
- 결과보고서 PPTX와 시연 MP4는 별도 전달 자료


기능별 브랜치와 커밋으로 개인 프로젝트의 변경 이력을 관리합니다. 설계, 구현, 테스트와 문서를 함께 갱신하고 GitHub Actions에서 빌드와 테스트를 실행합니다.
