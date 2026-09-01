# skills

프로젝트 AI 스킬을 여기에 추가한다.

## 새 스킬 추가

1. `skills/<skill-name>/` 디렉터리를 만든다.
2. 아래 템플릿으로 `SKILL.md` 를 작성한다.
3. 필요하면 `reference.md`, `examples.md`, `scripts/` 를 추가한다.

```markdown
---
name: skill-name
description: 무엇을 하는지. Use when 사용자가 ~를 요청할 때.
---

# Skill Title

## Instructions
1. ...
2. ...

## Examples
- ...
```

## 스킬 목록

### Spec Kit (GitHub spec-kit v0.12.2)

`specify init` 으로 설치된 스펙 주도 개발(SDD) 워크플로 스킬. 실행에 `.specify/` 디렉터리가 필요하다.

| 스킬 | 설명 |
|------|------|
| `speckit-constitution` | 프로젝트 원칙(`.specify/memory/constitution.md`) 작성·개정 |
| `speckit-specify` | 자연어 기능 설명으로 기능 명세(spec) 작성 |
| `speckit-clarify` | 명세의 모호한 부분을 구조화된 질문으로 확정 (plan 전, 선택) |
| `speckit-plan` | 구현 계획(plan) 수립 |
| `speckit-tasks` | 계획을 실행 가능한 작업 목록으로 분해 |
| `speckit-analyze` | 산출물 간 일관성·정합성 리포트 (tasks 후, 선택) |
| `speckit-checklist` | 요구사항 완전성·명확성 검증 체크리스트 생성 (plan 후, 선택) |
| `speckit-implement` | 작업 목록에 따라 구현 실행 |
| `speckit-converge` | 코드베이스 현황을 평가해 남은 작업을 tasks에 추가 |
| `speckit-taskstoissues` | 작업 목록을 이슈로 등록 |

권장 순서: `constitution` → `specify` → (`clarify`) → `plan` → `tasks` → (`analyze`) → `implement`

### 커밋

변경이력을 취합해 요약하고 저장소 관례에 맞는 한국어 메시지로 커밋한다.

| 스킬 | 설명 |
|------|------|
| `commitb` | 변경이력 취합 → 요약 → 커밋 (푸시하지 않음) |
| `commitbp` | `commitb` + 원격 푸시까지 |

두 스킬 모두 커밋 전에 **범위 검증**을 한다 — 이번 작업과 무관한 파일이 섞였으면 빼고 그 사실을 보고한다. `commitbp`는 기본·보호 브랜치(`main`, `develop` 등)에 푸시하기 전에 확인을 받는다.

### 리뷰

지정한 로컬 베이스 브랜치와 현재 HEAD를 three-dot diff로 비교해 리뷰한다. 베이스 브랜치 입력이 필수다.

| 스킬 | 설명 |
|------|------|
| `reviewdog-java-secure` | Java 보안성 검토·코드 리뷰. `vulnerabilities/` 아래 취약점 유형별 기준 문서를 참조한다 |
| `reviewdog-specs` | 스펙·마크다운 산출물의 정합성과 문장 검토 |

### 테스트

| 스킬 | 설명 |
|------|------|
| `tc-example-generator` | `specs/` 기능 번호를 받아 Speckit 산출물을 취합하고, 시험항목·목적·절차·이력을 담은 자체시험 결과서를 `.xlsx`로 생성 |

> 슬래시 명령으로 인식되는 경로는 Cursor가 `.cursor/skills/`, Claude Code가 `.claude/skills/` 이며 둘 다 `.gitignore` 대상이라 커밋되지 않는다.
> 여기(`.agent/skills/`)가 커밋 대상이자 기준이다. `pwsh .agent/scripts/link-skills.ps1` 로 두 경로에 정션을 걸어 두면 여기만 고쳐도 양쪽에 반영된다. 자세한 내용은 [../README.md](../README.md#ai-도구와의-연결).
