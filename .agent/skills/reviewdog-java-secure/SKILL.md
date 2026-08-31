---
name: reviewdog-java
description: >
  Java 구현 diff 로컬 브랜치 리뷰(reviewdog-java). 사용자가 지정한 로컬 베이스 브랜치와 현재 HEAD를
  three-dot diff로 비교한다(베이스 브랜치 입력 필수). 메인 에이전트가 Task 서브를 띄워
  references/guideline-java.md·references/cwe-mapping.md·references/quick-reference.md와
  저장소 루트 CLAUDE.md를 근거로 diff 리뷰(## PR 리뷰 결과)를 받고, 필수·권장을 파싱·적용한다.
  미적용 필수가 남으면 diff를 갱신해 서브를 재호출한다(서브 호출 최대 3회).
  "reviewdog-java", "로컬 브랜치 대비 Java 리뷰", "Java diff 리뷰" 요청 시 사용한다.
---

# reviewdog-java — Java 구현 diff (로컬 브랜치 비교)

**본 스킬**: **`*.java` 등 구현 변경**을 기본 대상으로 하며, 서브 에이전트는 Java 리뷰 절차·출력 형식으로 **[references/guideline-java.md](references/guideline-java.md)** (`review-java`의 `java-template.md` 사본), 보안 참조로 **[references/cwe-mapping.md](references/cwe-mapping.md)**·**[references/quick-reference.md](references/quick-reference.md)**, 그리고 저장소 루트 **`CLAUDE.md`** 를 읽고 `## PR 리뷰 결과` 형식으로 리뷰한다.

**목표**: 사용자가 **반드시 지정한 로컬 베이스 브랜치**와 **현재 작업 브랜치(HEAD)** 사이의 변경을 `git` three-dot diff로 확보하고, 서브가 위 참조를 근거로 리뷰한다. 메인이 **`### 필수 수정` / `### 권장 수정`** 을 파싱·적용한 뒤, **미적용 필수**가 남으면 diff를 다시 모아 **서브를 재호출**한다. **서브 에이전트 호출은 최대 3회**다.

## 전제 (NON-NEGOTIABLE)

1. **로컬 베이스 브랜치 이름**이 사용자 메시지에 없으면 진행하지 않고, 비교 기준이 될 **로컬 브랜치명**을 요청한 뒤 **중단**한다. (예: `release/R1.0.0`, `main`, `develop` — 원격만 있으면 `git fetch` 후 tracking 브랜치 생성 또는 사용자에게 로컬 확보를 안내한다.)
2. **서브 에이전트**는 다음을 **반드시 Read** 한 뒤 리뷰한다.
   - **`reviewdog-java/references/guideline-java.md`** — 절차·**출력 마크다운 골격**(`## PR 리뷰 결과` 이하).
   - **`reviewdog-java/references/cwe-mapping.md`** — CWE·심각도 등 **보안 매핑**.
   - **`reviewdog-java/references/quick-reference.md`** — 취약 패턴 vs 안전 패턴 **가이드**.
   - 저장소 루트 **`CLAUDE.md`** — 프로젝트 **팀 계약**(아키텍처·API·권한·예외·페이징·시드 등).
   `guideline-specs.md`, `markdown-specdoc.md` 는 **사용하지 않는다.**
3. 서브는 **`## PR 리뷰 결과` … 블록만** 반환하고 **즉시 종료**한다(저장소 수정·`git`·MCP·추가 질문 없음). **예외**: 위 참조 파일과 **`CLAUDE.md` 읽기**만 허용된다.
4. 메인은 Cursor **`Task`** 도구로 서브를 띄운다. **`readonly: true`** 로 두어 서브가 워크스페이스를 바꾸지 않게 한다.

## 역할 분리

| 역할 | 하는 일 |
|------|---------|
| **메인 에이전트** | 베이스 브랜치 확정 → `git`으로 three-dot diff 수집(기본 권장: Java 한정 시 `-- '*.java'` 또는 사용자 지정 `path…`) → **Task(서브)** 프롬프트 조립·호출 → 서브 출력 수신 → `### 필수 수정` / `### 권장 수정` 파싱 → 워크스페이스에 `before`/`after` 적용 → **미적용 필수** 여부 판단 → (조건 충족 시) diff 갱신 및 서브 재호출 → 최종 요약 |
| **서브 에이전트** | **guideline-java.md** + **cwe-mapping.md** + **quick-reference.md** + **`CLAUDE.md` Read** 후, 메인이 넘긴 **비교 맥락 + 통합 diff**(및 회차 2 이상이면 **로컬 미커밋 diff** 블록)를 근거로 리뷰 마크다운 **한 블록**만 생성·반환 |

## 참조 (메인·서브)

| 파일 | 용도 |
|------|------|
| [references/guideline-java.md](references/guideline-java.md) | 서브: **절차·출력 형식** (`review-java`의 `java-template.md`와 동일 내용) |
| [references/cwe-mapping.md](references/cwe-mapping.md) | 서브: **보안 CWE 매핑** |
| [references/quick-reference.md](references/quick-reference.md) | 서브: **시큐어코딩 퀵 레퍼런스**(수정 제안·코드 예시 근거) |
| 저장소 루트 `CLAUDE.md` | 서브: 프로젝트 계약 |

서브 프롬프트에는 위 네 경로를 **Read 하라고 명시**한다. **`guideline-specs.md` / `markdown-specdoc.md`** 는 서브 규범에 넣지 않는다.

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

경로를 사용자가 지정하지 않았을 때 **Java 구현 중심 리뷰**가 목적이면 아래처럼 **`*.java`** 로 한정하는 것을 **기본으로 권장**한다.

```bash
git diff --no-color <BASE>...HEAD -- '*.java'
git diff --stat <BASE>...HEAD -- '*.java'
```

다른 경로·모듈만 보고 싶으면 사용자 지정 `path…` 로 대체한다.

#### 4) 회차 2·3 — 워킹 트리 반영분 병합

직전 회차에서 워크스페이스를 수정했다면, 서브 입력 패킷에 아래를 **덧붙인다**(회차 1과 동일한 path 필터를 유지한다).

```bash
git diff --no-color HEAD -- '*.java'
```

(필터를 쓰지 않는 경우는 `git diff --no-color HEAD --` 만 사용.)

서브에게 “**첫 블록**은 `<BASE>...HEAD` 커밋 대비 변경, **추가 블록**은 로컬 미커밋 변경”임을 한 줄로 알린다. 변경이 없으면 생략 가능.

#### 5) 경로 한정

사용자가 경로를 지정했다면 §3·§4 명령 끝에 `-- path…` 를 붙인다.

---

### B. 서브 에이전트 프롬프트 (메인이 조립)

다음을 **한 번에** 서브에게 넘긴다.

1. 지시: “당신은 **읽기 전용** Java/diff 리뷰어다. **`git`·MCP를 호출하지 말 것.** 저장소 **수정·쓰기는 금지**한다. **예외**: 워크스페이스에서 아래 파일만 **Read** 한다.”
2. **반드시 Read 할 파일**(절대 경로는 `<workspace>` 를 저장소 루트로 치환):
   - `<workspace>/.agents/skills/reviewdog-java/references/guideline-java.md`
   - `<workspace>/.agents/skills/reviewdog-java/references/cwe-mapping.md`
   - `<workspace>/.agents/skills/reviewdog-java/references/quick-reference.md`
   - `<workspace>/CLAUDE.md`
3. **준수할 출력**: `guideline-java.md` 의 「출력 형식」·「규칙」. **`### 시큐어코딩 점검 결과`** 는 `cwe-mapping.md`·`quick-reference.md` 를 첨부된 가이드로 간주하여 **생략하지 않는다**(변경과 무관한 CWE 나열은 하지 말고, diff 관련 항목 위주·**guideline-java.md** 의 최소 검토 개수 규칙을 따른다).
4. **맥락**: 현재 브랜치명(`git branch --show-current`), 베이스 `<BASE>`, 비교식 `<BASE>...HEAD`, 짧은 SHA(선택), **`외부 회차: cycle/3`**.
5. **입력 데이터**: (A)에서 만든 diff 문자열(들). 전체 diff를 **리뷰 결과에 그대로 붙이지 말 것**.
6. **출력 형식**: `## PR 리뷰 결과` 로 시작하는 **단일 마크다운 블록만**. `guideline-java.md` 에 정의된 하위 섹션을 갖춘다. `### 필수 수정`·`### 권장 수정` 은 이슈 없으면 `(해당 없음)` 등으로 **비워두지 않는다**.

**Cursor에서의 “새 에이전트”**: **`Task`** 도구, `readonly: true`, `subagent_type`: `generalPurpose`(또는 탐색이 거의 없으면 적절한 유형).

---

### C. 메인: 파싱·적용

서브 응답에서 `## PR 리뷰 결과` … 블록을 추출한다.

1. **`### 필수 수정`**: `guideline-java.md` bullet 구조로 **실질 필수**(적용 가능한 조치가 있는 항목)를 식별한다.
   - **실질 필수 0건**(`(해당 없음)`만 등): 사용자가 “필수만”이 아니라면 **`### 권장 수정`** 만 적용 시도 후 **§D로 이동**.
2. **실질 필수 ≥ 1건**: **필수 전부**에 대해 `before`/`after` 기준으로 적용을 시도한다. (**같은 `cycle` 안에서는 서브를 다시 부르지 않는다.**)
3. **권장**: 사용자가 “필수만”을 요청하지 않았으면, 필수 시도 후 같은 회차에서 권장을 적용한다.

**파싱 규칙**:

- 위치: `[파일:라인]` 또는 `` `path/to/File.java:42` ``.
- 중첩 bullet: `before:` / `after:` 로 치환.
- `before`/`after` 없이 설명만 있으면 **추론 적용** 가능 시 수정하고 항목에 “추론 적용”으로 표시.

**파일 수정 시**:

- 라인 번호는 힌트-only. **`before` 텍스트**와 앞뒤 문맥으로 고유 위치를 찾는다.
- `before`가 **정확히 한 번**만 나오면 `after`로 교체.
- 여러 번 나오면 사용자에게 짧게 확인하거나 첫 매칭만 적용·나머지는 **미완료 필수**로 다음 회차에 넘긴다.
- 찾을 수 없으면 **미적용 필수**로 남긴다.

---

### D. 메인: 다음 회차 여부 (필수가 “없어질 때까지” + 상한)

- **미적용 필수**가 **1건이라도** 남았고 **`cycle < 3`** 이면 `cycle ← cycle + 1` 하고 **§A로 돌아간다**(새 서브 호출).
- **미적용 필수 0건**이면 **루프 종료**(추가 서브 호출 없음).
- **`cycle == 3`** 까지 진행했는데도 미적용 필수가 남으면 **중단**하고 최종 요약에 **“서브 3회 상한 도달”**을 명시한다.

---

### E. 헤더·섹션 검증 (메인, 서브 출력 직후)

서브가 `guideline-java.md` 출력 골격을 어기면 같은 회차에서 한 번 고치라고 요청할 수 있다. 그래도 실패하면 파싱 가능한 범위만 적용하고 미적용을 §D로 넘긴다.

---

## 최종 사용자 출력 (루프 종료 후 한 번)

```markdown
## reviewdog-java 적용 요약
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
- **한 회차 안에서** 서브를 두 번 호출하지 않는다.
- 서브 규범으로 **`guideline-specs.md`** 나 **`markdown-specdoc.md`** 를 쓰지 않는다.

## 주의

- 커밋 범위(`<BASE>...HEAD`)와 **적용 후 워킹 트리**가 섞이므로, 종료 후 사용자에게 **커밋 여부**를 안내한다.
- 본 스킬은 **로컬 베이스 브랜치가 있을 때만** 적용한다. PR URL만 있고 베이스 브랜치가 없으면 진행하지 않고, 사용자에게 비교 기준(로컬 브랜치 확보 등)을 안내한다.
