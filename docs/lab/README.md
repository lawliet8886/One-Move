# One Move — laboratório verificável

Base original preservada: `db79e44ca035b5b7d951b625220f025494e6b185`. Alterações em branch separada; `main` não foi substituída.

## Achado crítico

Na base recebida, `PhysicsWorld.step` consultava `solutionPinId`, interpolava personagens até o destino e forçava derrota após 1,1s. Não calculava gravidade/colisões. `HomeNestRenderer` também deslocava posições na apresentação. A nova física remove esses atalhos. Relatórios antigos que chamavam isso de simulação não validam o motor novo.

## Implementado

Dinâmica de círculos e segmentos espessos, passo fixo de 1/240s, gravidade, contatos, impulsos, portas articuladas, gangorras e molas. Resgate exige presença física, velocidade baixa e permanência na zona; todos os amigos devem chegar. `solutionPinId` é apenas metadado de autoria/teste.

Doze layouts foram reconstruídos para a física real. Os personagens, a regra de um movimento e os pinos foram preservados como conceito, não os antigos roteiros de animação. Há controles de 52dp com letras, pausa, progresso, estados finais legíveis, pivôs corrigidos e arte procedural. A espiral das molas foi ampliada atrás da placa sem deslocar sua superfície de contato.

O wrapper oficial foi restaurado e seu JAR é conferido por SHA-256 no CI. Caches `.gradle/` não pertencem ao código-fonte.

## Significado dos testes

- `PhysicsIntegrityTest`: 707 simulações e 122.991 verificações na bateria vigente. São repetições/cenários, não 707 métodos JUnit nem 122.991 bugs diferentes. Inclui seis cadências de quadros, gabarito alterado, barreira extra, trilho fino com queda rápida, contato com perigo, reset e pós-vitória.
- `VerticalSliceOutcomeMatrixTest`: 37 escolhas, dez repetições cada, total de 370 simulações adicionais. Sucesso matemático não substitui toque real.
- `RenderingIntegrityTest`: a apresentação não pode teletransportar personagens.
- `DevicePlaythroughTest`: Android real em emulador, 12 vitórias, 25 escolhas restantes, toques por coordenadas/botões, reset, progresso persistente, pausa em segundo plano e resultado fora do tabuleiro.
- `CompactUiTest`: 720x1280, densidade 320, fonte 1,15x, alvos mínimos de 48dp, toque fora do alvo, bloqueio de segundo movimento, pausa no seletor e reset.
- Monkey: 300 eventos aleatórios de toque/movimento, semente 20260922, restritos ao pacote do jogo. Não é exploração inteligente e não demonstra ausência de todos os erros.

A suíte ativa tem seis métodos JUnit de regressão e dois métodos instrumentados. Os dois antigos geradores de arte foram movidos sem alterar seus bytes para `tools/legacy/`: continham geração de conceitos, pressupostos da animação antiga e esperas sem limite. Eles não foram contados como testes aprovados.

## Evidências e integridade

O workflow salva `lab-evidence/commit.txt`, a versão real do Android, os resultados, registros, 12 vídeos nativos e capturas atuais. O APK distribuído precisa corresponder a esse hash. Vídeo é captura do aplicativo em execução, não remontagem de imagens antigas. A seleção de ações é roteirizada, não decisão visual ao vivo de uma IA.

A rodada `35680122883`, código `ae2b0876fe25c87839723b4a426ea6283e1adb1f`, passou no emulador API35 e produziu 12 vídeos verificados. A extensão para API36, o acabamento das molas e o estresse aleatório foram adicionados depois; seus resultados devem ser lidos na execução correspondente, não presumidos a partir da rodada anterior.

## Limitações

Este é um protótipo melhorado, não uma certificação de perfeição. Ainda cabem avaliação humana de diversão/dificuldade, aparelhos físicos variados, perfis de desempenho, áudio e acessibilidade mais ampla. A física é uma aproximação para este puzzle, não um simulador mecânico geral. Os níveis iniciais são simples; balanceamento não pode ser provado apenas por aprovação automática.

Não houve instalação no computador do usuário, conexão MCP local, acesso a Tripo, uso de créditos de API, publicação na Play Store ou acionamento de tarefa Codex. O workflow usa computação do GitHub e não contém um loop autônomo de IA.
