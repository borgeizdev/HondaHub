# =====================================================================
#  rodar.ps1  -  UM comando: compila, instala no celular/central e abre.
#
#  Uso:   .\rodar.ps1            (versao de teste/debug - mais rapido)
#         .\rodar.ps1 -Release   (versao final otimizada, menor e mais leve)
#  Ou de dois cliques em rodar.bat
#
#  Ordem: 1) compila  2) espera o aparelho  3) instala com o adb  4) abre.
#  Cada etapa tem a sua mensagem de erro (compilar != instalar).
# =====================================================================
param([switch]$Release)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Garante JDK/SDK mesmo se o terminal foi aberto antes da instalacao
if (-not $env:JAVA_HOME)    { $env:JAVA_HOME    = "$env:LOCALAPPDATA\Android\jdk-17" }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
# O scrcpy traz OUTRO adb; versoes diferentes derrubam o servidor uma da outra
# e o aparelho "some". Assim o scrcpy usa este mesmo adb do SDK.
$env:ADB = $adb
$pacote = 'com.multimidiacivic.launcher'

# 1) Compila (nao precisa de aparelho)
$tipo = if ($Release) { 'Release' } else { 'Debug' }
Write-Host ">> Compilando ($tipo)..." -ForegroundColor Cyan
& .\gradlew.bat "assemble$tipo" --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host 'Falhou ao COMPILAR. Veja o erro acima.' -ForegroundColor Red; exit 1 }
$apk = "app\build\outputs\apk\$($tipo.ToLower())\app-$($tipo.ToLower()).apk"
Write-Host "Compilado: $apk" -ForegroundColor Green

# 2) Espera o aparelho (ate 30 s)
& $adb start-server 2>$null | Out-Null
$aparelho = $null
for ($i = 0; $i -lt 30; $i++) {
    $aparelho = & $adb devices | Select-String "`tdevice$"
    if ($aparelho) { break }
    if ($i -eq 0) { Write-Host '>> Esperando o aparelho (cabo USB + Depuracao USB)...' -ForegroundColor Yellow }
    Start-Sleep -Seconds 1
}
if (-not $aparelho) {
    Write-Host 'O app COMPILOU, mas nenhum aparelho esta conectado.' -ForegroundColor Yellow
    $lista = & $adb devices | Select-String "`t"
    if ($lista -match 'unauthorized') { Write-Host 'O aparelho apareceu como NAO AUTORIZADO: aceite "Permitir depuracao USB" na tela dele.' }
    elseif ($lista -match 'offline')  { Write-Host 'O aparelho esta OFFLINE: tire e ponha o cabo de novo.' }
    else { Write-Host 'Confira: cabo USB (de dados, nao so de carga), "Depuracao USB" ligada e o aviso "Permitir depuracao" aceito.' }
    Write-Host "Ou instale na mao depois:  `"$adb`" install -r $apk"
    exit 1
}

# 3) Instala (-r = atualiza mantendo os ajustes)
Write-Host '>> Instalando...' -ForegroundColor Cyan
$saida = (& $adb install -r $apk 2>&1 | Out-String)
Write-Host $saida.Trim()
if ($saida -notmatch 'Success') {
    if ($saida -match 'UPDATE_INCOMPATIBLE|signatures do not match') {
        Write-Host "Ja existe uma versao assinada de outro jeito. Desinstale e rode de novo:  `"$adb`" uninstall $pacote" -ForegroundColor Yellow
    } elseif ($saida -match 'INSUFFICIENT_STORAGE') {
        Write-Host 'Sem espaco no aparelho.' -ForegroundColor Yellow
    }
    Write-Host 'Falhou ao INSTALAR (o app compilou). Veja a mensagem acima.' -ForegroundColor Red
    exit 1
}

# 4) Abre o app
Write-Host '>> Abrindo o app...' -ForegroundColor Cyan
& $adb shell am start -n "$pacote/.MainActivity" | Out-Null
Write-Host 'Pronto!' -ForegroundColor Green
