# Run in Windows PowerShell with Android SDK (Android Studio) and JDK 17 installed.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$version = "8.10.2"
$toolRoot = Join-Path $PSScriptRoot ".build-tools"
$gradleBat = Join-Path $toolRoot "gradle-$version\bin\gradle.bat"
if (-not (Test-Path $gradleBat)) {
    New-Item -ItemType Directory -Force -Path $toolRoot | Out-Null
    $zip = Join-Path $toolRoot "gradle-$version-bin.zip"
    Write-Host "Downloading Gradle $version from gradle.org..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $toolRoot -Force
    Remove-Item $zip
}
& $gradleBat assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "APK: $PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
