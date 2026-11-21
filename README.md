<p align="center">
  <img src="docs/banner.png" alt="HondaHub rodando na central multimídia do Civic" width="100%">
</p>

<p align="center">
  <a href="#stack"><b>Stack</b></a> ·
  <a href="#funcionalidades"><b>Funcionalidades</b></a> ·
  <a href="#arquitetura--estrutura"><b>Estrutura</b></a> ·
  <a href="#como-rodar"><b>Como rodar</b></a>
</p>

<br>

## Stack

<table align="center">
  <tr>
    <td align="center" width="110">
      <a href="https://kotlinlang.org"><img src="https://cdn.jsdelivr.net/gh/devicons/devicon@v2.16.0/icons/kotlin/kotlin-original.svg" width="48" height="48" alt="Kotlin"></a>
      <br><b>Kotlin</b>
    </td>
    <td align="center" width="110">
      <a href="https://developer.android.com"><img src="https://cdn.jsdelivr.net/gh/devicons/devicon@v2.16.0/icons/android/android-original.svg" width="48" height="48" alt="Android SDK"></a>
      <br><b>Android SDK</b>
    </td>
    <td align="center" width="110">
      <a href="https://gradle.org"><img src="https://cdn.jsdelivr.net/gh/devicons/devicon@v2.16.0/icons/gradle/gradle-original.svg" width="48" height="48" alt="Gradle"></a>
      <br><b>Gradle</b>
    </td>
    <td align="center" width="110">
      <a href="https://openjdk.org"><img src="https://cdn.jsdelivr.net/gh/devicons/devicon@v2.16.0/icons/java/java-original.svg" width="48" height="48" alt="Java"></a>
      <br><b>Java 17</b>
    </td>
    <td align="center" width="110">
      <a href="https://learn.microsoft.com/powershell"><img src="https://cdn.jsdelivr.net/gh/devicons/devicon@v2.16.0/icons/powershell/powershell-original.svg" width="48" height="48" alt="PowerShell"></a>
      <br><b>PowerShell</b>
    </td>
  </tr>
</table>

<p align="center">
  Android puro + Kotlin, <b>sem nenhuma biblioteca externa</b>, para rodar leve até em centrais antigas (Android 4.2+).
</p>

## Funcionalidades

<details>
<summary><b>Ver funcionalidades</b></summary>
<br>

| | |
|---|---|
| **Tela inicial de carro** | relógio, o Civic numa garagem iluminada e 6 atalhos em vidro |
| **Atalhos inteligentes** | os 6 botões se preenchem sozinhos ou você escolhe o app de cada um |
| **Gaveta de apps** | todos os apps da central, com filtros *Todos*, *Baixados* e *Do sistema* |
| **Player de música** | controla qualquer app de música (Spotify, YouTube Music…) com nome e progresso |
| **14 garagens** | papéis de parede na perspectiva do carro; botões e relógio acompanham a cor |
| **Personalizar** | painel do lápis com prévia ao vivo dos botões e do papel de parede |
| **Boas-vindas** | logo da Honda com reflexo de luz enquanto o app carrega |
| **Launcher de verdade** | vira a tela inicial no botão Home; o Voltar nunca sai dela |

</details>

## Arquitetura / Estrutura

<details>
<summary><b>Ver estrutura de pastas</b></summary>
<br>

```
app/src/main/java/com/multimidiacivic/launcher/
├── MainActivity.kt       monta a tela inicial e liga tudo
├── tela/                 peças da tela inicial (Tela, Splash, Fundo, Carro, Botao, Icone, Relogio, Player)
├── editor/Editor.kt      painel Personalizar (lápis)
├── gaveta/Gaveta.kt      gaveta com todos os apps
├── musica/Musica.kt      controle da música dos outros apps
├── dados/                apps instalados (Apps) e garagens + preferências (Temas)
└── util/                 peças comuns dos painéis, ícones, imagens e fontes

app/src/main/res/
├── drawable-nodpi/       carro, logo da splash e as 14 garagens
└── mipmap-*/             ícone do app

ferramentas/
├── GerarFundos.java      renderiza as garagens na perspectiva do carro
├── GerarIcone.java       gera o ícone do app
└── setup-ambiente.ps1    instala JDK, Android SDK e scrcpy
```

</details>

## Como rodar

<details>
<summary><b>Ver passo a passo</b></summary>
<br>

**1. Ambiente** (só uma vez), no PowerShell dentro do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File ferramentas\setup-ambiente.ps1
```

**2. Aparelho:** ligue a **Depuração USB** nas opções do desenvolvedor e conecte um cabo de dados.

**3. Rodar:**

```powershell
.\rodar.ps1            # compila, instala e abre (teste)
.\rodar.ps1 -Release   # versão final otimizada
```

**4. No carro:** instale o `app-release.apk` pelo navegador, aperte **Home** e escolha **HondaHub › Sempre**.

</details>

<br>

---

<p align="center">
  Feito por <b>@borgeizdev</b>
</p>
