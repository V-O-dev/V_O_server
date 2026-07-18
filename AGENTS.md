# AGENTS.md

> V_O_server에 적용된 Orchestra 멀티모델 파이프라인 프로젝트 규칙.
> 전체 사용법은 [docs/orchestra-pipeline-guide.md](docs/orchestra-pipeline-guide.md) 참고. (둘 다 로컬 전용 — git 미추적)

## 프로젝트 개요

V_O 서비스의 **백엔드 API 서버**. "매일 10초, 질문으로 열리는 우리들의 진짜 일상" — 초대코드/QR로만 입장하는
폐쇄형 그룹에서 매일 배정되는 질문에 10초 영상으로 답하고, 상호 호혜성(내가 올려야 남의 것이 열림) 피드로
소통하며, 개인 달력에 아카이빙하는 서비스. 인증은 **OAuth 소셜 로그인(KAKAO/GOOGLE/APPLE)** 기준.
도메인·기능·데이터 모델은 `docs/`의 기획안·기능명세서·API 명세서·ERD 문서를 정본으로 참조한다.

## 기술 스택

- 언어: **Java 21** (toolchain)
- 프레임워크: **Spring Boot 4.1.0** — Web MVC, Data JPA, Security, Validation
- DB/ORM: **PostgreSQL** + JPA/Hibernate (`ddl-auto=update`)
- API 문서: springdoc-openapi (Swagger UI) 3.0.3
- 보조: Lombok, Spring Boot DevTools
- 테스트: **JUnit Platform** (Spring Boot Test) — `./gradlew test`
- 빌드/실행: **Gradle** (`./gradlew` / Windows `gradlew.bat`), 패키지 베이스 `com.example.v_o_server`

## 절대 수정 금지 파일 목록

<!-- 여기 명시된 파일이 코드 작업 diff에 나타나면 Codex 리뷰 verdict는 BLOCKED.
     명시적으로 "이 파일을 바꾸는 작업"으로 계획된 경우만 예외. -->

- **비밀정보/설정**: `.env`, `.env.*`, `src/main/resources/application*.properties` (DB 크레덴셜 포함)
- **빌드/래퍼**: `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/**`
- **인프라**: `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- **VCS/CI**: `.github/**`, `.gitignore`, `.gitattributes`, `.git/**`
- **생성물**: `build/**`, `.gradle/**`
- **참고 문서 (Notion 스냅샷·가이드 — 코드 작업 중 변경 금지)**:
  `docs/README.md`, `docs/기획안.md`, `docs/기능명세서.md`, `docs/API-명세서.md`, `docs/컨벤션.md`, `docs/ERD.md`,
  `docs/변수명-표준.md`, `docs/orchestra-pipeline-guide.md`
  - 예외: 파이프라인 산출물 `docs/plans/`, `docs/reports/`, `docs/specs/` 는 정상 작업 대상이다.
  - 예외: 문서 동기화가 **명시적 작업 목표**일 때만 위 참고 문서를 수정한다.

## 워크플로우 규칙

1. **모든 작업 순서**
   - `/plan-codex` 로 계획 수립 → **Codex 감사**(APPROVED까지, 최대 3라운드) → **위험도 게이트** → 구현·리뷰(`/Codex-codex`)
   - 계획 없이 곧바로 구현하지 않는다.
2. **계획 파일 맨 앞 "사용자 요약" 블록 (필수)**
   계획서 최상단에 반드시 포함한다:
   - **끝나면 무엇이 달라지나** — 평문 3~5줄, 함수명·경로 대신 사용자가 체감하는 동작 중심으로.
   - **위험도: 낮음 / 중간 / 높음**
     - 낮음: 수정 파일 ≤ 3 AND 관련 기존 테스트 존재 AND 데이터/인증/외부연동 무관.
     - 중간: 위 셋 중 하나라도 벗어남.
     - 높음: DB 스키마·인증·크레덴셜·삭제(destructive)·마이그레이션 중 하나라도 포함.
   - 두 등급 사이에서 애매하면 **더 높은 쪽**으로 판정한다.
3. **위험도 게이트**
   - **낮음** → 사용자 승인 없이 **바로 구현으로 진행**한다.
   - **중간 이상** → **사용자 요약 블록만** 보여주고 승인을 기다린다. (전체 계획서는 요청 시에만 제시)
   - 위험도와 무관하게 구현 중 Codex 리뷰는 절대 생략하지 않는다.
4. **파일 위치 규칙 / GitHub 정책**
   - 계획: `docs/plans/<작업명>.md`
   - 명세(spec): `docs/specs/`
   - 작업 보고서: `docs/reports/YYYY-MM-DD-작업명.md` (`docs/reports/_TEMPLATE.md` 복사해서 작성)
   - **이 프로젝트에서는 Orchestra 산출물 전체를 git에 커밋하지 않는다** (팀 GitHub 미공유, `.git/info/exclude`로 로컬 무시).
     `AGENTS.md`·`docs/**` 모두 로컬 전용. 팀과 공유가 필요해지면 그때 개별적으로 추적 전환한다.
5. **보고서 필수 항목** (`_TEMPLATE.md` 구조를 따른다)
   - 목적 / 변경 파일 목록과 각 파일을 바꾼 이유 / 테스트 결과(실행 명령 + 통과/실패 증거) / 남은 이슈
   - **리뷰·수정 이력** — 감사·리뷰 라운드별 verdict(APPROVED/WARNING/BLOCKED)와, 각 지적사항을
     "**지적 → 심각도 → 수용/반박 판단 → 조치 → 결과**" 표로 보기 좋게 정리한다.
6. **테스트 기준선 (regression 확인)**
   - 구현 **전에** 관련 테스트를 먼저 실행해 기준선(RED)을 확보한다.
   - 구현 **후에** 동일한 테스트를 다시 실행해 회귀가 없는지(GREEN) 확인한다.
   - 두 결과 모두 보고서의 "테스트 결과"에 기록한다.
7. **범위 준수 (scope / no-drift)**
   - 계획/명세에 명시되지 않은 파일은 수정하지 않는다. (특히 위 "절대 수정 금지" 목록)
   - 불가피한 부수 변경(import 정리 등)은 보고서에 이유와 함께 남긴다.
   - 리뷰어는 [SCOPE] / [NO-DRIFT] / [TEST] / [REPORT] 를 최우선으로 검토한다.
8. **비밀정보 보호**
   - `.env*`, `*secret*`, `*credential*`, `*.pem`, `*.key` 는 외부 에이전트/MCP(Codex)에 절대 전달하지 않는다.

## 파이프라인 역할

- **Codex** — 오케스트레이터 겸 구현자
- **Codex (MCP, gpt-5.5)** — 계획 감사자 겸 코드 리뷰어 (APPROVED / WARNING / BLOCKED, 최대 3라운드)
- **사용자** — 작업 결정과 최종 승인
