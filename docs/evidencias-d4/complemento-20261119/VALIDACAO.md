# Validação complementar D4/D15 e regressão D5

Data da execução: 19/11/2026. Responsável pela complementação técnica: Mauro, com assistência de IA. Base integrada avaliada: `b00bc55c267edaa893f39dee9a850fc8f0abc46f`.

## Ambiente e limites da execução

Ambiente Linux descartável na nuvem, OpenJDK 21.0.13, MariaDB 11.8.6 e Connector/J 4.0.0 já incluído no projeto. Código compilado pelo módulo jdk.compiler da distribuição disponível. Banco isolado recém-criado, com schema.sql e migrações 011–016, usando exclusivamente `autobotz_d15_test` e `autobotz_d5_test`. Contas fictícias e permissões limitadas a essas bases. Nenhuma credencial ou dado de produção foi usado.

O bloqueio inicial ERR_CONNECTION_REFUSED foi resolvido ao executar o servidor e o MariaDB no mesmo ambiente de rede do navegador da nuvem. A complementação visual foi realizada em Chromium, viewport 1363 × 936 CSS px, sobre a base acima com as alterações do PR #59. Mauro concluiu manualmente o login/MFA com conta fictícia; a consulta administrativa funcionou no backend JDBC real de teste. Não foram registrados senhas, cookies ou códigos MFA nas capturas.
Celular físico não está acessível nesta sessão. Essa validação permanece pendente de aparelho e execução real.

## Ajustes necessários encontrados

O cálculo das cores CSS mostrou bordas de campos com contraste de 1,64:1 no tema claro e 1,86:1 no escuro. O foco amarelo sobre o fundo claro tinha 2,00:1. Foram ajustados somente os tokens de borda e de foco. O amarelo decorativo do cabeçalho e do material permanece no design.

| Elemento sobre o fundo da página | Claro antes | Claro depois | Escuro antes | Escuro depois |
| --- | ---: | ---: | ---: | ---: |
| Texto | 14,44:1 | 14,44:1 | 15,16:1 | 15,16:1 |
| Links | 7,24:1 | 7,24:1 | 9,24:1 | 9,24:1 |
| Bordas dos campos | 1,64:1 | 3,63:1 | 1,86:1 | 4,62:1 |
| Indicador de foco | 2,00:1 | 7,24:1 | 8,22:1 | 9,24:1 |

O teste `tests/D4ContrasteTest.js` calcula luminância e contraste das cores CSS. Verifica texto/links com mínimo 4,5:1 e bordas/foco com mínimo 3:1. As razões foram calculadas sobre cores sólidas do CSS, sem avaliação completa de todos os estados e combinações renderizados.

Referências usadas para os critérios de contraste:
- https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
- https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html

O CSS também passou a declarar largura mínima de 44 px, além da altura mínima já existente, para botões, campos, links do catálogo e o atalho de pular conteúdo. O atalho passou a usar display inline-block para que suas dimensões mínimas se apliquem. No navegador desktop foram medidos os campos de busca/login/datas, os quatro filtros, quatro links de materiais e os botões Entrar/Sair/Consultar: todos tinham altura mínima de 44 px e largura superior a 44 px. Isso não substitui medição em celular físico e de todos os estados responsivos.

## Validações desta execução

| Área | Método e resultado | Evidência |
| --- | --- | --- |
| Portal HTTP, catálogo, eventos e autorização | D15PortalTest aprovado | D15PortalTest.log |
| Conteúdo publicado e catálogo | D15ConteudoTest aprovado | D15ConteudoTest.log |
| Ciclo de abertura/encerramento do portal | D15AtalhoTest aprovado | D15AtalhoTest.log |
| Login, MFA, bloqueio e regras | D14SegurancaTest aprovado | D14SegurancaTest.log |
| Tokens, logout, expiração, rebaixamento de perfil | D14SessaoWebTest aprovado | D14SessaoWebTest.log |
| Eventos em banco real de teste | D15PersistenciaTest aprovado | D15PersistenciaTest.log |
| Login ADMIN/VENDEDOR com MFA e sessão HTTP real | D15AuthHttpJdbcTest aprovado | D15AuthHttpJdbcTest.log |
| Conferência SQL adicional dos eventos | Dois eventos HTTP válidos gravados uma vez cada, evento inválido recusado | D4BancoAudit.log |
| Integridade dos logs D5 | D5IntegridadeTest aprovado | D5IntegridadeTest.log |
| Atualização defeituosa e rollback do portal D5 | D5RollbackPortalTest aprovado | D5RollbackPortalTest.log |
| Recuperação de logs no MariaDB de teste D5 | D5RecuperacaoJdbcTest aprovado | D5RecuperacaoJdbcTest.log |
| Contraste após ajustes | D4ContrasteTest aprovado | D4ContrasteTest.log |

O fluxo JDBC verifica senha válida exigindo MFA, cookie emitido após MFA, ADMIN recebendo HTTP 200 nas métricas, VENDEDOR recebendo 403 e cookie invalidado após logout recebendo 401. Isso comprova protocolo e persistência. A complementação visual confirmou o formulário antes do login, a sessão autenticada e o dashboard com dados reais de execução. A etapa MFA foi concluída manualmente por Mauro; não foi capturada a tela com o código de demonstração.

