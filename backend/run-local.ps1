param([int]$Port = 8011, [int]$DevicePort = 8000, [string]$Serial = '', [switch]$Reload)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$python = Join-Path $PSScriptRoot '.venv/Scripts/python.exe'
if (!(Test-Path -LiteralPath $python)) { throw 'Tạo môi trường .venv và cài requirements.txt trước.' }
$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe' } else {
    Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
}
if (Test-Path -LiteralPath $adb) {
    $devices = @(& $adb devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if (!$Serial -and $devices.Count -eq 1) { $Serial = $devices[0] }
    if ($Serial) {
        & $adb -s $Serial reverse "tcp:$DevicePort" "tcp:$Port"
        if ($LASTEXITCODE -ne 0) { throw 'Không chuyển tiếp được kết nối Android.' }
    } elseif ($devices.Count -gt 1) { throw 'Có nhiều thiết bị. Chạy lại với -Serial để chọn thiết bị.' }
}
$listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($listener) {
    try { $health = Invoke-RestMethod "http://127.0.0.1:$Port/health" -TimeoutSec 3 } catch { throw "Cổng $Port đã được sử dụng bởi dịch vụ khác." }
    if ($health.service -ne 'sanbong-api') { throw "Cổng $Port không phải API Sân Bóng." }
    Write-Output "API Sân Bóng đã chạy tại http://127.0.0.1:$Port; kết nối Android đã sẵn sàng."
    return
}
Push-Location $PSScriptRoot
try {
    $apiArguments = @('-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', $Port, '--no-access-log')
    if ($Reload) { $apiArguments += '--reload' }
    & $python @apiArguments
}
finally { Pop-Location }
