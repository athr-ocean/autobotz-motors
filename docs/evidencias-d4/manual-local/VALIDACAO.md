# Validação manual local — encerramento das verificações disponíveis

## Versão e ambiente
Mauro executou os passos manualmente em seu Debian, no Brave 1.93.136 (Versão oficial, x86_64), durante a sessão de 19/11/2026. O terminal confirmou checkout detached no commit d3e692b256016b48fbfb29cc539afe8772a133de, do PR #59, na worktree ~/git/autobotz-d4-depois, limpa antes da troca. A compilação javac -Xlint:all -sourcepath src dos arquivos src/autobotz/portal/*.java terminou sem mensagens. O JShell utilizado se identificou como versão 25.0.4.1; não foi coletada a versão do executável javac, portanto não se atribui JDK 21 a esta execução local.

O servidor foi PortalServer na porta 18086, ligado somente em 127.0.0.1, com MockPortalBackend e catálogo oficial. Os eventos desta etapa ficam em memória. Não é nova comprovação de persistência, autenticação/MFA real ou dashboard JDBC. A execução anterior desses fluxos está documentada separadamente no PR.

## Método e limites
Mauro operou o navegador e enviou as capturas; a assistência de IA examinou as imagens e orientou os passos. As larguras 320/375/768/1280 CSS px foram informadas por Mauro no modo responsivo, com zoom orientado a 100%. As imagens não incluem o seletor de largura ou a barra de endereço; as dimensões em pixels do PNG não são tomadas como prova da largura CSS. A inspeção visual aprova somente a área capturada; leitura completa, rolagem e ações abaixo são identificadas como confirmações manuais.

## Resultados

| Caso | Evidência observada ou confirmação de Mauro |
| --- | --- |
| Claro, 320 px | Lista/busca/filtros e trecho do guia sem corte lateral aparente nas imagens. Mauro confirmou que a lista, o formulário e a página cabem e corrigiu um relato inicial de falha: Voltar aos materiais funcionou. |
| Claro, 375 px | Lista e início do guia capturados sem cortes laterais aparentes. Retorno à lista confirmado por Mauro. |
| Claro, 768 px | Lista e guia lado a lado; botão Voltar visível. Mauro confirmou leitura do restante sem rolagem horizontal. |
| Claro, 1280 px | Lista e guia lado a lado, sem sobreposição aparente. Mauro confirmou leitura do restante sem cortes. |
| Escuro, 375 px | Lista e início do guia com texto, links e botão Voltar visíveis. Mauro confirmou leitura até o fim e retorno à lista. |
| Escuro, 1280 px | Guia, catálogo e controles visíveis na área capturada, sem sobreposição aparente. Não foi coletada medição instrumental de todos os estados. |
| Teclado, escuro/desktop | Mauro confirmou Tab da busca para Todos com contorno, Shift+Tab retornando à busca, Tab pelos filtros até o guia e Enter abrindo/focando o título. Shift+Tab para Voltar e Enter retornaram à lista com foco em Materiais. |
| Zoom 200%, escuro/desktop | Mauro confirmou texto e botão Voltar acessíveis, sem cortes laterais, após selecionar 200% pelo menu do navegador fora do modo responsivo. Resultado declarado pelo executor, sem nova captura específica. |
| Contraste e alvos | A aparência dos casos acima complementa as razões de contraste e medições anteriores. Não se declara auditoria completa WCAG nem medição de 44 px em todos os estados do aparelho. |

## Capturas originais
- [Lista 320, claro](lista-320-claro.png)
- [Guia 320, claro](guia-320-claro.png)
- [Lista 375, claro](lista-375-claro.png)
- [Guia 375, claro](guia-375-claro.png)
- [Lista 768, claro](lista-768-claro.png)
- [Guia 768, claro](guia-768-claro.png)
- [Guia 1280, claro](guia-1280-claro.png)
- [Lista 375, escuro](lista-375-escuro.png)
- [Guia 375, escuro](guia-375-escuro.png)
- [Guia 1280, escuro](guia-1280-escuro.png)

As imagens foram fornecidas por Mauro e copiadas sem edição.

## Android físico: não realizado
Mauro disponibilizou um Android e cabo USB, mas o aparelho não apareceu no gerenciador de arquivos nem na saída lsusb fornecida. Foram testados reconexão, outras portas e desbloqueio, sem sucesso relatado. A causa não foi determinada: não se conclui defeito no cabo, no celular ou no Debian. A pedido de Mauro, a tentativa foi encerrada.

Não houve acesso ao portal pelo Android; modelo, versão do Android e navegador móvel não foram coletados. O servidor permaneceu restrito a localhost; não foi alterado para exposição em Wi-Fi. Emulação responsiva não substitui celular físico.

## Fechamento para revisão
Nenhuma nova alteração de código foi necessária nesta rodada. O PR #59 pode ser revisado pelo Arthur com o código e a regressão integrada anteriormente aprovados, além destas evidências manuais. Esta rodada apenas acrescenta documentação e imagens.

Continuam não comprovados nesta rodada: Android físico, matriz completa de temas/resoluções e todos os estados de foco/contraste; captura segura da etapa MFA e conferência visual de logout/expiração (protocolos cobertos anteriormente); resultados de usabilidade com participantes e definição de gravação dependentes do Higor. O tutorial D15 permanece roteiro textual identificado como não gravado. Não se declara vídeo pronto nem aprovação final/integração feita pelo Arthur.