A auditoria SQL complementar é uma verificação de persistência de chamadas HTTP isoladas. Ela não mede duplicidade em navegação real do navegador nesta execução.

Compilação concluída com dois avisos preexistentes, documentados em compilacao.log. Sintaxe do app.js, git diff --check e verificador de links Markdown passaram. O ensaio completo de backup/restauração do D5 tem guarda específica para o CI. Foi aprovado no GitHub Actions, commit 147ee24106234446b72e4781ca57faeab44eeed7, execução 44595959177 (Integrated ERP Regression), junto à regressão integrada. Essa execução local cobriu integridade, rollback do portal e recuperação JDBC.

## Evidências anteriores preservadas

Os testes de busca com e sem acento, navegação por link direto/recarga/histórico, teclado no guia, zoom 200%, temas e capturas de 320/375/768/1280 px já estão documentados na entrega integrada do PR #49. Foram preservados como histórico e não repetidos sem necessidade. O tema claro e o foco de teclado foram observados nesta complementação desktop. A matriz completa em tema escuro, zoom e resoluções com os novos tokens continua pendente.

## Alinhamento com Higor

O autor `higorleonardo-create` foi confirmado nos PRs #21 e #51. A solicitação de resultados de participantes, problemas reproduzidos, versão e evidências foi enviada no PR #51. O plano atual em docs/D4_PLANO_TESTES_USABILIDADE.md permanece marcado como planejado/não executado. Não foram inventados participantes, tempos ou satisfação. O fechamento dos fluxos operacionais depende do retorno do Higor.

## Tutorial D15

Os slugs roteiro-tutorial-3-min e roteiro-video-30-min continuam no catálogo como tipo ROTEIRO e formato MARKDOWN. Os títulos informam não gravado/gravação pendente. Não foi encontrado arquivo mp4, webm, mov ou m4v em portal-treinamento. Não há evidência de tutorial gravado nesta versão.

A solicitação ao Higor também pede responsável, previsão de gravação/revisão e confirmação de arquivo efetivamente produzido. Até o retorno, o que está entregue é o roteiro textual. A gravação e publicação do vídeo permanecem pendentes, conforme entrega/D15_LANCAMENTO_E_MATERIAIS.md.

## Complementação visual e correção da restauração da sessão

A consulta de métricas foi executada no período 21/10/2026 a 19/11/2026 (fim inclusivo). O dashboard mostrou origem DADOS_REAIS_DE_EXECUCAO. Abrir guia-rapido, usar Voltar aos materiais, Avançar e recarregar preservou um único MATERIAL_VIEW. A consulta SQL final no banco descartável confirmou MATERIAL_VIEW/guia-rapido = 1 e PORTAL_OPEN = 3. Cada recarga completa registra uma nova abertura do portal; isso não foi confundido com duplicação da leitura.

Foi reproduzida uma falha: após recarregar, a interface escondia o dashboard e apresentava login, embora a sessão continuasse válida e a rota de métricas devolvesse os dados. O achado e a abordagem foram registrados no PR antes da alteração no fluxo compartilhado. O frontend passou a consultar a rota existente ao iniciar e restaurar a apresentação da sessão: 200 restaura ADMIN, 403 informa ausência de permissão ADMIN e 401 mantém login. Uma revisão do estado impede que resposta atrasada desfaça um logout. Nenhuma rota ou regra de autorização foi alterada.

O teste D4SessaoInterfaceTest.js cobre 200/401/403/500, falha de rede e resposta atrasada após logout. A correção foi conferida visualmente recarregando o link do guia com sessão aberta e consultando novamente as métricas. O guia e o acesso ao dashboard foram preservados. O teste usa DOM mínimo simulado e não substitui os testes HTTP/JDBC de autorização.

Também foi verificada a mensagem de período invertido: “O fim precisa ser igual ou posterior ao início.” A consulta válida voltou a exibir os dados. Tab do botão Sair alcançou o campo De, com foco visível rgb(11, 90, 104). Foi observado o mesmo contorno ao navegar entre os campos de login antes da autenticação. A página não apresentou largura horizontal excedente em 1363 CSS px (scrollWidth = 1363).

Evidências desta complementação:
- [Dashboard com sessão restaurada e métricas](dashboard-sessao-restaurada.jpg).
- [Formulário antes da autenticação](login-antes-autenticacao.jpg).
- [Teste da restauração da interface](D4SessaoInterfaceTest.log).
- [Conferência SQL após a navegação](D4NavegadorBanco.log).

## Pendências para fechamento acadêmico

- Celular físico e registro do modelo/navegador usados.
- Revisão completa dos novos contornos em tema escuro, zoom e diferentes resoluções; o resultado desktop claro não aprova toda a matriz.
- Captura da etapa MFA sem expor código e conferência visual de logout/expiração (regras já cobertas pelos testes HTTP/JDBC).
- Retorno do Higor com resultados reais de usabilidade e confirmação de como a gravação será organizada.
- A execução integrada citada comprova o commit 147ee241. A alteração posterior de restauração tem testes JS locais e deve ter sua nova execução de CI conferida no PR.

Esta entrega encaminha a validação técnica e os limites em 19/11, antes do prazo de 20/11. O fechamento acadêmico completo depende das pendências explicitadas.
