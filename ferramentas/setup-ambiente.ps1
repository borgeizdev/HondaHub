# =====================================================================
#  setup-ambiente.ps1  -  Instala TUDO que precisa para compilar o app
#  (sem Android Studio). Pode rodar de novo quantas vezes quiser: o que
#  ja estiver instalado e pulado.
#
#  Como rodar (no terminal do VS Code, PowerShell):
#     powershell -ExecutionPolicy Bypass -File ferramentas\setup-ambiente.ps1
# =====================================================================
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$base = Join-Path $env:LOCALAPPDATA 'Android'
$jdk  = Join-Path $base 'jdk-17'
$sdk  = Join-Path $base 'Sdk'
$tmp  = Join-Path $env:TEMP 'android-setup'
New-Item -ItemType Directory -Force $base, $sdk, $tmp | Out-Null

function Baixar($url, $destino) {
    Write-Host "    baixando $url"
    & curl.exe -L --fail --silent --show-error -o $destino $url
    if ($LASTEXITCODE -ne 0) { throw "Falha ao baixar $url" }
}

# ---------- 1) JDK 17 (Eclipse Temurin, versao zip, nao precisa admin) ----------
if (-not (Test-Path "$jdk\bin\java.exe")) {
    Write-Host '[1/6] Instalando JDK 17...'
    Baixar 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk' "$tmp\jdk.zip"
    Remove-Item "$tmp\jdk" -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory "$tmp\jdk" | Out-Null
    tar -xf "$tmp\jdk.zip" -C "$tmp\jdk"
    $inner = Get-ChildItem "$tmp\jdk" -Directory | Select-Object -First 1
    Move-Item $inner.FullName $jdk
} else { Write-Host '[1/6] JDK 17 ja instalado.' }

# ---------- 2) Android command-line tools (sdkmanager) ----------
$sdkmanager = "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdkmanager)) {
    Write-Host '[2/6] Instalando Android command-line tools...'
    Baixar 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' "$tmp\cmdline.zip"
    Remove-Item "$tmp\cmdline" -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory "$tmp\cmdline" | Out-Null
    tar -xf "$tmp\cmdline.zip" -C "$tmp\cmdline"
    New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
    Move-Item "$tmp\cmdline\cmdline-tools" "$sdk\cmdline-tools\latest"
} else { Write-Host '[2/6] command-line tools ja instalados.' }

# ---------- 3) Variaveis de ambiente (para o seu usuario, sem admin) ----------
Write-Host '[3/6] Configurando JAVA_HOME, ANDROID_HOME e PATH...'
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdk, 'User')
$extras = @("$jdk\bin", "$sdk\platform-tools", "$sdk\cmdline-tools\latest\bin")
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
if (-not $userPath) { $userPath = '' }
foreach ($p in $extras) {
    if (($userPath -split ';') -notcontains $p) { $userPath = ($userPath.TrimEnd(';') + ';' + $p).TrimStart(';') }
}
[Environment]::SetEnvironmentVariable('Path', $userPath, 'User')
# vale tambem para esta janela
$env:JAVA_HOME = $jdk; $env:ANDROID_HOME = $sdk
$env:Path = ($extras -join ';') + ';' + $env:Path

# ---------- 4) Licencas + plataforma 34 + build-tools + adb ----------
Write-Host '[4/6] Aceitando licencas e instalando platform-tools (adb), android-34 e build-tools...'
# Roda o sdkmanager respondendo "y" (sim) a todas as perguntas de licenca.
# (pipe comum do PowerShell 5 nao funciona com .bat, por isso o Process)
function SdkComSim([string]$argumentos) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo 'cmd.exe', "/c `"`"$sdkmanager`" $argumentos`""
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    for ($i = 0; $i -lt 60; $i++) { try { $p.StandardInput.WriteLine('y') } catch { break } }
    $p.StandardInput.Close()
    $p.WaitForExit()
}
SdkComSim '--licenses'
SdkComSim '--install platform-tools platforms;android-34 build-tools;34.0.0'
if (-not (Test-Path "$sdk\platform-tools\adb.exe") -or -not (Test-Path "$sdk\platforms\android-34")) { throw 'sdkmanager falhou: pacotes nao instalados' }

# ---------- 5) scrcpy (espelhar a tela do celular no PC) ----------
if (-not (Get-Command scrcpy -ErrorAction SilentlyContinue)) {
    Write-Host '[5/6] Instalando scrcpy via winget...'
    & winget install --id Genymobile.scrcpy -e --accept-source-agreements --accept-package-agreements --silent
} else { Write-Host '[5/6] scrcpy ja instalado.' }

# ---------- 6) Extensoes do VS Code ----------
if (Get-Command code -ErrorAction SilentlyContinue) {
    Write-Host '[6/6] Instalando extensoes do VS Code...'
    foreach ($ext in 'fwcd.kotlin', 'redhat.vscode-xml', 'vscjava.vscode-gradle') {
        & code --install-extension $ext --force | Out-Null
    }
}

# local.properties do projeto (diz ao Gradle onde esta o SDK)
$proj = Split-Path $PSScriptRoot -Parent
"sdk.dir=$($sdk -replace '\\','\\')" | Set-Content -Encoding ASCII (Join-Path $proj 'local.properties')

Write-Host ''
Write-Host 'PRONTO! Feche e abra o VS Code para ele enxergar as novas variaveis.' -ForegroundColor Green
& "$jdk\bin\java.exe" -version
