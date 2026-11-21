<p align="center">
  <img src="docs/banner.png" alt="HondaHub rodando na central multimídia do Civic" width="100%">
</p>

<h3 align="center">A tela inicial que a central multimídia do Civic merecia.</h3>

<p align="center">
  Launcher Android leve, feito sob medida para o <b>Honda Civic</b>: garagem iluminada,<br>
  atalhos de apps, player de música e personalização, rodando liso até em centrais antigas.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 1.9">
  <img src="https://img.shields.io/badge/Android-4.2%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 4.2+">
  <img src="https://img.shields.io/badge/Gradle-8.5-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle AGP 8.5">
  <img src="https://img.shields.io/badge/JDK-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="JDK 17">
</p>

<p align="center">
  <a href="#stack"><b>Stack</b></a> ·
  <a href="#funcionalidades"><b>Funcionalidades</b></a> ·
  <a href="#tecnologias"><b>Tecnologias</b></a> ·
  <a href="#arquitetura--estrutura"><b>Estrutura</b></a> ·
  <a href="#como-rodar"><b>Como rodar</b></a>
</p>

<br>

## Stack

<table align="center">
  <tr>
    <td align="center" width="150"><b>Linguagem</b><br><a href="https://kotlinlang.org">Kotlin</a></td>
    <td align="center" width="150"><b>Plataforma</b><br><a href="https://developer.android.com">Android SDK</a></td>
    <td align="center" width="150"><b>Build</b><br><a href="https://developer.android.com/build">Gradle + AGP</a></td>
    <td align="center" width="150"><b>Dependências</b><br>nenhuma</td>
  </tr>
</table>

> **Zero bibliotecas externas.** Nada de AppCompat nem Jetpack: só o Android puro + Kotlin, para o app abrir rápido e gastar pouca memória em centrais multimídia simples.

## Funcionalidades

| | |
|---|---|
| **Tela inicial de carro** | relógio grande, o Civic numa garagem iluminada e 6 atalhos em vidro, tudo nas posições do modelo |
| **Atalhos inteligentes** | os 6 botões se preenchem sozinhos (Play Store primeiro, depois os apps baixados), ou você escolhe o app de cada um |
| **Gaveta de apps** | todos os apps da central, com filtros *Todos*, *Baixados* e *Do sistema*. Um toque abre o app |
| **Player de música** | tocar, pausar, avançar, voltar, mudo e barra de progresso para qualquer app de música (Spotify, YouTube Music…) |
| **14 garagens** | papéis de parede renderizados com a mesma perspectiva da foto do carro. Botões e relógio acompanham a cor |
| **Personalizar** | painel do lápis com prévia ao vivo para trocar os apps dos botões e o papel de parede |
| **Boas-vindas** | logo da Honda com reflexo de luz e *"Bem-vindo a bordo"* enquanto o app carrega |
| **Atalhos do sistema** | ajustes do Android, Wi-Fi e Bluetooth direto da tela inicial |
| **Launcher de verdade** | aparece no botão Home do Android, e o Voltar nunca sai da tela inicial |

## Tecnologias

