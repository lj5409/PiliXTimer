# 启动 PWA 本地服务器，并把局域网地址打印出来（iPhone 可直接访问）
#
#   powershell -ExecutionPolicy Bypass -File tools\serve_pwa.ps1
#
param(
    [int]$Port = 8080,
    [string]$Python = "$env:USERPROFILE\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
)

$ErrorActionPreference = 'Stop'
$web = Join-Path (Split-Path -Parent $PSScriptRoot) 'web'

if (-not (Test-Path $Python)) {
    $cmd = Get-Command python -ErrorAction SilentlyContinue
    if ($cmd) { $Python = $cmd.Source }
    else { throw "找不到 Python，请用 -Python 指定路径" }
}

Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }

Write-Host "启动本地服务器: $web  ->  0.0.0.0:$Port"
Start-Process -FilePath $Python -ArgumentList @(
    '-m', 'http.server', $Port, '--bind', '0.0.0.0', '--directory', $web
) -WindowStyle Hidden
Start-Sleep -Seconds 2

$ips = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and $_.InterfaceAlias -notlike '*WSL*' -and $_.InterfaceAlias -notlike '*Default Switch*' }

Write-Host ""
foreach ($ip in $ips) {
    Write-Host ("  本机访问 : http://localhost:{0}/" -f $Port)
    Write-Host ("  手机访问 : http://{0}:{1}/" -f $ip.IPAddress, $Port) -ForegroundColor Cyan
}
Write-Host ""
Write-Host "在 iPhone Safari 打开上面的「手机访问」地址 -> 分享 -> 添加到主屏幕"
Write-Host "（局域网 http 访问时 Service Worker 不会注册，离线缓存需部署到 https 后生效）"
Write-Host "按 Ctrl+C 结束此脚本（服务器仍在后台运行，可用任务管理器结束 python.exe）"
