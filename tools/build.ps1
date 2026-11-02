# 构建 PiliX Timer 的 Release APK
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File tools\build.ps1
#
# 优先使用用户级共享 Android 环境（%USERPROFILE%\.android-dev）：
#   JDK 21 / Android SDK (platform 35+36, build-tools 35+36) / Gradle / 共享依赖缓存
# 若共享环境不存在，则回退到便携工具链 %USERPROFILE%\.dsh\tooling。

param(
    [string]$Task = "assembleRelease",
    # 显式指定工具链根目录；留空则自动探测（共享环境 -> 便携工具链 -> 系统环境变量）
    [string]$Tooling = "",
    # 放在 OneDrive / 网盘目录时，构建目录可能被同步锁冲突，可指定到本地磁盘：
    #   powershell -File tools\build.ps1 -BuildDir "$env:LOCALAPPDATA\PiliXTimer\build"
    [string]$BuildDir = ""
)

$ErrorActionPreference = 'Stop'

# --- 探测工具链根目录 ---
$roots = @()
if ($Tooling) { $roots += $Tooling }
$roots += "$env:USERPROFILE\.android-dev"
$roots += "$env:USERPROFILE\.dsh\tooling"

# --- 定位 JDK ---
$jdkDir = $null
foreach ($root in $roots) {
    $jdkDir = Get-ChildItem "$root\jdk-*" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkDir) { break }
}
if ($jdkDir) {
    $env:JAVA_HOME = $jdkDir.FullName
} elseif ($env:JAVA_HOME) {
    Write-Host "使用系统 JAVA_HOME: $env:JAVA_HOME"
} else {
    throw "找不到 JDK，请设置 JAVA_HOME 或修改 `$Tooling"
}

# --- 定位 Android SDK ---
$sdk = $null
foreach ($root in $roots) {
    if (Test-Path "$root\sdk\platform-tools") { $sdk = "$root\sdk"; break }
    if (Test-Path "$root\android-sdk\platform-tools") { $sdk = "$root\android-sdk"; break }
}
if ($sdk) {
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
} elseif ($env:ANDROID_HOME) {
    Write-Host "使用系统 ANDROID_HOME: $env:ANDROID_HOME"
} else {
    throw "找不到 Android SDK，请设置 ANDROID_HOME 或修改 `$Tooling"
}

# --- 共享 Gradle 缓存 / AVD 目录 ---
foreach ($root in $roots) {
    if (Test-Path "$root\gradle-home") {
        $env:GRADLE_USER_HOME = "$root\gradle-home"
        break
    }
}
foreach ($root in $roots) {
    if (Test-Path "$root\android-home") {
        $env:ANDROID_USER_HOME = "$root\android-home"
        $env:ANDROID_AVD_HOME = "$root\android-home\avd"
        break
    }
}

$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
Write-Host "工具链: JAVA_HOME=$env:JAVA_HOME"
Write-Host "        ANDROID_HOME=$env:ANDROID_HOME"
if ($env:GRADLE_USER_HOME) { Write-Host "        GRADLE_USER_HOME=$env:GRADLE_USER_HOME" }

$project = Split-Path -Parent $PSScriptRoot
Set-Location $project

Write-Host "== 构建 $Task =="
if ($BuildDir) {
    New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
    & "$project\gradlew.bat" $Task "-Ppilix.buildDir=$BuildDir"
    $outDir = $BuildDir
} else {
    & "$project\gradlew.bat" $Task
    $outDir = Join-Path $project "app\build"
}
if ($LASTEXITCODE -ne 0) {
    throw "构建失败，退出码 $LASTEXITCODE"
}

$apk = Join-Path $outDir "outputs\apk\release\app-release.apk"
if (Test-Path $apk) {
    $size = [math]::Round((Get-Item $apk).Length / 1MB, 2)
    Write-Host ""
    Write-Host "APK: $apk ($size MB)"
}
