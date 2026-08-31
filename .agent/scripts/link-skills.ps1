<#
.SYNOPSIS
  .agent/skills 의 스킬을 각 AI 도구가 인식하는 경로에 연결한다.

.DESCRIPTION
  소스 오브 트루스는 .agent/skills 다.
  도구별 탐색 경로(.claude/skills, .cursor/skills)는 .gitignore 대상이라
  클론 직후에는 비어 있다. 이 스크립트로 다시 만든다.

  Claude Code 는 <skill-name> 디렉터리 단위의 심볼릭 링크만 따라가므로
  (skills 디렉터리 전체를 링크로 두는 형태는 문서화되어 있지 않다),
  스킬마다 디렉터리 정션(mklink /J)을 만든다.
  정션은 관리자 권한 없이 만들 수 있다.

.PARAMETER Target
  claude | cursor | all (기본 all)

.PARAMETER Copy
  정션 대신 실제 복사본을 만든다. 정션을 쓸 수 없는 환경용.
  복사본은 원본과 어긋날 수 있으므로 변경 후 다시 실행해야 한다.

.EXAMPLE
  pwsh .agent/scripts/link-skills.ps1
  pwsh .agent/scripts/link-skills.ps1 -Target claude
#>
[CmdletBinding()]
param(
    [ValidateSet('claude', 'cursor', 'all')]
    [string]$Target = 'all',

    [switch]$Copy
)

$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$source = Join-Path $repoRoot '.agent\skills'

if (-not (Test-Path $source)) {
    throw "소스 스킬 디렉터리가 없습니다: $source"
}

$targets = switch ($Target) {
    'claude' { @('.claude\skills') }
    'cursor' { @('.cursor\skills') }
    'all' { @('.claude\skills', '.cursor\skills') }
}

$skills = Get-ChildItem -Directory $source | Where-Object {
    Test-Path (Join-Path $_.FullName 'SKILL.md')
}

if ($skills.Count -eq 0) {
    throw "SKILL.md 를 가진 스킬 디렉터리가 없습니다: $source"
}

foreach ($relative in $targets) {
    $destRoot = Join-Path $repoRoot $relative
    Write-Host "== $relative" -ForegroundColor Cyan

    New-Item -ItemType Directory -Force -Path $destRoot | Out-Null

    foreach ($skill in $skills) {
        $dest = Join-Path $destRoot $skill.Name

        # 기존 항목 제거: 정션은 rmdir 로 링크만 지운다(대상은 건드리지 않음).
        if (Test-Path $dest) {
            $item = Get-Item $dest -Force
            if ($item.LinkType) {
                cmd /c rmdir "`"$dest`"" | Out-Null
            }
            else {
                Remove-Item $dest -Recurse -Force
            }
        }

        if ($Copy) {
            Microsoft.PowerShell.Management\Copy-Item $skill.FullName $dest -Recurse
            Write-Host "  copy     $($skill.Name)"
        }
        else {
            cmd /c mklink /J "`"$dest`"" "`"$($skill.FullName)`"" | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "정션 생성 실패: $dest. -Copy 옵션으로 다시 시도하세요."
            }
            Write-Host "  junction $($skill.Name)"
        }
    }
}

Write-Host ""
Write-Host "완료. 스킬 $($skills.Count)개." -ForegroundColor Green
Write-Host "Claude Code 는 세션 시작 시점에 없던 최상위 skills 디렉터리를 감시하지 않는다."
Write-Host "처음 만들었다면 Claude Code 를 재시작해야 /speckit-* 가 인식된다."
