---
name: reviewdog-specs
description: >
  스펙·마크다운 산출물 로컬 브랜치 리뷰(reviewdog-specs). 사용자가 지정한 로컬 베이스 브랜치와 현재 HEAD를
  three-dot diff로 비교한다(베이스 브랜치 입력 필수). 메인 에이전트가 Task 서브를 띄워 references/guideline-specs.md와
  저장소 루트 CLAUDE.md를 근거로 diff 리뷰(## PR 리뷰 결과)를 받고, 필수·권장을 파싱·적용한다. 미적용 필수가 남으면 diff를 갱신해 서브를
  재호출한다(서브 호출 최대 3회). "reviewdog-specs", "reviewdog", "로컬 브랜치 대비 스펙 리뷰", "guideline-specs 로컬 diff"
  요청 시 사용한다.
---

# reviewdog-specs — 스펙·문서 산출물 (로컬 브랜치 diff)

**본 스킬**: `spec.md`, `plan.md`, 계약 문서 등 **스펙·마크다운 산출물**을 기본 대상으로 한다.

**목표**: 사용자가 **반드시 지정한 로컬 베이스 브랜치**와 **현재 작업 브랜치(HEAD)** 사이의 변경을 `git` three-dot diff로 확보하고, **서브 에이전트**는 **[references/guideline-specs.md](references/guideline-specs.md)**(절차·출력 형식)와 **저장소 루트 `CLAUDE.md`**(프로젝트 계약·규약)를 함께 참조해 `## PR 리뷰 결과` 형식으로 리뷰한다. 메인이 **`### 필수 수정` / `### 권장 수정`** 을 파싱·적용한 뒤, **미적용 필수**가 남으면 diff를 다시 모아 **서브를 재호출**한다. **서브 에이전트 호출은 최대 3회**다.

## 전제 (NON-NEGOTIABLE)

1. **로컬 베이스 브랜치 이름**이 사용자 메시지에 없으면 진행하지 않고, 비교 기준이 될 **로컬 브랜치명**을 요청한 뒤 **중단**한다. (예: `release/R1.0.0`, `main`, `develop` — 원격 전용 ref만 있고 로컬에 없으면 `git fetch` 후 tracking 브랜치 생성 또는 사용자에게 로컬 확보를 안내한다.)
2. **서브 에이전트**는 절차·출력의 **1차 규범**으로 **`reviewdog-specs/references/guideline-specs.md`** 를 따른다. **프로젝트 팀 계약**으로 저장소 **루트 `CLAUDE.md`** 를 **반드시 Read** 하여, diff가 API·권한·리스트 응답 스키마·스펙·시드 등과 충돌하는지 점검한다(`guideline-specs.md`에 역할이 정리됨). 서브는 **`## PR 리뷰 결과` … 블록만** 반환하고 **즉시 종료**한다(저장소 수정·`git`·MCP·추가 질문 없음). `CLAUDE.md` 는 일반 마크다운이며 파일명은 관례일 뿐 **읽기·해석에 제약 없음**.
3. 메인은 Cursor **`Task`** 도구로 서브를 띄운다. **`readonly: true`** 로 두어 서브가 워크스페이스를 바꾸지 않게 한다.

## 역할 분리

| 역할 | 하는 일 |
|------|---------|
| **메인 에이전트** | 베이스 브랜치 확정 → `git`으로 three-dot diff 수집 → **Task(서브)** 프롬프트 조립·호출 → 서브 출력 수신 → `### 필수 수정` / `### 권장 수정` 파싱 → 워크스페이스에 `before`/`after` 적용 → **미적용 필수** 여부 판단 → (조건 충족 시) diff 갱신 및 서브 재호출 → 최종 요약 |
| **서브 에이전트** | **guideline-specs.md** + **`CLAUDE.md` Read** 후, 메인이 넘긴 **비교 맥락 + 통합 diff**(및 회차 2 이상이면 **로컬 미커밋 diff** 블록)를 근거로 리뷰 마크다운 **한 블록**만 생성·반환 |

## 참조 (메인이 스펙 파일 보강 시)

| 파일 | 용도 |
|------|------|
| [references/guideline-specs.md](references/guideline-specs.md) | 서브: 절차·출력 형식·`CLAUDE.md` 연동 방법(**서브 입력 스펙 포함**) |
| 저장소 루트 `CLAUDE.md` | 서브: 프로젝트 아키텍처·API·권한·리스트 응답·스펙·시드 등 **팀 계약** (서브가 Read) |
| [../reviewdog-pr-specs/references/spec-review-rules.md](../reviewdog-pr-specs/references/spec-review-rules.md) | diff에 `specs/**/spec.md` 등이 포함될 때 메인이 **적용 후** 선택 적용 |

서브 프롬프트에는 **guideline-specs.md** 규범을 포함하고, **`CLAUDE.md` 는 저장소에서 Read** 하도록 지시한다. `spec-review-rules.md` 는 서브 규범으로 강제하지 않는다([reviewdog-pr-specs](../reviewdog-pr-specs/SKILL.md)와 동일).

**참고**: 일부 환경에서 `../reviewdog-pr-specs/` 가 없을 수 있다. 그때는 `spec-review-rules.md` 링크를 건너뛰고 **guideline-specs.md** 와 본 파일만으로 루프를 수행한다.

---

## 외부 루프 (서브 호출 최대 3회)

변수: `cycle ∈ {1,2,3}`. **한 `cycle`당 서브 에이전트를 정확히 한 번** 호출한다(총 ≤ 3회).

### A. Diff 수집 (메인, 매 회차 시작)

저장소 루트에서 실행. `<BASE>` 는 사용자가 지정한 **로컬 베이스 브랜치명**이다.

#### 1) 베이스 존재 확인

```bash
git rev-parse --verify <BASE>
```

실패 시: 브랜치 목록을 안내하고 사용자에게 로컬 베이스 확보 또는 이름 수정을 요청한 뒤 중단한다.

#### 2) 커밋 범위 (선택, 요약용)

```bash
git log --oneline <BASE>..HEAD
```

#### 3) 통합 diff (three-dot)

```bash
git diff --no-color <BASE>...HEAD
git diff --stat <BASE>...HEAD
```

#### 4) 회차 2·3 — 워킹 트리 반영분 병합

직전 회차에서 워크스페이스를 수정했다면, 서브 입력 패킷에 아래를 **덧붙인다**.

```bash
git diff --no-color HEAD --
```

서브에게 “**첫 블록**은 `<BASE>...HEAD` 커밋 대비 변경, **추가 블록**은 로컬 미커밋 변경”임을 한 줄로 알린다. 변경이 없으면 생략 가능.

#### 5) 경로 한정

사용자가 경로를 지정했다면 §3·§4 명령 끝에 `-- path…` 를 붙인다.

---

### B. 서브 에이전트 프롬프트 (메인이 조립)

다음을 **한 번에** 서브에게 넘긴다.

1. 지시: “당신은 **읽기 전용** 스펙/문서 리뷰어다. **`git`·MCP를 호출하지 말 것.** 저장소 **수정·쓰기는 금지**한다. **예외**: 워크스페이스에서 **`CLAUDE.md` 만 Read** 하여 프로젝트 계약을 반영한다(경로: 저장소 루트 `<workspace>/CLAUDE.md`).”
2. **반드시 준수할 규범**: `.agents/skills/reviewdog-specs/references/guideline-specs.md` 전문을 포함하거나, 서브가 파일을 읽을 수 있으면 해당 **절대 경로를 지정해 Read 한 뒤** 동일 규칙을 적용하라고 명시한다. **직후에 `CLAUDE.md` Read** 를 동일하게 명시한다.
3. **맥락**: 현재 브랜치명(`git branch --show-current`), 베이스 `<BASE>`, 비교식 `<BASE>...HEAD`, 짧은 SHA(선택), **`외부 회차: cycle/3`**.
4. **입력 데이터**: (A)에서 만든 diff 문자열(들). 전체 diff를 **리뷰 결과에 그대로 붙이지 말 것**(guideline-specs.md 규칙과 동일).
5. **출력 형식**: `## PR 리뷰 결과` 로 시작하는 **단일 마크다운 블록만**. `### 필수 수정`·`### 권장 수정` 은 guideline에 따라 **항상 존재**; 이슈 없으면 `(해당 없음)` 등으로 **비워두지 않는다**.

**Cursor에서의 “새 에이전트”**: **`Task`** 도구, `readonly: true`, `subagent_type`: `generalPurpose` 또는 저장소 탐색이 거의 없으면 적절한 유형.

---

### C. 메인: 파싱·적용

서브 응답에서 `## PR 리뷰 결과` … 블록을 추출한다.

1. **`### 필수 수정`**: [guideline-specs.md](references/guideline-specs.md) bullet 구조로 **실질 필수**(적용 가능한 조치가 있는 항목)를 식별한다.
   - **실질 필수 0건**(`(해당 없음)`만 등): 사용자가 “필수만”이 아니라면 **`### 권장 수정`** 만 적용 시도 후 **§D로 이동**(조기 종료 조건 확인).
2. **실질 필수 ≥ 1건**: **필수 전부**에 대해 `before`/`after` 기준으로 적용을 시도한다. (**같은 `cycle` 안에서는 서브를 다시 부르지 않는다.**)
3. **권장**: 사용자가 “필수만”을 요청하지 않았으면, 필수 시도 후 같은 회차에서 권장을 적용한다.

**파싱 규칙** ([reviewdog-pr-specs](../reviewdog-pr-specs/SKILL.md)와 동일 계약):

- 위치: `[파일:라인]` 또는 `` `path/to/file.md:42` ``.
- 중첩 bullet: `before:` / `after:` 로 치환.
- `before`/`after` 없이 설명만 있으면 **추론 적용** 가능 시 수정하고 항목에 “추론 적용”으로 표시.

**적용 후 검증**(선택): 대상이 `specs/**/spec.md` 등이면 메인은 [spec-review-rules.md](../reviewdog-pr-specs/references/spec-review-rules.md)를 짧게 재점검한다.

**파일 수정 시**:

- 라인 번호는 힌트-only. **`before` 텍스트**와 앞뒤 문맥으로 고유 위치를 찾는다.
- `before`가 **정확히 한 번**만 나오면 `after`로 교체.
- 여러 번 나오면 사용자에게 짧게 확인하거나 첫 매칭만 적용·나머지는 **미완료 필수**로 다음 회차에 넘긴다.
- 찾을 수 없으면 **미적용 필수**로 남긴다.

---

### D. 메인: 다음 회차 여부 (필수가 “없어질 때까지” + 상한)

- **미적용 필수**(적용 실패·미매칭·모호 등)가 **1건이라도** 남았고 **`cycle < 3`** 이면 `cycle ← cycle + 1` 하고 **§A로 돌아간다**(새 서브 호출).
- **미적용 필수 0건**이면 **루프 종료**(추가 서브 호출 없음).
- **`cycle == 3`** 까지 진행했는데도 미적용 필수가 남으면 **중단**하고 최종 요약에 **“서브 3회 상한 도달”**을 명시한다.

---

### E. 헤더·섹션 검증 (메인, 서브 출력 직후)

서브가 규범을 어기면 같은 회차에서 **guideline-specs.md에 맞게** `### 필수 수정` / `### 권장 수정` 만 고쳐 쓰라고 **한 번** 요청할 수 있다. 그래도 실패하면 파싱 가능한 범위만 적용하고 미적용을 §D로 넘긴다.

---

## 최종 사용자 출력 (루프 종료 후 한 번)

```markdown
## reviewdog-specs 적용 요약
- 베이스 브랜치: <BASE>
- 현재 브랜치: <HEAD 브랜치명>
- 비교: <BASE>...HEAD
- 서브 호출 횟수: K/3
- 종료 사유: 미적용 필수 0건 / 또는 서브 3회 상한

## 수정 반영한 파일
- …

## 미적용 필수 (남은 경우만)
| # | 파일 | 사유 | 다음 액션 |
|---|------|------|----------|

## 미적용 권장 (남은 경우만)
| # | 파일 | 사유 | 다음 액션 |
|---|------|------|----------|
```

가능하면 **마지막 회차의 전체** `## PR 리뷰 결과` 블록을 그대로 포함한다(길면 필수·권장·요약만 유지).

---

## 메인이 하지 않는 것

- **로컬 베이스 브랜치 없이** 서브를 돌리지 않는다.
- **한 회차 안에서** 서브를 두 번 호출하지 않는다(적용 실패 시에도 동일 회차 재호출 금지 → 다음 `cycle`에서 재시도).
- 서브 규범으로 **`markdown-specdoc.md`** 나 **`spec-review-rules.md`** 전체를 프롬프트에 붙여 넣지 않는다. 서브는 **guideline-specs.md** + **저장소 `CLAUDE.md` Read** 조합을 쓴다.

## 주의

- 커밋 범위(`<BASE>...HEAD`)와 **적용 후 워킹 트리**가 섞이므로, 종료 후 사용자에게 **커밋 여부**를 안내한다.
- 베이스 브랜치가 오래되었으면 `git merge-base` 관점에서 three-dot 결과가 기대와 다를 수 있다 — 필요 시 사용자와 ref를 합의한다.
- Bitbucket **PR URL**이 있고 MCP diff 루프가 필요하면 **[reviewdog-pr-specs](../reviewdog-pr-specs/SKILL.md)** 를 사용한다.

## 상호 참조

- 서브 에이전트 규범·출력 골격·입력 정의·`CLAUDE.md` 연동: [references/guideline-specs.md](references/guideline-specs.md)
- PR URL·MCP diff·동일 루프(최대 3회): [../reviewdog-pr-specs/SKILL.md](../reviewdog-pr-specs/SKILL.md)
- Speckit spec.md 심화(메인 선택): [../reviewdog-pr-specs/references/spec-review-rules.md](../reviewdog-pr-specs/references/spec-review-rules.md)
