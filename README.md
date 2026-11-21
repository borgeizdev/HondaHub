<p align="center">
  <img src="docs/banner.png" alt="HondaHub rodando na central multimídia do Civic" width="100%">
</p>

<p align="center">
  <a href="#stack"><b>Stack</b></a> ·
  <a href="#funcionalidades"><b>Funcionalidades</b></a> ·
  <a href="#arquitetura--estrutura"><b>Estrutura</b></a> ·
  <a href="#baixar"><b>Baixar</b></a>
</p>

<br>

<a id="stack"></a>
<h2 align="center">Stack</h2>

<p align="center">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=kotlin,androidstudio,gradle,java" alt="Kotlin, Android, Gradle e Java" height="56">
  </a>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><b>Kotlin</b></a> ·
  <a href="https://developer.android.com"><b>Android SDK</b></a> ·
  <a href="https://gradle.org"><b>Gradle</b></a> ·
  <a href="https://openjdk.org"><b>Java 17</b></a>
</p>

<p align="center">
  Android puro + Kotlin, <b>sem nenhuma biblioteca externa</b>, para rodar leve até em centrais antigas (Android 4.2+).
</p>

<a id="funcionalidades"></a>
<h2 align="center">Funcionalidades</h2>

<div align="center">

| | |
|---|---|
| **Tela inicial de carro** | relógio com data, o Civic numa garagem iluminada e 6 atalhos de apps em vidro, tudo nas posições do modelo |
| **Atalhos inteligentes** | os 6 botões se preenchem sozinhos (Play Store primeiro, depois os apps baixados), ou você escolhe o app de cada um |
| **Gaveta de apps** | o botão de grade abre todos os apps da central, com filtros *Todos*, *Baixados* e *Do sistema* |
| **Player de música** | tocar, pausar, avançar, voltar, mudo e barra de progresso para qualquer app de música (Spotify, YouTube Music…) |
| **14 garagens** | papéis de parede renderizados na mesma perspectiva da foto do carro; botões e relógio acompanham a cor escolhida |
| **Personalizar** | o lápis abre um painel com prévia ao vivo para trocar os apps dos botões e o papel de parede |
| **Boas-vindas** | logo da Honda com reflexo de luz e *"Bem-vindo a bordo"* enquanto o app carrega |
| **Atalhos do sistema** | ajustes do Android, Wi-Fi e Bluetooth direto da tela inicial |
| **Launcher de verdade** | vira a tela inicial no botão Home do Android, e o Voltar nunca sai dela |
| **Leve** | painéis montados uma vez só, grade de apps reciclável e imagens lidas no tamanho da tela, para não travar em central fraca |

</div>

## Arquitetura / Estrutura

<details>
<summary><b>Ver estrutura de pastas</b></summary>
<br>

```
app/src/main/java/com/multimidiacivic/launcher/
├── MainActivity.kt       monta a tela inicial e liga tudo (splash, painéis, música, apps)
├── tela/                 peças da tela inicial
│   ├── Tela.kt           posiciona as peças pelas coordenadas do modelo (1358 × 681)
│   ├── Splash.kt         boas-vindas enquanto a tela carrega
│   ├── Fundo.kt          papel de parede (garagem) escolhido
│   ├── Carro.kt          foto do carro + sombra
│   ├── Botao.kt          botões em trapézio de vidro, tingidos pela cor da garagem
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
    └── Fontes.kt         fontes Exo 2 e Orbitron, carregadas uma vez só

app/src/main/res/
├── drawable-nodpi/       carro.png, logo.png (splash) e fundo_<cor>.jpg (garagens)
└── mipmap-*/             ícone do app em todos os tamanhos

ferramentas/
├── GerarFundos.java      renderiza as 14 garagens casadas com a perspectiva do carro
├── GerarIcone.java       gera o ícone do app a partir de logo-app.png
└── setup-ambiente.ps1    instala JDK, Android SDK e scrcpy
```

</details>

<a id="baixar"></a>
<h2 align="center">Baixar</h2>

<p align="center">
  <a href="https://hondahub-site.vercel.app/">
    <img src="https://img.shields.io/badge/Baixar%20HondaHub-D7111E?style=for-the-badge&logo=android&logoColor=white" alt="Baixar HondaHub" height="44">
  </a>
</p>

<p align="center">
  Abra <a href="https://hondahub-site.vercel.app/"><b>hondahub-site.vercel.app</b></a> no navegador da central do carro ou de um celular Android e toque em <b>Baixar HondaHub</b>.<br>
  Depois de instalar, aperte o botão <b>Home</b> e escolha <b>HondaHub › Sempre</b>.
</p>

<br>

---

<p align="center">
  Feito por <b>@borgeizdev</b>
</p>
