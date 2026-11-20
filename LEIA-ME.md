# Multimidia Civic – Launcher leve para central multimídia

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
transparente (o nome precisa continuar `carro.png`). O reflexo no chão é gerado sozinho.

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
| Grade de apps | só visual por enquanto ("Em breve") |

## Estrutura
```
app/src/main/java/com/multimidiacivic/launcher/
  MainActivity.kt     monta a tela e liga tudo
  tela/               as peças que aparecem na tela inicial
    Tela.kt           posiciona as peças pelas coordenadas do modelo
    Fundo.kt          papel de parede escolhido
    Carro.kt          carro + reflexo
    Botao.kt          botões em trapézio (vidro azul)
    Icone.kt          ícones desenhados no código
    Relogio.kt        relógio
    Player.kt         caixa do player
  editor/
    Editor.kt         painel do lápis (apps dos botões / papel de parede)
  musica/
    Musica.kt         controle da música dos outros apps (+ OuvinteNotificacoes)
  dados/
    Apps.kt           lista os apps instalados
    Temas.kt          lista de papéis de parede + o que fica salvo (Ajustes)
  util/
    Fontes.kt         fontes Exo 2 (assets/fontes)
    Imagens.kt        carrega imagem em segundo plano
```

## 6. Papéis de parede
As imagens `fundo_<cor>.jpg` são geradas por `ferramentas\GerarFundos.java`:

```powershell
& "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarFundos.java
```

Para uma cor nova: acrescente uma linha em `TEMAS` (no .java) e a mesma cor em `dados/Temas.kt`.
