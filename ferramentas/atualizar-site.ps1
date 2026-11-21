# =====================================================================
#  atualizar-site.ps1  -  gera a versao final e coloca no site.
#
#  Uso:  powershell -ExecutionPolicy Bypass -File ferramentas\atualizar-site.ps1
#
#  1) compila a versao final (assinada com a chave do HondaHub)
#  2) copia para site\HondaHub.apk
#  3) atualiza versao e tamanho mostrados no site\index.html
#  Depois e so publicar a pasta site (Vercel).
#  Lembre de subir o versionCode em app\build.gradle.kts a cada versao nova.
# =====================================================================
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = "$env:LOCALAPPDATA\Android\jdk-17" }

if (-not (Test-Path 'assinatura.properties')) {
    Write-Host 'Falta o assinatura.properties (chave do app). Sem ele a versao final nao e assinada.' -ForegroundColor Red
    exit 1
}

Write-Host '>> Compilando a versao final...' -ForegroundColor Cyan
& .\gradlew.bat assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host 'Falhou ao COMPILAR. Veja o erro acima.' -ForegroundColor Red; exit 1 }

$apk = 'app\build\outputs\apk\release\app-release.apk'
Copy-Item $apk 'site\HondaHub.apk' -Force

# Versao (do build.gradle.kts) e tamanho (do APK)
$gradle = [IO.File]::ReadAllText((Resolve-Path 'app\build.gradle.kts'), [Text.Encoding]::UTF8)
$versao = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$mb = (Get-Item 'site\HondaHub.apk').Length / 1MB
$tamanho = ('{0:N1} MB' -f $mb).Replace('.', ',')

$caminho = (Resolve-Path 'site\index.html').Path
$html = [IO.File]::ReadAllText($caminho, [Text.Encoding]::UTF8)
$html = [regex]::Replace($html, '<!--versao-->.*?<!--/versao-->', "<!--versao-->$versao<!--/versao-->")
$html = [regex]::Replace($html, '<!--tamanho-->.*?<!--/tamanho-->', "<!--tamanho-->$tamanho<!--/tamanho-->")
[IO.File]::WriteAllText($caminho, $html, (New-Object Text.UTF8Encoding $false))

Write-Host "Site atualizado: versao $versao, $tamanho" -ForegroundColor Green
