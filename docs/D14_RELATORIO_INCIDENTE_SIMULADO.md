# D14 — Relatório de incidente simulado e plano de resposta

**Classificação:** exercício acadêmico simulado; não é relato de incidente real.  
**Data da revisão do código:** 28/10/2026.  
**Responsável pela frente:** João — autenticação, MFA e resposta inicial.  
**Dados de produção:** não consultados nem utilizados neste exercício.

## 1. Resumo executivo do cenário

A revisão identificou uma lacuna de política no código anterior: o MFA era exigido somente de `ADMIN`, embora `VENDEDOR` pudesse listar/cadastrar clientes e acessar o CRM. Nesse estado, uma senha válida de vendedor era suficiente para abrir sessão e alcançar funções com dados pessoais. Isso é uma vulnerabilidade de controle de acesso no desenho da autenticação; não é evidência de que alguém explorou a falha.

A correção realizada exige MFA simulado para os perfis existentes `ADMIN` e `VENDEDOR`. O primeiro fator agora produz `MFA_NECESSARIA` sem usuário autenticado/token; a sessão só pode ser aberta após a verificação do desafio.

## 2. Tentativas e usuários observados no exercício

Os nomes abaixo são contas de teste criadas por fixtures dos testes automatizados. Não representam usuários reais nem acessos de produção.

| Evento no cenário | Conta/identidade de teste | Resultado esperado/observado nos testes locais | Significado |
|---|---|---|---|
| Quatro senhas incorretas na janela de 10 minutos | `vendedor` | As quatro tentativas são negadas; não há sessão | O limite ainda não foi atingido. |
| Quinta senha incorreta na mesma janela | `vendedor` | `BLOQUEADO`, com 600 segundos restantes no início do bloqueio | A política bloqueia novas tentativas por usuário. |
| Tentativa com senha certa durante o bloqueio | `vendedor` | Continua `BLOQUEADO`; não recebe desafio nem sessão | A senha certa não contorna o bloqueio. |
| Senha correta depois do término do bloqueio | `vendedor` | `MFA_NECESSARIA`, sem sessão/token até o segundo fator | A correção impede login só com senha. |
| Código MFA incorreto | `vendedor` ou `admin` de teste | Acesso negado; desafio pode permitir nova tentativa conforme o limite | Um código incorreto não autentica a sessão. |
| Código MFA correto | `vendedor` ou `admin` de teste | Autenticação liberada; o desafio é de uso único | O segundo fator é necessário para abrir sessão. |
| Tentativa com nome inexistente | `fantasma` | Mesma decisão genérica de credenciais inválidas e política de bloqueio | O fluxo não deve revelar se o usuário existe. |

Os testes executados nesta revisão foram `D14SegurancaTest` com 72 verificações e `D14SessaoWebTest` com 59 verificações. Eles usam repositório/usuários em memória e HTTP local de teste. Não são logs de um incidente real nem uma medição do banco de produção.

## 3. Origem dos acessos

- **Desktop/console:** o IP de origem não é observável no fluxo local; a trilha mantém `origem = NULL`. Não atribuir endereço IP a tentativas feitas nesses clientes.
- **Teste HTTP local:** as verificações usam um servidor de teste em loopback para conferir o contrato HTTP. O teste não representa tráfego externo de cliente nem permite atribuir a tentativa a uma pessoa.
- **Produção:** nenhuma origem real foi consultada nesta revisão. Não há IP, geolocalização ou usuário real a declarar.

## 4. Vulnerabilidade e impacto possível

**Falha simulada:** política de segundo fator restrita a `ADMIN`, apesar de `VENDEDOR` possuir permissões de `LISTAR_CLIENTES`, `CADASTRAR_CLIENTE` e `ACESSAR_CRM`.

**Impacto possível se explorada:** acesso não autorizado a dados pessoais de clientes por uma conta de vendedor cuja senha tenha sido obtida por terceiro. O alcance real depende das permissões efetivas e da operação executada. Não há evidência de que dados tenham sido acessados, exfiltrados, alterados ou expostos; esse exercício não confirma incidente real nem vazamento.

## 5. Ações realizadas na revisão

1. A política `AuthService.PERFIS_COM_MFA` passou a incluir todos os perfis enumerados, cobrindo `ADMIN` e `VENDEDOR` atuais.
2. Os testes de autenticação foram atualizados para demonstrar que senha correta do vendedor não libera usuário sem MFA.
3. Os testes de sessão web agora verificam que, antes do MFA, o vendedor não consegue obter token nem autorizar `LISTAR_CLIENTES`, `CADASTRAR_CLIENTE` ou `ACESSAR_CRM`.
4. Após MFA válido, a autorização no servidor continua respeitando a matriz RBAC; por exemplo, vendedor não recebe permissões exclusivas de administrador.
5. O MFA permanece explicitamente **SIMULADO**. Não é um segundo fator entregue por canal independente e não deve ser descrito como MFA de produção.

