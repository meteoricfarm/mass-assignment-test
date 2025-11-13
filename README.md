# Spring Boot: Mass Assignment & XSS 취약점 데모

이 프로젝트는 Java Spring Boot 애플리케이션에서 발생하는 두 가지 일반적인 웹 취약점, **Mass Assignment(대량 할당)**와 **저장형 XSS(Stored Cross-Site Scripting)**를 시연하고, 이를 해결하는 방법을 보여주기 위해 제작되었습니다.

Coverity와 같은 SAST(정적 분석) 도구에서 탐지되는 보안 이슈를 재현하고 검증하는 데 사용할 수 있습니다.

---

## 🚀 프로젝트 실행 및 환경

이 프로젝트는 별도의 DB 설치 없이 H2 인메모리 데이터베이스를 사용합니다.

### 1. 전제 조건

* Java 17
* Maven (`./mvnw`) 또는 Gradle (`./gradlew`)
* `curl` (터미널에서 테스트용)

### 2. 실행 방법

1.  프로젝트를 클론하고 디렉터리로 이동합니다.
    ```bash
    git clone [YOUR_REPOSITORY_URL]
    cd mass-assignment-test
    ```

2.  애플리케이션을 실행합니다.
    * Maven 사용 시:
        ```bash
        ./mvnw spring-boot:run
        ```
    * Gradle 사용 시:
        ```bash
        ./gradlew bootRun
        ```

3.  서버가 실행되면 `http://localhost:8080/h2-console`에 접속하여 H2 데이터베이스를 확인할 수 있습니다.
    * **JDBC URL:** `jdbc:h2:mem:testdb`
    * **Username:** `sa`
    * **Password:** (비워두기)

---

## 🔒 1. Mass Assignment 취약점 테스트

애플리케이션이 시작되면 `ID=1`, `role='USER'`인 테스트 사용자가 자동으로 생성됩니다.

### 1-1. 공격 시나리오 (취약한 엔드포인트)

1.  **[사전 확인]** H2 콘솔에서 쿼리를 실행하여 현재 `role`을 확인합니다.
    ```sql
    SELECT * FROM USERS WHERE ID = 1;
    ```
    > **결과:** `ID`=1, `USERNAME`='normalUser', **`ROLE`='USER'**

2.  **[공격 수행]** `curl`을 사용해 `role=ADMIN` 파라미터를 포함한 악의적인 요청을 `/profile/update_vulnerable` 엔드포인트로 전송합니다.
    ```bash
    curl -X POST http://localhost:8080/profile/update_vulnerable \
         -d "id=1" \
         -d "username=HACKED" \
         -d "email=hacked@example.com" \
         -d "role=ADMIN"
    ```

3.  **[결과 확인]** 다시 H2 콘솔에서 쿼리를 실행합니다.
    ```sql
    SELECT * FROM USERS WHERE ID = 1;
    ```
    > **결과:** `ID`=1, `USERNAME`='HACKED', **`ROLE`='ADMIN'**
    >
    > **(공격 성공) ⚠️** `User` 엔티티가 `role` 필드를 직접 바인딩하여 권한이 탈취되었습니다.

### 1-2. 방어 시나리오 (안전한 엔드포인트)

1.  **[테스트 준비]** 서버를 재시작하여 DB를 초기화합니다. (`ID=1` 사용자가 `ROLE='USER'`로 복구됩니다.)

2.  **[공격 수행]** 동일한 악성 페이로드를 DTO를 사용하는 `/profile/update_safe/1` 엔드포인트로 전송합니다.
    ```bash
    curl -X POST http://localhost:8080/profile/update_safe/1 \
         -d "username=SAFE_UPDATE" \
         -d "email=safe@example.com" \
         -d "role=ADMIN"
    ```

3.  **[결과 확인]** H2 콘솔에서 쿼리를 실행합니다.
    ```sql
    SELECT * FROM USERS WHERE ID = 1;
    ```
    > **결과:** `ID`=1, `USERNAME`='SAFE_UPDATE', **`ROLE`='USER'**
    >
    > **(방어 성공) ✅** `UserUpdateDto`가 `role` 필드를 허용하지 않아 악의적인 값이 무시되었습니다. `username`은 정상적으로 업데이트되었습니다.

---

## 🛡️ 2. Stored XSS 취약점 및 방어 테스트

Mass Assignment를 방어했더라도, DTO로 받은 값은 여전히 '신뢰할 수 없는' 데이터입니다.

### 2-1. XSS 공격 및 방어 시나리오

1.  **[테스트 준비]** (필요시) 서버를 재시작하여 DB를 초기화합니다.

2.  **[공격 수행]** `username` 필드에 XSS 페이로드(스크립트 태그)를 삽입하여 안전한 엔드포인트로 전송합니다.
    ```bash
    curl -X POST http://localhost:8080/profile/update_safe/1 \
         -d "username=<script>alert('XSS')</script>" \
         -d "email=xss@example.com"
    ```

3.  **[결과 확인]** H2 콘솔에서 쿼리를 실행하여 DB에 저장된 값을 확인합니다.
    ```sql
    SELECT * FROM USERS WHERE ID = 1;
    ```
    > **결과 (방어 성공) ✅**
    >
    > `USERNAME` 필드에 `<script>alert('XSS')</script>`가 아닌, HTML 인코딩된 `&lt;script&gt;alert('XSS')&lt;/script&gt;`가 저장된 것을 볼 수 있습니다.
    >
    > 이는 `UserController`의 `updateUserSafe` 메소드 내부에서 `HtmlUtils.htmlEscape`를 통해 입력값을 정화(Sanitization)했기 때문입니다. 이 값은 나중에 웹페이지에 렌더링되더라도 스크립트로 실행되지 않고 문자열 그대로 보이게 됩니다.

---

## 📁 주요 파일 설명

* **`UserController.java`**:
    * `update_vulnerable(...)`: `User` 엔티티를 직접 파라미터로 받아 Mass Assignment에 취약한 메소드.
    * `update_safe(...)`: `UserUpdateDto`를 사용하여 Mass Assignment를 방어하고, `HtmlUtils.htmlEscape`를 통해 Stored XSS를 방어하는 안전한 메소드.
* **`User.java`**:
    * JPA 엔티티. `role`이라는 민감한 필드를 포함합니다.
* **`UserUpdateDto.java`**:
    * Mass Assignment를 방어하기 위한 DTO (Data Transfer Object).
    * `role` 필드 자체가 존재하지 않아 바인딩될 수 없습니다.
* **`DataInitializer.java`**:
    * 애플리케이션 시작 시 테스트용 `USER`를 생성합니다.
* **`application.properties`**:
    * H2 인메모리 DB 설정 및 H2 콘솔을 활성화합니다.