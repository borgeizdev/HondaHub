# HondaHub – Launcher leve para central multimídia

## 1. Instalar o ambiente (só uma vez)
No terminal do VS Code (PowerShell), dentro da pasta do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File ferramentas\setup-ambiente.ps1
```

O que ele instala (tudo na sua pasta de usuário, sem precisar ser administrador):

| O quê | Onde |
|---|---|
| JDK 17 (Java) | `%LOCALAPPDATA%\Android\jdk-17` |
| Android SDK: sdkmanager, adb, android-34, build-tools 34 | `%LOCALAPPDATA%\Android\Sdk` |
| Variáveis `JAVA_HOME`, `ANDROID_HOME` e o `PATH` | do seu usuário |
| scrcpy (espelhar a tela) | pelo winget |
| Extensões do VS Code: Kotlin, XML, Gradle | |

Depois, **feche e abra o VS Code**.

O Gradle não precisa ser instalado: o `gradlew.bat` (Gradle Wrapper) baixa sozinho.

## 2. Preparar o celular
1. Configurações > Sobre o telefone > toque 7 vezes em **Número da versão** (libera o "Modo desenvolvedor").
2. Configurações > Sistema > Opções do desenvolvedor > ligue **Depuração USB**.
3. Ligue o cabo USB e aceite **"Permitir depuração USB"** no celular.
4. Confira no PC: `adb devices` deve mostrar o aparelho com a palavra `device`.

## 3. Rodar (um comando só)
```powershell
.\rodar.ps1            # compila, instala e abre (versão de teste)
.\rodar.ps1 -Release   # versão final otimizada (bem menor)
```
Também dá para dar dois cliques em `rodar.bat`, ou no VS Code apertar **Ctrl+Shift+B**.

Para ver a tela no PC: `scrcpy` (ou o menu Terminal > Executar Tarefa > "Espelhar tela").
Se o app fechar sozinho: Executar Tarefa > "Ver erros do app (logcat)".

## 4. Trocar a imagem do carro
Substitua `app\src\main\res\drawable-nodpi\carro.png` por um PNG com fundo
transparente (o nome precisa continuar `carro.png`). O reflexo no chão é gerado sozinho. Depois, refaça os papéis de parede (seção 6) para a garagem casar com a nova foto.

## Tela de boas-vindas
Ao abrir, aparece "Bem-vindo a bordo" enquanto o papel de parede, o carro e os apps carregam
(some sozinha: no mínimo 2 s, no máximo 5 s). Para ter o logo GRANDE no meio, coloque um PNG
com fundo transparente em `app\src\main\res\drawable-nodpi\logo.png`; sem ele aparece um
emblema simples "CIVIC". O logo ganha acabamento cromado e um reflexo de luz; para manter
as cores originais do PNG, troque `CROMADO` para `false` em `tela/Splash.kt` (lá também ficam
o tamanho `ALTURA_LOGO` e os textos).

## 5. A tela (igual ao modelo)
Tudo é posicionado com as coordenadas do modelo (1358 x 681) e esticado para o
tamanho real da tela — para mexer em posição, veja `montarTela()` no `MainActivity.kt`.

| Peça | O que faz |
|---|---|
| Relógio | hora atual (HH:mm), atualiza a cada minuto |
| 6 botões "TEXTO" | viram atalhos dos apps instalados (Play Store primeiro). Sobrando botão, fica "TEXTO" apagado |
| Player | ▶ e ⏸ controlam a música de qualquer app. Nome e barra de progresso aparecem depois de tocar no nome e liberar **Acesso a notificações** (Android 5+) |
| Engrenagem / Wi-Fi / Bluetooth | abrem os ajustes do Android (se a central deixar) |
| Lápis | abre o painel **Personalizar**: escolher o app de cada um dos 6 botões, ou trocar o papel de parede (14 cores) |
| Grade de apps | abre a **gaveta** com todos os apps da central (filtros Todos / Baixados / Do sistema). Tocar num app abre ele |

## Estrutura
```
app/src/main/java/com/multimidiacivic/launcher/
  MainActivity.kt     monta a tela e liga tudo
  tela/               as peças que aparecem na tela inicial
    Tela.kt           posiciona as peças pelas coordenadas do modelo
    Splash.kt         boas-vindas enquanto a tela carrega
    Fundo.kt          papel de parede escolhido
    Carro.kt          carro + reflexo
    Botao.kt          botões em trapézio (vidro azul)
    Icone.kt          ícones desenhados no código
    Relogio.kt        relógio
    Player.kt         caixa do player
  editor/
    Editor.kt         painel do lápis (apps dos botões / papel de parede)
  gaveta/
    Gaveta.kt         gaveta com todos os apps (botão de grade)
  musica/
    Musica.kt         controle da música dos outros apps (+ OuvinteNotificacoes)
  dados/
    Apps.kt           lista os apps instalados
    Temas.kt          lista de papéis de parede + o que fica salvo (Ajustes)
  util/
    Fontes.kt         fontes Exo 2 (assets/fontes)
    Imagens.kt        carrega imagem em segundo plano
    Icones.kt         ícones dos apps prontos como imagem (lápis e gaveta)
    Pecas.kt          textos, caixas e grade de apps comuns aos dois painéis
```

## 6. Papéis de parede
As imagens `fundo_<cor>.jpg` são geradas por `ferramentas\GerarFundos.java`:

```powershell
& "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarFundos.java
```

Para uma cor nova: acrescente uma linha em `TEMAS` (no .java) e a mesma cor em `dados/Temas.kt`.

A garagem é desenhada com a mesma câmera da foto do carro (altura 1,44 m,
horizonte rente ao teto do carro) e já traz a sombra de contato embaixo das rodas.
**Se trocar o `carro.png`, refaça os fundos**: ajuste `CAM`, `V0`, `F` e `CARRO_*`
no .java e confira o encaixe com

```powershell
& "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" -Dcor=azul ferramentas\GerarFundos.java conferir --conferir
```

Isso gera `conferir\fundo_azul.jpg` com o contorno do carro (amarelo), as rodas
(rosa) e o horizonte (verde) desenhados — as rodas rosa têm que cair embaixo dos pneus.

## 7. Ícone do app
O ícone sai de `ferramentas\logo-app.png` (símbolo com fundo transparente). Para trocar,
substitua esse arquivo e rode:

```powershell
& "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarIcone.java
```

Ele gera todos os tamanhos em `res\mipmap-*` (quadrado branco arredondado com o símbolo) e a
versão adaptável do Android 8+, que o próprio sistema recorta no formato dele.
