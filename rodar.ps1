# =====================================================================
#  rodar.ps1  -  UM comando: compila, instala no celular (adb) e abre.
#
#  Uso:   .\rodar.ps1            (versao de teste/debug - mais rapido)
#         .\rodar.ps1 -Release   (versao final otimizada, menor e mais leve)
#  Ou de dois cliques em rodar.bat
# =====================================================================
param([switch]$Release)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Garante JDK/SDK mesmo se o terminal foi aberto antes da instalacao
if (-not $env:JAVA_HOME)    { $env:JAVA_HOME    = "$env:LOCALAPPDATA\Android\jdk-17" }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$pacote = 'com.multimidiacivic.launcher'

# 1) Tem aparelho conectado?
$aparelhos = & $adb devices | Select-String "`tdevice$"
if (-not $aparelhos) {
    Write-Host 'Nenhum aparelho conectado (ou depuracao USB nao autorizada).' -ForegroundColor Yellow
    Write-Host 'Confira: cabo USB, "Depuracao USB" ligada e o aviso "Permitir depuracao" aceito no celular.'
    & $adb devices
    exit 1
}

# 2) Compila e instala
$tarefa = if ($Release) { 'installRelease' } else { 'installDebug' }
Write-Host ">> Compilando e instalando ($tarefa)..." -ForegroundColor Cyan
& .\gradlew.bat $tarefa --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host 'Falhou ao compilar. Veja o erro acima.' -ForegroundColor Red; exit 1 }

# 3) Abre o app
Write-Host '>> Abrindo o app...' -ForegroundColor Cyan
& $adb shell am start -n "$pacote/.MainActivity" | Out-Null
Write-Host 'Pronto!' -ForegroundColor Green
