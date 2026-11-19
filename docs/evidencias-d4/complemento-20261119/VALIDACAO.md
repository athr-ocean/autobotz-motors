# Validação complementar D4/D15 e regressão D5

Data da execução: 19/11/2026. Responsável pela complementação técnica: Mauro, com assistência de IA. Base integrada avaliada: `b00bc55c267edaa893f39dee9a850fc8f0abc46f`.

## Ambiente e limites da execução

Ambiente Linux descartável na nuvem, OpenJDK 21.0.13, MariaDB 11.8.6 e Connector/J 4.0.0 já incluído no projeto. Código compilado pelo módulo jdk.compiler da distribuição disponível. Banco isolado recém-criado, com schema.sql e migrações 011–016, usando exclusivamente `autobotz_d15_test` e `autobotz_d5_test`. Contas fictícias e permissões limitadas a essas bases. Nenhuma credencial ou dado de produção foi usado.

A tentativa de abrir o portal em `http://127.0.0.1:18087/` no navegador da nuvem retornou `ERR_CONNECTION_REFUSED`. O servidor e as bases funcionaram no contexto de execução dos testes HTTP/JDBC. A ferramenta de registro do erro do navegador também recusou a página interna de erro. A execução visual de login/MFA/dashboard e novas capturas ficaram bloqueadas. Nenhuma captura antiga é apresentada como prova desta execução.

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

O CSS também passou a declarar largura mínima de 44 px, além da altura mínima já existente, para botões, campos, links do catálogo e o atalho de pular conteúdo. O atalho passou a usar display inline-block para que suas dimensões mínimas se apliquem. A declaração CSS está verificada por inspeção. A medição de todos os retângulos renderizados continua pendente do navegador.

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

O fluxo JDBC verifica senha válida exigindo MFA, cookie emitido após MFA, ADMIN recebendo HTTP 200 nas métricas, VENDEDOR recebendo 403 e cookie invalidado após logout recebendo 401. Isso comprova protocolo e persistência. A aparência e interação visual dos formulários e dashboard permanecem pendentes.

A auditoria SQL complementar é uma verificação de persistência de chamadas HTTP isoladas. Ela não mede duplicidade em navegação real do navegador nesta execução.

Compilação concluída com dois avisos preexistentes, documentados em compilacao.log. Sintaxe do app.js, git diff --check e verificador de links Markdown passaram. O ensaio completo de backup/restauração do D5 tem guarda específica para o CI e deve ser conferido na regressão do PR. Essa execução local cobriu integridade, rollback do portal e recuperação JDBC.

## Evidências anteriores preservadas

Os testes de busca com e sem acento, navegação por link direto/recarga/histórico, teclado no guia, zoom 200%, temas e capturas de 320/375/768/1280 px já estão documentados na entrega integrada do PR #49. Foram preservados como histórico e não repetidos sem necessidade. As correções atuais de contraste exigem nova inspeção visual quando houver acesso ao portal no navegador.

## Alinhamento com Higor

O autor `higorleonardo-create` foi confirmado nos PRs #21 e #51. A solicitação de resultados de participantes, problemas reproduzidos, versão e evidências foi enviada no PR #51. O plano atual em docs/D4_PLANO_TESTES_USABILIDADE.md permanece marcado como planejado/não executado. Não foram inventados participantes, tempos ou satisfação. O fechamento dos fluxos operacionais depende do retorno do Higor.

## Tutorial D15

Os slugs roteiro-tutorial-3-min e roteiro-video-30-min continuam no catálogo como tipo ROTEIRO e formato MARKDOWN. Os títulos informam não gravado/gravação pendente. Não foi encontrado arquivo mp4, webm, mov ou m4v em portal-treinamento. Não há evidência de tutorial gravado nesta versão.

A solicitação ao Higor também pede responsável, previsão de gravação/revisão e confirmação de arquivo efetivamente produzido. Até o retorno, o que está entregue é o roteiro textual. A gravação e publicação do vídeo permanecem pendentes, conforme entrega/D15_LANCAMENTO_E_MATERIAIS.md.

## Pendências para fechamento acadêmico

- Celular físico e registro do modelo/navegador usados.
- Inspeção visual do login/MFA, sessão e dashboard administrativo sobre o banco de teste, com capturas.
- Medição dos alvos renderizados e revisão visual dos novos contornos nos temas e resoluções, incluindo zoom e teclado nos formulários.
- Retorno do Higor com resultados reais de usabilidade e confirmação de como a gravação será organizada.
- Conferência do CI integrado do PR, incluindo o backup/restauração exclusivo do CI.

Esta entrega encaminha a validação técnica e os bloqueios em 19/11, antes do prazo de 20/11. O fechamento acadêmico completo depende das pendências explicitadas.
