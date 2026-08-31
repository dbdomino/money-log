# .agent

이 프로젝트 전용 AI 에이전트 자산 디렉터리입니다.

## 구조

```
.agent/
├── README.md          # 이 파일
├── scripts/
│   └── link-skills.ps1    # 도구별 경로에 스킬 연결
└── skills/            # AI 스킬 보관소 (소스 오브 트루스)
    └── <skill-name>/
        ├── SKILL.md   # 필수 — 스킬 본문
        ├── reference.md   # 선택 — 상세 참고
        ├── examples.md    # 선택 — 예시
        └── scripts/       # 선택 — 유틸 스크립트
```

## 스킬 작성 규칙

- 각 스킬은 `skills/<skill-name>/SKILL.md` 하나로 시작한다.
- `name`: 소문자·숫자·하이픈만 (최대 64자)
- `description`: 무엇을 하는지 + 언제 쓰는지 (3인칭)
- `SKILL.md` 본문은 500줄 이하로 유지한다.
- 상세 문서는 `reference.md` 등으로 분리하고, `SKILL.md`에서 한 단계만 링크한다.

## AI 도구와의 연결

**소스 오브 트루스는 `.agent/skills/`** 다. 도구는 각자 정해진 경로만 탐색한다.

| 도구 | 탐색 경로 | 슬래시 명령 |
|------|-----------|-------------|
| Cursor | `.cursor/skills/<skill-name>/SKILL.md` | 디렉터리 이름 |
| Claude Code | `.claude/skills/<skill-name>/SKILL.md` | 디렉터리 이름 |

두 경로 모두 `.gitignore` 대상이라 **클론 직후에는 비어 있다.** 아래로 만든다.

```powershell
pwsh .agent/scripts/link-skills.ps1            # 둘 다
pwsh .agent/scripts/link-skills.ps1 -Target claude
```

스킬마다 **디렉터리 정션**(`mklink /J`)을 건다. 복사가 아니라 링크라서 `.agent/skills/` 를 고치면 양쪽에 바로 반영되고, 한쪽만 고쳐 어긋나는 일이 없다. 정션은 관리자 권한이 필요 없다. 정션을 쓸 수 없는 환경이면 `-Copy` 로 복사본을 만들되, 원본을 고칠 때마다 다시 실행해야 한다.

### 주의

- **`skills/` 디렉터리 전체를 하나의 링크로 두지 않는다.** Claude Code가 따라가는 것은 `<skill-name>` 디렉터리 단위 링크다.
- 링크를 지울 때는 `rmdir <경로>` (또는 `Remove-Item` )로 **링크만** 제거한다. 재귀 삭제 도구가 정션을 따라 들어가면 원본이 지워질 수 있다. `link-skills.ps1` 은 이 구분을 지켜서 지운다.
- Claude Code는 **세션 시작 시점에 없던** 최상위 `skills/` 디렉터리를 감시하지 않는다. 처음 만들었다면 재시작해야 `/speckit-*` 가 뜬다. 이미 있는 디렉터리 안에서 스킬을 추가·수정하는 것은 재시작 없이 반영된다.
- AI는 작업 시 `.agent/skills/` 를 우선 확인한다. (`.cursor/rules/agent-skills.mdc` 참고)
