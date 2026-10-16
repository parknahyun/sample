# 구현 계획 (Implementation Plan): [FEATURE]

**브랜치**: `[###-feature-name]` | **날짜**: [DATE] | **명세**: [링크]

**입력**: `/specs/[###-feature-name]/spec.md`의 기능 명세

**참고**: 이 템플릿은 `/speckit-plan` 명령이 채우며, 실행 워크플로는 해당 명령의 정의에 기술되어 있다.

## 요약 (Summary)

[기능 명세에서 추출: 핵심 요구사항 + 조사에서 도출한 기술적 접근 방식]

## 기술 맥락 (Technical Context)

<!--
  필수 작업: 이 섹션의 내용을 프로젝트의 기술 세부사항으로 교체할 것.
  여기의 구조는 반복 작업을 안내하기 위한 참고용이다.
-->

**언어/버전**: [예: Python 3.11, Swift 5.9, Rust 1.75 또는 NEEDS CLARIFICATION]

**주요 의존성**: [예: FastAPI, UIKit, LLVM 또는 NEEDS CLARIFICATION]

**저장소**: [해당하는 경우, 예: PostgreSQL, CoreData, 파일 또는 N/A]

**테스트**: [예: pytest, XCTest, cargo test 또는 NEEDS CLARIFICATION]

**대상 플랫폼**: [예: Linux 서버, iOS 15+, WASM 또는 NEEDS CLARIFICATION]

**프로젝트 유형**: [예: library/cli/web-service/mobile-app/compiler/desktop-app 또는 NEEDS CLARIFICATION]

**성능 목표**: [도메인별, 예: 1000 req/s, 초당 1만 줄, 60 fps 또는 NEEDS CLARIFICATION]

**제약 조건**: [도메인별, 예: p95 200ms 미만, 메모리 100MB 미만, 오프라인 지원 또는 NEEDS CLARIFICATION]

**규모/범위**: [도메인별, 예: 사용자 1만 명, 100만 LOC, 화면 50개 또는 NEEDS CLARIFICATION]

## 헌법 점검 (Constitution Check)

*게이트: Phase 0 조사 전에 통과해야 한다. Phase 1 설계 후 다시 점검한다.*

[헌법 파일을 기준으로 결정된 게이트]

## 프로젝트 구조 (Project Structure)

### 문서 (이 기능)

```text
specs/[###-feature]/
├── plan.md              # 이 파일 (/speckit-plan 명령 출력)
├── research.md          # Phase 0 출력 (/speckit-plan 명령)
├── data-model.md        # Phase 1 출력 (/speckit-plan 명령)
├── quickstart.md        # Phase 1 출력 (/speckit-plan 명령)
├── contracts/           # Phase 1 출력 (/speckit-plan 명령)
└── tasks.md             # Phase 2 출력 (/speckit-tasks 명령 - /speckit-plan이 생성하지 않음)
```

### 소스 코드 (저장소 루트)
<!--
  필수 작업: 아래 자리표시자 트리를 이 기능의 구체적인 레이아웃으로 교체할 것.
  사용하지 않는 옵션은 삭제하고, 선택한 구조를 실제 경로(예: apps/admin,
  packages/something)로 확장한다. 최종 계획에는 옵션 라벨이 포함되어서는 안 된다.
-->

```text
# [사용하지 않으면 삭제] 옵션 1: 단일 프로젝트 (기본)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [사용하지 않으면 삭제] 옵션 2: 웹 애플리케이션 ("frontend" + "backend"가 감지된 경우)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [사용하지 않으면 삭제] 옵션 3: 모바일 + API ("iOS/Android"가 감지된 경우)
api/
└── [위 backend와 동일]

ios/ 또는 android/
└── [플랫폼별 구조: 기능 모듈, UI 흐름, 플랫폼 테스트]
```

**구조 결정**: [선택한 구조를 문서화하고 위에서 정한 실제 디렉터리를 참조]

## 복잡성 추적 (Complexity Tracking)

> **헌법 점검에서 정당화가 필요한 위반이 있을 때만 작성**

| 위반 사항 | 필요한 이유 | 더 단순한 대안을 기각한 이유 |
|-----------|------------|-------------------------------------|
| [예: 4번째 프로젝트] | [현재 필요] | [프로젝트 3개로 부족한 이유] |
| [예: 리포지토리 패턴] | [구체적인 문제] | [직접 DB 접근이 부족한 이유] |
