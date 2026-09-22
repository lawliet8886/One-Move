# One Move

Puzzle Android nativo em Kotlin e Jetpack Compose. Pip, Mochi e Blobbo precisam chegar ao refúgio com um único movimento. A campanha tem 12 fases e progresso local.

## Versão de laboratório

A branch `chatgpt/android-lab-20260922` reconstrói a física e a apresentação sobre a base preservada `db79e44ca035b5b7d951b625220f025494e6b185`. Não é uma certificação de perfeição, publicação final ou integração de modelos 3D.

- Gravidade e contatos com passo fixo; vitória depende da chegada física dos três amigos.
- Rampas, dobradiças, gangorras, molas e corpos com massas diferentes.
- Controles por toque no tabuleiro ou botões grandes com letras, resultado abaixo do jogo, reinício, seleção de fases e pausa.
- Personagens e mecanismos desenhados por código; nenhum crédito de Tripo ou chamada de IA é necessário para jogar.

## Compilar no computador

Requisitos: JDK 21, Android SDK plataforma 36 e build-tools 36.0.0. Configure `ANDROID_HOME` ou `sdk.dir` em `local.properties` (arquivo local, fora do Git).

Windows / PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest --tests com.example.GameRegressionTestSuite
```

Linux / macOS:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --tests com.example.GameRegressionTestSuite
```

O APK de testes fica em `app/build/outputs/apk/debug/app-debug.apk`. O wrapper oficial Gradle 9.3.1 está incluído. Não é preciso criar `.env`, chave Gemini ou chave OpenAI.

## Laboratório Android no GitHub

O workflow **One Move Android Lab** compila o projeto e executa testes instrumentados em emuladores API 35 e 36. Cada job verifica 12 vitórias por toques na tela, as demais escolhas de pinos, reinício, progresso, pausa e uma tela menor com fonte ampliada. Um teste adicional injeta 300 eventos aleatórios com semente fixa no próprio aplicativo.

As gravações usam `screenrecord` nativo. São **partidas automatizadas por roteiro**, não um humano e não um agente visual tomando decisões ao vivo. A escolha de pinos nos testes usa um plano conhecido; o motor de física não lê o gabarito.

O resultado válido é o da execução concluída e de seu hash. Logs, XMLs, JSONs, APKs, capturas e vídeos ficam nos artefatos do workflow por 14 dias. As imagens antigas da raiz são históricas, não evidências de uma rodada nova.

Detalhes, limitações e interpretação dos testes: `docs/lab/README.md`. Geradores antigos de arte estão preservados em `tools/legacy/`, fora dos testes ativos.
