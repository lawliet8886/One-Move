[CmdletBinding()]
param(
    [ValidateSet('Preflight','Tests','Smoke')][string]$Action='Preflight',
    [string]$LabRoot=(Join-Path $env:USERPROFILE 'OneMove-Lab'),
    [string]$Blender='C:\Program Files\Blender Foundation\Blender 5.2\blender.exe'
)
$ErrorActionPreference='Stop'
$Root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if ((& git -C $Root branch --show-current).Trim() -ne 'chatgpt/tripo-asset-prep') {
    throw 'Asset preparation only: expected chatgpt/tripo-asset-prep. No branch is switched.'
}
$Cli=Join-Path $LabRoot '.tools\tripo-cli-0.5.1\node_modules\tripo-cli\dist\cli.js'
if ($Action -eq 'Preflight') {
    & node --version
    if (Test-Path $Blender) { & $Blender --version | Select-Object -First 1 }
    else { Write-Output 'Blender not found at the selected path; no installation attempted.' }
    if (-not (Test-Path $Cli)) { throw 'Pinned project-local CLI is missing; no automatic generation or installation.' }
    & python (Join-Path $PSScriptRoot 'tripo_preflight.py') --cli $Cli
    if ($LASTEXITCODE -ne 0) { throw 'The read-only diagnostic wrapper failed.' }
    Write-Output 'Read-only preflight finished. Unknown/auth-failed balance is NOT zero. Studio and API wallets are separate.'
    return
}
if ($Action -eq 'Tests') {
    & python (Join-Path $PSScriptRoot 'test_factory.py')
    if ($LASTEXITCODE -ne 0) { throw 'Offline atlas contracts failed.' }
    return
}
if (-not (Test-Path $Blender)) { throw 'Select an existing Blender executable.' }
$memory=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB
$free=(Get-PSDrive ([IO.Path]::GetPathRoot($Root).Substring(0,1))).Free/1GB
if ($memory -lt 1.5 -or $free -lt 2) {
    throw "RESOURCE_GUARD: $memory GB RAM / $free GB disk free. No program closed and no Blender started."
}
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
$Out=Join-Path $LabRoot ".local-lab\art-factory-smoke-$stamp"
& python (Join-Path $PSScriptRoot 'smoke_factory.py') --blender $Blender --out $Out
if ($LASTEXITCODE -ne 0) { throw "Offline smoke failed. Preserve evidence in $Out" }
Write-Output "Offline fixture evidence: $Out. Nothing integrated in the Android renderer."
