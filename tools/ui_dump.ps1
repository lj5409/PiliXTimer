# 从模拟器抓取当前界面元素（文本 + 中心坐标），用于自动化验证
param(
    [string]$Tag = 'dump',
    [string]$Device = 'emulator-5554',
    [string]$AdbDir = "$env:USERPROFILE\.dsh\tooling\android-sdk\platform-tools"
)

$env:PATH = "$AdbDir;$env:PATH"
$tmp = Join-Path $env:TEMP "ui_dump.xml"

& adb.exe -s $Device shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
& adb.exe -s $Device pull /sdcard/ui.xml $tmp 2>&1 | Out-Null
$xml = Get-Content $tmp -Raw

$rx = [regex]'(?:text|content-desc)="([^"]+)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
Write-Output "=== [$Tag] ==="
foreach ($m in $rx.Matches($xml)) {
    $g = $m.Groups
    $cx = [int](([int]$g[2].Value + [int]$g[4].Value) / 2)
    $cy = [int](([int]$g[3].Value + [int]$g[5].Value) / 2)
    "{0,-18} center=({1},{2})" -f $g[1].Value, $cx, $cy
}