| Parte | Como é feita |
|---|---|
| **Interface** | `View`s próprias desenhadas no [`Canvas`](https://developer.android.com/reference/android/graphics/Canvas) (botões em trapézio, ícones, player, relógio), sem XML de layout |
| **Layout** | coordenadas de um modelo de **1358 × 681** escaladas para a tela real |
| **Música** | [`MediaSessionManager`](https://developer.android.com/reference/android/media/session/MediaSessionManager) + [`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService) (Android 5+) e teclas de mídia nas versões antigas |
| **Lista de apps** | [`PackageManager`](https://developer.android.com/reference/android/content/pm/PackageManager) + [`GridView`](https://developer.android.com/reference/android/widget/GridView) reciclável, com ícones convertidos em imagem uma vez só |
| **Imagens** | [`BitmapFactory`](https://developer.android.com/reference/android/graphics/BitmapFactory) com `inSampleSize` e `RGB_565`, decodificando fora da thread da tela |
| **Preferências** | [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences): app de cada botão e garagem escolhida |
| **Fontes** | [Exo 2](https://fonts.google.com/specimen/Exo+2) nos textos e [Orbitron](https://fonts.google.com/specimen/Orbitron) no título das boas-vindas |
| **Ferramentas** | scripts Java que renderizam as garagens (ray tracing simples da sala) e geram o ícone do app |

<details>
<summary><b>Como o app fica leve numa central fraca</b></summary>
<br>

- O painel **Personalizar** e a **gaveta** são montados uma vez, quando o app fica ocioso, e depois só aparecem e somem.
- Cada toque atualiza apenas o que mudou (seleção, texto, cor). Nada é remontado.
- A grade de apps só cria as células visíveis e as reaproveita ao rolar.
- Com um painel aberto, a tela inicial por baixo deixa de ser desenhada.
- Imagens são lidas já no tamanho da tela, nunca no tamanho original.

</details>

## Arquitetura / Estrutura

```
app/src/main/java/com/multimidiacivic/launcher/
├── MainActivity.kt       monta a tela inicial e liga tudo (splash, painéis, música, apps)
├── tela/                 peças da tela inicial
│   ├── Tela.kt           posiciona as peças pelas coordenadas do modelo
│   ├── Splash.kt         boas-vindas enquanto a tela carrega
│   ├── Fundo.kt          papel de parede (garagem) escolhido
│   ├── Carro.kt          foto do carro + sombra
│   ├── Botao.kt          botões em trapézio de vidro (tingidos pela cor da garagem)
│   ├── Icone.kt          ícones desenhados no código
│   ├── Relogio.kt        relógio e data
│   └── Player.kt         caixa do player de música
├── editor/Editor.kt      painel Personalizar (lápis): apps dos botões e papel de parede
├── gaveta/Gaveta.kt      gaveta com todos os apps (botão de grade)
├── musica/Musica.kt      controle da música dos outros apps (+ OuvinteNotificacoes)
├── dados/
│   ├── Apps.kt           lista os apps instalados e distribui nos 6 botões
│   └── Temas.kt          garagens disponíveis + o que fica salvo (Ajustes)
└── util/
    ├── Pecas.kt          textos, botões, caixas e grade de apps comuns aos painéis
    ├── Icones.kt         ícones dos apps prontos como imagem
    ├── Imagens.kt        decodificação de imagens em segundo plano
    └── Fontes.kt         fontes carregadas uma vez só

app/src/main/res/
├── drawable-nodpi/       carro.png, logo.png (splash) e fundo_<cor>.jpg (garagens)
└── mipmap-*/             ícone do app em todos os tamanhos

ferramentas/
├── GerarFundos.java      renderiza as 14 garagens casadas com a perspectiva do carro
├── GerarIcone.java       gera o ícone do app a partir de logo-app.png
└── setup-ambiente.ps1    instala JDK, Android SDK e scrcpy
```

## Como rodar

### 1. Preparar o ambiente <sub>(só uma vez)</sub>

No terminal do VS Code (PowerShell), dentro da pasta do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File ferramentas\setup-ambiente.ps1
```

Instala tudo na sua pasta de usuário, **sem precisar ser administrador**: JDK 17, Android SDK (adb, android-34, build-tools 34), as variáveis `JAVA_HOME` / `ANDROID_HOME` e o [scrcpy](https://github.com/Genymobile/scrcpy) para espelhar a tela. Depois, feche e abra o VS Code. O Gradle baixa sozinho pelo `gradlew.bat`.

### 2. Preparar o aparelho

1. Configurações › Sobre › toque **7 vezes** em *Número da versão* para liberar o modo desenvolvedor.
2. Opções do desenvolvedor › ligue **Depuração USB**.
3. Conecte um cabo **de dados** e aceite *"Permitir depuração USB"* na tela.

### 3. Compilar, instalar e abrir

```powershell
.\rodar.ps1            # versão de teste
.\rodar.ps1 -Release   # versão final otimizada (bem menor)
```

Também funciona com dois cliques em `rodar.bat` ou **Ctrl + Shift + B** no VS Code. O script compila, espera o aparelho, instala e abre o app, e explica o que fazer se algo falhar.

| Atalho no VS Code (Terminal › Executar Tarefa) | Para quê |
|---|---|
| **Espelhar tela (scrcpy)** | ver e controlar a tela do aparelho no PC |
| **Ver erros do app (logcat)** | descobrir por que o app fechou |

### 4. Instalar no carro

Gere a versão final com `.\rodar.ps1 -Release` e envie o `app\build\outputs\apk\release\app-release.apk` para a central (pelo Google Drive, por exemplo). Lá:

1. Abra o APK pelo navegador e autorize a instalação de apps.
2. Aperte o botão **Home** e escolha **HondaHub › Sempre**.
3. Toque no nome da música no player e libere **Acesso a notificações**, para aparecer a música tocando.

<details>
<summary><b>Personalizar as imagens</b> (carro, garagens, logo e ícone)</summary>
<br>

| O quê | Como |
|---|---|
| **Foto do carro** | substitua `res\drawable-nodpi\carro.png` (PNG com fundo transparente) e refaça as garagens |
| **Garagens** | `java ferramentas\GerarFundos.java`; para conferir o encaixe com o carro: `-Dcor=azul ... conferir --conferir` |
| **Cor nova de garagem** | uma linha em `TEMAS` no `GerarFundos.java` e a mesma cor em `dados/Temas.kt` |
| **Logo da splash** | `res\drawable-nodpi\logo.png`; tamanho, acabamento e textos em `tela/Splash.kt` |
| **Ícone do app** | substitua `ferramentas\logo-app.png` e rode `java ferramentas\GerarIcone.java` (cores, giro e tamanho no topo do arquivo) |

Use o Java do JDK instalado, por exemplo:

```powershell
& "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarIcone.java
```

</details>

<br>

---

<p align="center">
  Feito por <b>@borgeizdev</b>
</p>
