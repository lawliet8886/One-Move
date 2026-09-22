# One Move — laboratório verificável

Base preservada: `db79e44ca035b5b7d951b625220f025494e6b185`. Trabalho em branch separada; não é uma certificação de perfeição.

## Achado crítico da base

`PhysicsWorld.step` consultava `solutionPinId`, interpolava personagens até o destino e forçava a derrota em 1,1s. Não calculava gravidade/colisões. Portanto, relatórios antigos de "simulação física" não comprovavam física. `HomeNestRenderer` também interpolava posições na apresentação. Ambos os atalhos foram removidos.

## Alterações

- Dinâmica de círculos e segmentos espessos com passo fixo de 1/240s, gravidade, contatos, impulsos, limites e mola com intervalo entre impulsos.
- Portas com vínculo explícito à trava, gangorras, pedras e pesos; mesma geometria de porta para desenho e colisão.
- Vitória depende da chegada física de todos os amigos. `solutionPinId` é apenas metadado do catálogo/teste.
- Doze layouts físicos distintos, preservando os personagens e a regra de um movimento. Isso ainda é uma campanha de protótipo; dificuldade, diversão e acessibilidade exigem avaliação adicional por pessoas.
- Relógio continua após o fim para animar e finalizar partículas. Retorno do segundo plano e seletor de fases pausam simulação.
- Botões de pinos de 52dp, letras além das cores, resultados abaixo do tabuleiro, conclusão da campanha e persistência.
- Pivôs de desenho corrigidos em gangorra, pesos, pedra e confete. Corpos dos personagens alinhados ao raio físico, iluminação por gradiente.

## O que cada teste significa

`PhysicsIntegrityTest`: executa o motor real com seis cadências de quadros, repetições, gabarito alterado, bloqueio adicional, trilho fino, contato com perigo, reset e fim da animação.

`VerticalSliceOutcomeMatrixTest`: conserva a matriz das escolhas e dez repetições por escolha. Não substitui testes do Android.

`RenderingIntegrityTest`: impede que a apresentação teleporte um personagem que não chegou.

`DevicePlaythroughTest`: abre o APK real num Android; injeta toques em coordenadas da tela; verifica 12 vitórias, demais pinos, reinício, persistência e pausa. Grava `screenrecord` por fase. É um roteiro automatizado de interação real, **não** uma pessoa nem um agente visual tomando decisões ao vivo.

Dois geradores antigos de arte (`FidelityVisualProofExportTest` e `ConceptArtAuditionGeneratorTest`) não são executados pela suíte `GameRegressionTestSuite`. O primeiro contém esperas sem limite e pressupostos de encaixe roteirizado incompatíveis com física real; o segundo gera conceitos, não valida partidas. Seu código foi preservado sem alterações para auditoria. Eles não contam como testes aprovados. A seleção é explícita no comando do workflow, não uma alegação de que todos os testes históricos passaram.

## Executar e verificar

O workflow instala Java 21, SDK Android e Gradle 9.3.1; regenera o wrapper oficial, compila ambos APKs, roda testes e um emulador API35. Cada execução tem limite de tempo, sem loop infinito, sem chave de IA, sem chamada do Codex e sem publicação na Play Store.

Artefatos: APK, XML de testes, matriz de física, logs, imagens atuais, JSON por partida e vídeos nativos. Imagens antigas da raiz nunca são usadas como evidência de uma nova execução.

Uma execução vermelha não é sucesso. Um vídeo demonstra a execução nele mostrada, não ausência de todos os defeitos. A compilação/execução deve ser vinculada ao hash em `lab-evidence/commit.txt`.
