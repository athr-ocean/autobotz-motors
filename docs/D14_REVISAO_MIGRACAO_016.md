# D14 — Revisão de compatibilidade da migration 016 com a 014

**Data da revisão:** 28/10/2026.  
**Escopo:** confirmar a separação entre a autenticação/MFA de João e a proteção de dados pessoais de clientes da PR #18 de Arthur.  
**Resultado:** compatibilidade estrutural plausível; confirmação integral bloqueada pela ausência da migration 016 no ZIP recebido.

## Escopo da migration 014 disponível localmente

`db/migrations/014_d14_security.sql` altera `usuarios.senha_hash` para `VARCHAR(255)` e cria `eventos_seguranca` e `mfa_desafios`. A migration não altera a tabela `clientes` nem cria colunas de cifra/HMAC para os dados pessoais.

## Informação pública consultada sobre a PR #18

A PR pública [#18 — feat(d14): protege dados pessoais de clientes](https://github.com/athr-ocean/autobotz-motors/pull/18) permanece aberta na consulta realizada em 28/10/2026. O comentário mais recente da PR afirma que a estrutura PII foi isolada em `db/migrations/016_d14_client_pii.sql`, em vez de alterar `014_d14_security.sql`, e orienta a sequência 014 → 015 → 016. O escopo descrito para a 016 é a proteção de dados em `clientes`, enquanto a 014 cobre usuários, eventos de segurança e desafios MFA.

## Avaliação

- **Separação de tabelas:** pelo escopo declarado, a 014 e a 016 atuam em tabelas diferentes (`usuarios`, `eventos_seguranca`, `mfa_desafios` contra `clientes`), sem colisão direta de nomes.
- **Ordem:** a ordem 014 → 015 → 016 é consistente com a sequência declarada na PR, desde que `clientes` exista conforme `schema.sql` e que 016 não dependa de objetos criados somente depois dela.
- **Responsabilidade:** a migration 016 deve permanecer separada da 014 para preservar autoria e permitir que a política de autenticação/MFA e a cifra/HMAC evoluam independentemente.
- **MFA:** as alterações desta frente não dependem da migration PII para negar sessão sem segundo fator; a verificação do MFA é feita antes de emitir a sessão/token.

## O que não foi possível confirmar nesta cópia

O ZIP recebido contém `014_d14_security.sql` e `015_d15_treinamento.sql`, mas **não contém `016_d14_client_pii.sql`**. Além disso, esta cópia não contém `.git`, e o ambiente atual não conseguiu acessar o repositório por `git fetch`. Portanto, não foi possível verificar o SQL exato da 016, executar a sequência completa nem confirmar chaves, tamanhos de coluna, índices, idempotência ou rollback desta migration de forma independente.

Há também uma dependência de instalação limpa já registrada em `docs/D14_LOGIN_MFA.md`: neste ZIP, `schema.sql` ainda declara `usuarios.senha_hash CHAR(64)`, enquanto `AuthService` grava PBKDF2 e a migration 014 amplia a coluna para `VARCHAR(255)`. Se a instalação iniciar só por `schema.sql` e não aplicar a migration 014, o hash não cabe. Como o schema está sob responsabilidade de Arthur, esta revisão não altera esse arquivo; é necessário que Arthur confirme a correção na instalação limpa.

A página pública da PR registra que Arthur declarou ter testado a 016 duas vezes em uma base descartável `_test`, confirmando colunas, índice, preservação de registro fictício e idempotência. Esse é um resultado declarado pelo autor da PR, não um teste reproduzido nesta revisão.

## Critério para remover o bloqueio

Incluir a migration 016 atual no working tree e executar a sequência 014 → 015 → 016 em um MariaDB descartável, nunca em produção. Conferir antes/depois: tabelas/colunas, índices, conteúdo de fixtures, segunda execução idempotente, hash de senha PBKDF2 e criação/consumo de desafios MFA. Registrar a saída real do teste e revisar o diff integral da PR #18 antes de aprovar ou integrar.
