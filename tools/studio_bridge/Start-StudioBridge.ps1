[CmdletBinding()]
param([ValidateSet('Console','Server','Status','Inspect','Bind')][string]$Mode='Console',[string]$TargetId='')
$ErrorActionPreference='Stop'
$Python=Join-Path $env:USERPROFILE 'OneMove-Lab\.tools\studio-mcp-2.2\Scripts\python.exe'
if (-not (Test-Path $Python)) { throw 'The isolated Studio MCP environment is missing. No automatic install.' }
$listener=@(Get-NetTCPConnection -LocalPort 9222 -State Listen -ErrorAction SilentlyContinue)
if (-not $listener) { throw 'Dedicated Edge is closed. Open TRIPO CONTROLADO; this launcher does not force-restart a browser.' }
if (@($listener|Where-Object {$_.LocalAddress -notin @('127.0.0.1','::1')}).Count) {
    throw 'CDP is not loopback-only; no bridge connection attempted.'
}
switch ($Mode) {
    'Console' { & $Python -u (Join-Path $PSScriptRoot 'console.py') }
    'Server' { & $Python -u (Join-Path $PSScriptRoot 'mcp_server.py') }
    'Status' { & $Python (Join-Path $PSScriptRoot 'bridge.py') status }
    'Inspect' { & $Python (Join-Path $PSScriptRoot 'bridge.py') inspect }
    'Bind' {
        if ($TargetId) { & $Python (Join-Path $PSScriptRoot 'bind_target.py') --target-id $TargetId }
        else { & $Python (Join-Path $PSScriptRoot 'bind_target.py') }
    }
}
if ($LASTEXITCODE -ne 0) { throw "Bridge exited with code $LASTEXITCODE. No automatic action retry." }