## 6. Plano de resposta a incidentes

### Identificação

- Revisar a trilha `eventos_seguranca` para `LOGIN`, `LOGIN_BLOQUEADO`, `MFA_EMITIDO`, `MFA_VERIFICACAO` e `ACESSO_NEGADO`.
- Agrupar por nome de conta, horário, resultado e causa; observar padrões de falha repetida, bloqueios e desafio MFA inválido/expirado.
- Examinar a origem somente quando a camada que recebe a conexão a observar de fato. Para desktop/console, preservar `NULL`; não fabricar IPs.
- Confirmar evidência no banco de logs e nos controles do servidor antes de classificar o caso como incidente real.

### Contenção

- Suspender ou bloquear a conta suspeita conforme o procedimento institucional aprovado e invalidar as sessões ativas relacionadas quando houver mecanismo disponível.
- Impedir novas tentativas pela política de bloqueio; se houver evidência de credencial comprometida, iniciar troca de senha por canal verificado.
- Restringir temporariamente a rota/permissão afetada no servidor se a autorização não puder ser comprovada.
- Preservar cópias dos logs relevantes, com acesso restrito, horários e responsáveis. Não registrar senhas, tokens ou códigos MFA.

### Recuperação

- Corrigir a causa técnica e validar em ambiente descartável antes de reabrir o acesso.
- Revalidar a matriz RBAC e as sessões, revogar tokens afetados e confirmar que a conta passa por senha + segundo fator.
- Conferir a integridade dos registros de clientes e dos backups; se houver evidência de alteração, restaurar apenas após preservar a evidência e aprovar a recuperação.
- Reabrir o fluxo gradualmente depois que os testes de aceitação passarem e o responsável pelo serviço autorizar.

### Análise posterior

- Registrar linha do tempo, vetor de acesso, contas afetadas, dados efetivamente acessados, decisões tomadas e evidências disponíveis.
- Distinguir causa raiz comprovada de hipótese. Documentar os pontos sem telemetria e as limitações do MFA simulado.
- Criar ações preventivas com responsável e prazo; revisar testes para evitar regressão e registrar quem aprovou a retomada.
- Revisar a comunicação enviada e oferecer canal de contato; corrigir a comunicação se novas evidências alterarem o escopo.

## 7. Comunicado preparado para clientes — uso apenas após confirmação

> **AVISO: TEXTO DE SIMULAÇÃO. NÃO ENVIAR COMO NOTIFICAÇÃO DE INCIDENTE REAL.**
>
> **Assunto: Atualização de segurança do AutoBotz Motors**
>
> Estamos realizando uma revisão de segurança nos controles de acesso do AutoBotz Motors. Como medida preventiva, estamos reforçando a verificação de identidade antes de liberar funções que tratam dados de clientes.
>
> Este texto faz parte de um exercício acadêmico e não afirma que houve acesso indevido, vazamento ou perda de dados. Caso uma investigação real confirme um incidente que possa afetar seus dados, enviaremos uma comunicação específica com o período, as informações envolvidas, as providências adotadas e as orientações pertinentes, após validar esses fatos.
>
> **Perguntas e respostas**
>
> **Ocorreu um vazamento dos meus dados?**  
> Este exercício não encontrou nem confirma vazamento. A existência e o alcance de qualquer incidente real só podem ser informados após investigação com evidências.
>
> **Quais dados estão envolvidos?**  
> A revisão trata das permissões para funções de clientes e CRM. Não há, neste exercício, evidência de que registros de clientes tenham sido acessados ou extraídos por terceiros.
>
> **O que mudou?**  
> A aplicação passou a exigir a etapa de MFA simulado para os perfis existentes antes de abrir uma sessão. A implementação é acadêmica e o segundo fator não é enviado por SMS ou e-mail.
>
> **Preciso fazer alguma coisa agora?**  
> Este aviso é uma peça de simulação e não exige ação do cliente. Em uma comunicação real, as orientações serão específicas, verificadas e enviadas pelos canais oficiais da instituição.
>
> **Como posso tirar dúvidas?**  
> Em caso real, substituir este trecho pelo canal de contato aprovado pela instituição antes de enviar. Não inventar telefone, endereço de e-mail nem prazo de atendimento.

## 8. Limites e pendências de evidência

- Não foi observado incidente real; o relatório descreve um cenário simulado e uma lacuna de política encontrada no código.
- Nenhum IP foi inventado para desktop/console; a origem fica desconhecida/NULL quando não observável.
- O MFA é simulado e não substitui uma entrega real de segundo fator.
- A regressão JDBC requer um MariaDB descartável com nome `autobotz_*_test`; não foi executada nesta sessão.
