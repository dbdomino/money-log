# .agent

이 프로젝트 전용 AI 에이전트 자산 디렉터리입니다.

## 구조

```
.agent/
├── README.md          # 이 파일
└── skills/            # AI 스킬 보관소
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

## Cursor와의 관계

- Cursor 네이티브 프로젝트 스킬 경로는 `.cursor/skills/` 이다.
- 이 저장소는 **소스 오브 트루스**를 `.agent/skills/` 로 둔다.
- AI는 작업 시 `.agent/skills/` 를 우선 확인한다. (`.cursor/rules/agent-skills.mdc` 참고)
