# D14 — Login seguro, tentativas e MFA (João)

Branch sugerida: `feature/d14-auth-mfa` · Migração: `db/migrations/014_d14_security.sql`

## O que existe

| Requisito do plano | Implementação |
|---|---|
| Hash com sal e derivação | `util/SenhaHash`: `pbkdf2_sha256$<iter>$<salB64>$<hashB64>`, PBKDF2WithHmacSHA256, 600.000 iterações, sal de 16 bytes |
| Migração do SHA-256 legado | No login bem-sucedido, `AuthService` regrava o hash (compare-and-set em `UsuarioDAO.atualizarSenhaHash`). Hash legado = 64 hex |
| Bloqueio 5 falhas / 10 min | Janela móvel por usuário, calculada com `Clock` injetável. Um acerto zera a contagem; tentativas já bloqueadas **não** prorrogam o bloqueio |
| Sem enumeração de usuário | Usuário inexistente, senha errada e conta bloqueada produzem o mesmo `ResultadoLogin.NEGADO` e a mesma mensagem; inexistente também é bloqueado; verificação "falsa" iguala o tempo de resposta |
| MFA de uso único | `MfaService`: 6 dígitos, validade 5 min, 3 tentativas, consumo atômico, novo desafio invalida o anterior; só sal+hash no banco |
| Trilha de segurança | Tabela `eventos_seguranca` (LOGIN, LOGIN_BLOQUEADO, MFA_EMITIDO, MFA_VERIFICACAO, HASH_MIGRADO). `origem` fica NULL: não há IP remoto em execução local |
| Consulta restrita | `SegurancaService.consultarEventos` exige `Permissao.CONSULTAR_EVENTOS_SEGURANCA` (só ADMIN); a checagem é no serviço |

## Fluxo de login (Swing e console)

1. `AuthService.iniciarLogin(usuario, senha)` → `AUTENTICADO`, `MFA_NECESSARIA` ou `NEGADO`.
2. Se `MFA_NECESSARIA`, a tela mostra o código **rotulado como DEMONSTRAÇÃO** e chama `concluirMfa(idDesafio, codigo)`.
3. Só `AUTENTICADO` chega a `SessaoUsuario.setUsuario(...)`.
   `INVALIDO` permite nova tentativa; `EXPIRADO`, `ESGOTADO`, `JA_UTILIZADO` e `INEXISTENTE` exigem refazer o passo 1.

`AuthService.autenticar(nome, senha)` foi mantido (usado por `CoreRegression`), mas é **somente o primeiro fator**:
aplica bloqueio, trilha e migração de hash, porém **não** aplica MFA. Telas devem usar `iniciarLogin`.

## Decisões que precisam de confirmação (Arthur / equipe)

- **Política de MFA:** `AuthService.PERFIS_COM_MFA = {ADMIN}` (VENDEDOR entra só com senha). É uma constante; mudar a política é uma linha.
- **Permissão nova:** `CONSULTAR_EVENTOS_SEGURANCA` no enum de `AutorizacaoService` (ADMIN recebe via `allOf`; VENDEDOR não).
- **Migração 014:** contém só a parte de João. As colunas de cifra/índice HMAC de `clientes` são do Arthur e devem ser acrescentadas por ele.
- **`schema.sql` não foi editado** (regra do plano). Arthur consolida depois.

## Atenção para a CI (Adryan)

Depois desta mudança, `cadastrar()` grava um hash ~100 caracteres em `usuarios.senha_hash`, que hoje é `CHAR(64)`.
O banco `_test` **precisa receber a migração 014** antes da `CoreRegression`, senão o cadastro de `audit_admin` falha.
A migração não usa `USE`/`CREATE DATABASE`.

## Como testar

```bash
# compilar (precisa de JDK 17+; o driver JDBC só é exigido para rodar contra o banco)
mkdir -p build/classes build/tests
javac -encoding UTF-8 -Xlint:all -d build/classes $(find src/autobotz -name '*.java')
javac -encoding UTF-8 -Xlint:all -cp build/classes -d build/tests tests/D14SegurancaTest.java
java -cp build/classes:build/tests:src D14SegurancaTest     # termina com "D14_OK checks=58"
```

`D14SegurancaTest` não usa banco (relógio controlável + repositório em memória) e leva ~15 s por causa das 600 mil iterações do PBKDF2.

## Limitações declaradas

- MFA é **simulado**: não há envio real de SMS/e-mail. O código aparece na tela como demonstração.
- A camada SQL (`SegurancaDAO`, `UsuarioDAO.buscarPorId/atualizarSenhaHash`, migração 014) **não foi executada** contra MariaDB nesta entrega; precisa de teste de integração em banco `_test`.
- Não há função "antirransomware"; o plano de resposta a incidentes (identificação, contenção, recuperação, comunicação, pós-incidente) é um documento a ser escrito e não está neste módulo.
- Não existe painel Swing de eventos de segurança; o serviço está pronto para o Arthur ligar a um `JPanel` no `AppFrame`.
- Se o hash de um usuário já usa PBKDF2 com menos iterações que o padrão, ele é reforçado no próximo login.
