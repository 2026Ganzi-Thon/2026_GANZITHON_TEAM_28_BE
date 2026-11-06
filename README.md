# Team28 Backend

무기력한 사람을 위한 초소형 행동 지원 서비스 (서비스명 미정) 백엔드

## 기술 스택
- Java 21, Spring Boot 4.0.0, Gradle 8.14.3
- Spring Data JPA, MySQL 8 (로컬은 Docker), Flyway
- Swagger (springdoc-openapi 3.x)
cd
## 로컬 실행
```bash
cp .env.example .env
docker compose up -d mysql
docker compose ps            # team28-mysql 이 healthy 인지 확인
./gradlew bootRun
```

- Health: http://localhost:8080/api/health
- Swagger: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

## 규칙
- DB 테이블 변경은 `src/main/resources/db/migration` 에 새 버전 파일(`V2__...sql`)을 추가합니다. 이미 반영된 파일은 수정하지 않습니다.
- 응답은 `ApiResponse`, 에러는 `throw new GeneralException(ErrorStatus.XXX)` 를 사용합니다.
- `.env` 는 커밋하지 않습니다.
- 작업 브랜치는 `dev` 에서 `feat/기능명` 으로 만들고 `dev` 로 PR 을 올립니다.
