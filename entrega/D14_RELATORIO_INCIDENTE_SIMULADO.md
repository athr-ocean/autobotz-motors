# D14 — Relatório de incidente simulado e comunicação

**Natureza:** exercício acadêmico simulado. Não há evidência, neste relatório, de que tenha ocorrido um incidente real contra o AutoBotz Motors.

## 1. Cenário e evidências que o sistema pode registrar

O exercício considera tentativas de autenticação inválidas contra contas do ERP e a tentativa de acessar dados pessoais de clientes ou o CRM antes de concluir o segundo fator. O cenário existe para validar controles e resposta, não para afirmar que essas tentativas aconteceram em produção.

A trilha `eventos_seguranca` pode registrar, conforme o fluxo executado, o nome de usuário informado, data/hora, tipo de evento, sucesso, causa e origem quando disponível. Exemplos de eventos previstos são `LOGIN` com causa `SENHA_INVALIDA`, `LOGIN_BLOQUEADO`, `MFA_EMITIDO`, `MFA_VERIFICACAO` e `ACESSO_NEGADO` com causa `SEM_PERMISSAO:<permissão>`.

- **Tentativas/usuários:** devem ser apurados consultando os eventos do ambiente efetivamente investigado. Este pacote não inclui uma exportação de eventos de produção; portanto, não afirma quantidade real de tentativas nem identifica contas reais atacadas.
- **Origem:** no Swing/console local, o endereço de rede não é observável e `origem` permanece `NULL`. Não se devem inventar IPs. Em uma rota HTTP, a origem só pode ser relatada se a camada de servidor a observar e registrar de forma confiável; cabeçalhos enviados pelo cliente não são prova de endereço de origem.
- **Vulnerabilidade exercitada:** a política anterior exigia MFA apenas para `ADMIN`, embora `VENDEDOR` pudesse listar/cadastrar clientes e acessar o CRM. Isso deixava operações com dados pessoais acessíveis após somente a senha. A política foi alterada para exigir o segundo fator para todos os perfis atuais.
- **Impacto real:** desconhecido/não aplicável ao exercício. Não foi fornecida evidência de acesso indevido ou exfiltração real.

## 2. Plano de resposta

### Identificação

1. Receber o alerta ou relato e registrar horário, sistema/ambiente, conta afetada e quem observou o comportamento.
2. Consultar `eventos_seguranca` por intervalo de tempo e usuário; correlacionar `LOGIN`, `LOGIN_BLOQUEADO`, `MFA_*` e `ACESSO_NEGADO`.
3. Separar fatos observados de hipóteses. Não concluir que houve comprometimento apenas por falhas de login.

### Contenção

1. Revogar sessões web suspeitas por logout ou reinicialização controlada do serviço, considerando que as sessões deste protótipo ficam em memória.
2. Desabilitar temporariamente a conta suspeita ou redefinir sua senha por procedimento administrativo validado.
3. Bloquear a rota ou operação vulnerável até confirmar que a autorização no servidor é aplicada antes de consultar ou alterar dados.
4. Preservar os registros necessários à análise, com acesso restrito; não copiar senhas, códigos MFA ou tokens para o relatório.

### Recuperação

1. Aplicar a correção de MFA e verificar que senha correta, isoladamente, não cria sessão/token.
2. Testar o segundo fator inválido, expirado e reutilizado; testar os acessos de clientes e CRM antes e depois do MFA.
3. Confirmar as permissões no serviço do servidor, sem confiar apenas em ocultação de botões na interface.
4. Restaurar o acesso gradualmente após os testes e monitorar eventos de negação e bloqueio.

### Análise posterior

1. Construir uma linha do tempo a partir de registros reais disponíveis.
2. Documentar causa raiz, alcance confirmado, dados possivelmente expostos e incertezas.
3. Criar ações corretivas com responsável e prazo; revisar permissões por perfil e a retenção/acesso aos logs.
4. Repetir os testes de segurança após a correção e guardar seus resultados, sem misturar simulações com eventos reais.

## 3. Comunicado-modelo aos clientes

**Assunto: Atualização de segurança do AutoBotz Motors**

Estamos revisando os controles de acesso do sistema AutoBotz Motors para garantir que operações envolvendo dados de clientes exijam autenticação adicional. Como parte desse trabalho, reforçamos o fluxo de autenticação e a verificação de permissões no servidor.

Neste momento, este comunicado é um modelo de exercício acadêmico e não representa a confirmação de um incidente real nem de exposição de dados. Se uma investigação real identificar impacto sobre informações de clientes, enviaremos uma comunicação específica com os fatos confirmados, os dados envolvidos, as medidas adotadas e as orientações necessárias.

Em caso de dúvidas, entre em contato pelo canal oficial de atendimento da organização.

## 4. Perguntas e respostas

**Houve um vazamento confirmado?**  Não há confirmação de incidente real neste exercício. Qualquer afirmação sobre exposição precisa ser sustentada por evidências da investigação.

**Quais dados podem estar envolvidos?**  O exercício trata de acesso a cadastro de clientes e CRM. O conjunto real de dados afetados só pode ser informado após análise dos registros e das consultas realizadas.

**O que foi corrigido?**  O segundo fator passou a ser obrigatório para os perfis existentes, incluindo `VENDEDOR`, antes de criar uma sessão. A autorização dos recursos continua sendo verificada no serviço do servidor.

**O MFA envia SMS ou e-mail?**  Não. Neste projeto, o MFA é simulado para fins didáticos; o código é apresentado como demonstração. Isso não equivale a um segundo fator de produção entregue por um canal independente.

**Como saberemos se houve impacto?**  Pela análise de eventos, sessões e operações do ambiente investigado. Este relatório não substitui essa apuração nem inventa quantidades, usuários ou endereços IP.
