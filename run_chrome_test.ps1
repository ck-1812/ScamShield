$tempDir = Join-Path $env:TEMP "chrome_test_profile_$(Get-Random)"
$chromePath = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$url = "http://localhost:5173/"
$outPath = "c:\Users\Ck Sri Hari\.gemini\antigravity-ide\brain\09dadf62-f6b0-44d9-82a6-21bc959a8070\scratch\dashboard_dom.html"

Start-Process -FilePath $chromePath -ArgumentList "--headless=new", "--user-data-dir=`"$tempDir`"", "--virtual-time-budget=5000", "--dump-dom", "`"$url`"" -RedirectStandardOutput $outPath -Wait -NoNewWindow

$html = Get-Content $outPath -Raw
Write-Output "Has STT Engine: $($html.Contains('TRANSCRIPTION ENGINE'))"
Write-Output "Has Local Whisper Card: $($html.Contains('LOCAL WHISPER'))"
Write-Output "Has Whisper Button: $($html.Contains('Initialize Local Whisper'))"
Write-Output "Has Audio Path: $($html.Contains('AUDIO PATH VISUALIZATION'))"
Write-Output "Has Evidence Inspector: $($html.Contains('LATEST EVIDENCE INSPECTOR'))"
