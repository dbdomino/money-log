---
name: commitb
description: >-
  Collects the change history (staged, unstaged, untracked), summarizes it in
  2–3 Korean lines, and creates a git commit, optionally prefixing the title with
  a user-supplied tag such as a ticket id. Self-contained — does not read any
  other skill. Does not push. Use when the user asks to commit work (e.g.
  "/commitb", "/commitb [SKSIAP-1007]", "요약해서 커밋", "변경사항 커밋",
  "작업 반영 커밋").
---

# commitb — 변경이력 취합 요약 커밋

변경이력을 취합해 2~3줄로 요약하고 **커밋까지만** 한다. 푸시는 하지 않는다.

> 이 파일 하나로 완결된다. **다른 스킬·규칙 문서를 읽지 않는다.**

## 사용법

- `/commitb` 와 **같은 메시지**에 커밋 제목 앞에 붙일 **접두사**를 적는다.
  예: `/commitb [SKSIAP-1007]`
- 접두사가 필요 없으면 접두사 없이 실행한다.

## 절차

### 1. 변경이력 취합

아래를 모두 확인한 뒤에 메시지를 쓴다. 하나도 건너뛰지 않는다.

```bash
git status --porcelain=v1 -b   # 브랜치 + 변경 파일 전체(untracked 포함)
git diff --staged              # 스테이징된 변경 (있으면 이게 우선 대상)
git diff                       # 작업 트리 변경
git log -20 --format='%s'      # 이 저장소의 실제 커밋 제목 관례
```

- diff가 크면 `git diff --stat` 으로 윤곽을 먼저 잡고 핵심 파일만 본문을 읽는다.
- untracked 파일은 `git status`에 뜬 것만 대상으로 한다.
- **세션 대화 맥락**(무엇을 왜 고쳤는지)도 함께 근거로 쓴다. diff만으로는 "왜"가 안 나온다.

### 2. 범위 확인

`git diff --name-only` + untracked 목록을 이번 작업과 대조한다.

- 이번 요청과 **무관한 파일**이 섞여 있으면 커밋에 넣지 않는다. 제외한 파일과 이유를 사용자에게 보고한다.
- 넣을지 뺄지 애매한 파일이 있으면 **커밋 전에** 물어본다.

### 3. 요약 작성

**본문 요약을 2~3문장(한국어)** 으로 쓴다: 무엇을 바꿨는지, 왜인지(배경·리스크가 있으면 한 줄).

### 4. 제목 작성

- 한 줄, `type(scope): 요약` 형태를 권장한다 (`feat`, `fix`, `docs`, `test`, `refactor` 등). scope는 모듈·기능명으로 짧게.
- 단, 1단계 `git log`에서 본 **저장소 관례가 이와 다르면 관례를 따른다**. 예를 들어 최근 커밋이 전부 `[SKSIAP-1007] 이메일 템플릿 수정` 처럼 티켓 접두 + 한글 한 줄이면 그 형태로 쓴다.
- 최근 커밋들이 **제목 한 줄만** 쓰는 저장소면 본문을 억지로 붙이지 않는다. 그때 3단계 요약은 사용자 보고용으로만 출력한다.

### 5. 접두사 적용

- 사용자가 접두사를 명시했으면 **최종 커밋 제목 첫 줄 맨 앞**에 붙인다. 접두사와 나머지 제목 사이는 **공백 한 칸**.
  - 예: 접두사 `[SKSIAP-1007]` → `[SKSIAP-1007] feat(account): SNS 연동 해제 수신 처리`
- 본문에는 접두사를 반복하지 않는다(제목에만).
- 접두사를 안 적었는데 저장소 관례가 티켓 접두 형식이면, 브랜치명(`feature/SKSIAP-1007`)에서 뽑은 값을 **제안하고 확인**받는다. 확인 없이 지어내지 않는다.

### 6. 스테이징과 커밋

- 이미 스테이징된 게 있으면 **그것을 존중**한다. 사용자가 의도적으로 고른 범위다.
- 스테이징이 비어 있으면 2단계에서 추린 경로만 **명시적으로** `git add <path>...` 한다. `git add -A` / `git add .` 는 쓰지 않는다.
- 커밋:

```bash
git commit -m "$(cat <<'MSG'
<메시지>
MSG
)"
```

- 커밋 후 `git log -1 --stat` 으로 확인하고 제목·포함 파일을 보고한다.

## 커밋 메시지 형식

```
[접두사] type(scope): 한 줄 제목 (50자 전후)

- 첫째 줄: 주요 변경 요지
- 둘째 줄: 부가 변경·테스트·문서 등
- (선택) 셋째 줄: 주의사항·후속 작업
```

## 금지·주의

- 요약에 diff에 없는 내용을 지어내지 않는다.
- 비밀·토큰·자격증명·대용량 생성물(`build/`, `.gradle/`, `node_modules/` 등)을 커밋에 넣지 않는다. 발견하면 멈추고 알린다.
- 사용자가 "커밋 메시지만" 요청하면 `git commit`은 실행하지 않고 메시지만 제시한다. "스테이징만" 요청하면 커밋하지 않는다.
- `--no-verify`, `--amend`, `git reset --hard`, `git checkout --` 은 사용자가 명시적으로 요청할 때만. 훅이 실패하면 우회하지 말고 원인을 고친다.
- **푸시하지 않는다.** 사용자가 푸시까지 원하면 커밋 결과를 보고한 뒤 푸시 여부를 따로 묻는다.
- 기본 브랜치(`main`/`master`/`develop`)에 있으면 커밋 전에 브랜치를 만들지 물어본다.

## 예시 (출력만)

**제목:** `[SKSIAP-1007] test(cs-mgmt): 객실 상태이력 리포지토리 제약 검증 추가`

**본문:**
- NOT NULL·history_content 길이에 대한 통합 테스트를 추가하고 예외 체인 단언을 정리했다.
- Bean Validation과 DB 제약이 달리 동작할 수 있어 원인 체인에서 `ConstraintViolationException`/`DataIntegrityViolationException`을 모두 허용한다.
