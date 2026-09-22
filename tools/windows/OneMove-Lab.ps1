# Windows lab preparation. No global settings, downloads, force reset or main writes.
[CmdletBinding()]
param(
    [ValidateSet('Doctor','Build','Test','PrepareDevice','StartDevice')]
    [string]$Action = 'Doctor',
    [string]$SdkPath = (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
    [string]$JavaHome = ''
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$Branch = (& git -C $Root branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or $Branch -ne 'chatgpt/android-lab') {
    throw 'This tool only runs in chatgpt/android-lab. No checkout performed.'
}
if (-not $JavaHome) {
    $JavaHome = Split-Path (Split-Path (Get-Command java -ErrorAction Stop).Source)
}
$SdkPath = [IO.Path]::GetFullPath($SdkPath)
$Local = Join-Path $Root '.local-lab'
$AvdName = 'OneMove_Lab_API34'
$AvdRoot = Join-Path $Local 'avd'
$Adb = Join-Path $SdkPath 'platform-tools\adb.exe'
$Emulator = Join-Path $SdkPath 'emulator\emulator.exe'
$AvdManager = Join-Path $SdkPath 'cmdline-tools\latest\bin\avdmanager.bat'
$Image = Join-Path $SdkPath 'system-images\android-34\google_apis\x86_64\system.img'
function Get-Capacity {
    $os = Get-CimInstance Win32_OperatingSystem
    $disk = Get-PSDrive ([IO.Path]::GetPathRoot($Root).Substring(0,1))
    [pscustomobject]@{ FreeRAM_GB=[math]::Round($os.FreePhysicalMemory/1MB,2);
        TotalRAM_GB=[math]::Round($os.TotalVisibleMemorySize/1MB,2);
        FreeDisk_GB=[math]::Round($disk.Free/1GB,2) }
}
function Require-Capacity([double]$MinimumRam,[double]$MinimumDisk) {
    $c = Get-Capacity
    if ($c.FreeRAM_GB -lt $MinimumRam -or $c.FreeDisk_GB -lt $MinimumDisk) {
        throw "RESOURCE_GUARD: need $MinimumRam GB free RAM and $MinimumDisk GB free disk; available $($c.FreeRAM_GB) GB RAM / $($c.FreeDisk_GB) GB disk. No apps closed or heavy task started."
    }
}
function Require-File([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing: $Path. No automatic installation." }
}
foreach ($file in @($Adb,$Emulator,$AvdManager,(Join-Path $JavaHome 'bin\java.exe'))) { Require-File $file }
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $SdkPath
$env:ANDROID_SDK_ROOT = $SdkPath
$env:ANDROID_USER_HOME = Join-Path $Local 'android-user'
$env:ANDROID_AVD_HOME = $AvdRoot
# Environment changes affect this process and children only, not Windows settings.
$env:PATH = "$JavaHome\bin;$SdkPath\platform-tools;$SdkPath\emulator;$env:PATH"
$Gradlew = Join-Path $Root 'gradlew.bat'
$Head = (& git -C $Root rev-parse HEAD).Trim()
if ($Action -eq 'Doctor') {
    [pscustomobject]@{ Project=$Root; Branch=$Branch; Commit=$Head; Capacity=(Get-Capacity);
        Java=$JavaHome; SDK=$SdkPath; WrapperPresent=(Test-Path $Gradlew);
        API34ImagePresent=(Test-Path $Image);
        DedicatedDevicePresent=(Test-Path (Join-Path $AvdRoot "$AvdName.ini"));
        ADB=$Adb; Emulator=$Emulator; Note='Diagnostic only; no emulator started.' } | ConvertTo-Json -Depth 4
    return
}
New-Item -ItemType Directory -Path $Local -Force | Out-Null
if ($Action -in @('Build','Test')) {
    # Conservative local guards, not official Android minimum requirements.
    $minimum = if ($Action -eq 'Test') { 5.0 } else { 3.0 }
    Require-Capacity $minimum 6.0
    Require-File $Gradlew
    $arguments = @('--no-daemon','--max-workers','2','--console','plain',
        '--project-cache-dir',(Join-Path $Local 'gradle-project-cache'),
        '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8')
    if ($Action -eq 'Build') { $arguments += ':app:assembleDebug' }
    else { $arguments += @(':app:testDebugUnitTest','--tests','com.example.PhysicsContractTest',
        '--tests','com.example.PinHitTesterTest','--tests','com.example.VerticalSliceOutcomeMatrixTest') }
    Push-Location $Root
    try { & $Gradlew @arguments; if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" } }
    finally { Pop-Location }
    return
}
Require-File $Image
if ($Action -eq 'PrepareDevice') {
    Require-Capacity 0.5 8.0
    $devicePath = Join-Path $AvdRoot "$AvdName.avd"
    $indexPath = Join-Path $AvdRoot "$AvdName.ini"
    if ((Test-Path $devicePath) -or (Test-Path $indexPath)) {
        if ((Test-Path $indexPath) -and (Test-Path (Join-Path $devicePath 'config.ini'))) {
            Write-Output 'Dedicated device already present; not overwritten.'; return
        }
        throw 'Partial existing AVD; inspect it rather than overwriting.'
    }
    New-Item -ItemType Directory -Path $AvdRoot,$env:ANDROID_USER_HOME -Force | Out-Null
    'no' | & $AvdManager create avd --name $AvdName --package 'system-images;android-34;google_apis;x86_64' --device pixel_5 --path $devicePath
    if ($LASTEXITCODE -ne 0) { throw 'AVD creation failed; no device was started.' }
    Require-File $indexPath
    Write-Output "Dedicated AVD prepared at $devicePath. Other AVDs are unchanged."
    return
}
if ($Action -eq 'StartDevice') {
    Require-Capacity 4.0 6.0
    Require-File (Join-Path $AvdRoot "$AvdName.ini")
    if (Get-NetTCPConnection -LocalPort 5580,5581 -State Listen -ErrorAction SilentlyContinue) {
        throw 'Lab emulator ports are occupied. Existing processes were not stopped.'
    }
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $stdout = Join-Path $Local "emulator-$stamp.stdout.log"
    $stderr = Join-Path $Local "emulator-$stamp.stderr.log"
    $options = @('-avd',$AvdName,'-port','5580','-memory','1536','-cores','2',
        '-no-snapshot','-no-boot-anim','-noaudio','-no-metrics',
        '-camera-back','none','-camera-front','none','-gpu','auto')
    $process = Start-Process -FilePath $Emulator -ArgumentList $options -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    [pscustomobject]@{ PID=$process.Id; Serial='emulator-5580'; AVD=$AvdName;
        Log=$stdout; ErrorLog=$stderr; Commit=$Head;
        Note='Launch requested only; verify boot and screenshots before claiming gameplay.' } | ConvertTo-Json
}
