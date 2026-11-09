param([string[]]$Tasks = @(':app:testDebugUnitTest', ':app:assembleDebug'))
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:GRADLE_USER_HOME = Join-Path (Split-Path $PSScriptRoot) '.android-tools/gradle-cache'
$gradleExecutable = Join-Path (Split-Path $PSScriptRoot) '.android-tools/gradle/gradle-8.11.1/bin/gradle.bat'
Push-Location $PSScriptRoot
try {
    & $gradleExecutable @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
} finally { Pop-Location }
