# Evidências visuais do D4

Capturas produzidas manualmente por Mauro no Brave, emulação de dispositivos, e compartilhadas para revisão.

| Arquivo | Versão | Largura CSS informada |
| --- | --- | --- |
| antes-catalogo-344.png | Base 4dd760b, catálogo | 344 px |
| antes-guia-344.png | Base 4dd760b, guia | 344 px |
| antes-catalogo-desktop.png | Base 4dd760b, catálogo | Desktop, largura não confirmada |
| antes-guia-desktop.png | Base 4dd760b, guia | Desktop, largura não confirmada |
| depois-guia-320-escuro.png | D4 ad96703, guia | 320 px |
| depois-guia-375-escuro.png | D4 ad96703, guia | 375 px |
| depois-guia-768-escuro.png | D4 ad96703, guia | 768 px |
| depois-guia-1280-escuro.png | D4 ad96703, guia | 1280 px |

As dimensões do PNG incluem a escala de pixels do dispositivo e podem diferir da largura CSS. As larguras de emulação foram informadas por Mauro. As capturas comprovam os trechos visíveis. A ausência de rolagem horizontal ao percorrer o guia foi confirmada manualmente em 375 px.

## Validação funcional

No commit ad96703, com MockPortalBackend em memória na porta 18086, Mauro confirmou busca por `rapido` e `rápido`, guia por link direto após recarga, Voltar/Avançar do navegador, abertura e retorno pelo teclado, zoom de 200% no desktop e legibilidade nos temas claro e escuro.

A aba Rede mostrou apenas PORTAL_OPEN após recarga de uma entrada já visitada. Mauro confirmou que Voltar/Avançar mantiveram a contagem e uma nova abertura pela lista enviou um único MATERIAL_VIEW para guia-rapido.

## Testes automatizados

D15PortalTest, D15ConteudoTest e D15AtalhoTest compilados com javac -Xlint:all e executados com sucesso nesta entrega, sem MariaDB.

## Limitações

Celular físico, persistência no MariaDB, autenticação administrativa completa, medição de contraste e medição dos alvos de toque não verificados nesta entrega. A matriz de capturas do antes contém 344 px e desktop. A matriz de capturas do depois contém 320, 375, 768 e 1280 px.
