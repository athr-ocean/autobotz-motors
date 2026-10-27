# D14 — Login seguro, tentativas e MFA (João)

Branch sugerida: `feature/d14-auth-mfa` · Migração: `db/migrations/014_d14_security.sql`
Portal HTTP: ver `docs/D14_SESSAO_WEB.md`.

## O que existe

| Requisito do plano | Implementação |
|---|---|
| Hash com sal e derivação | `util/SenhaHash`: `pbkdf2_sha256$<iter>$<salB64>$<hashB64>`, PBKDF2WithHmacSHA256, 600.000 iterações, sal de 16 bytes |
| Migração do SHA-256 legado | No login bem-sucedido, `AuthService` regrava o hash (compare-and-set em `UsuarioDAO.atualizarSenhaHash`). Hash legado = 64 hex |
| Bloqueio 5 falhas / 10 min | Janela móvel por usuário, com `Clock` injetável. Um acerto zera a contagem; tentativas já bloqueadas **não** prorrogam o bloqueio |
| Aviso de bloqueio com tempo | A quinta falha já devolve `BLOQUEADO` com o tempo restante ("Acesso bloqueado ... Tente em 9 min 59 s"). Vale para **qualquer** nome digitado, existente ou não, então não revela se a conta existe |
| Sem enumeração de usuário | Usuário inexistente e senha errada dão o mesmo `NEGADO` e a mesma mensagem; inexistente também é bloqueado igual; verificação "falsa" iguala o tempo de resposta |
| MFA de uso único | `MfaService`: 6 dígitos, validade 5 min, 3 tentativas, consumo atômico, novo desafio invalida o anterior; só sal+hash no banco |
| Trilha de segurança | Tabela `eventos_seguranca` (LOGIN, LOGIN_BLOQUEADO, MFA_EMITIDO, MFA_VERIFICACAO, HASH_MIGRADO, LOGOUT, ACESSO_NEGADO). `origem` fica NULL: não há IP remoto em execução local |
| Consulta restrita | `SegurancaService.consultarEventos` exige `Permissao.CONSULTAR_EVENTOS_SEGURANCA` (só ADMIN); a checagem é no serviço |
| Sessão do portal (401/403/200) | `SessaoWebService` — ver `docs/D14_SESSAO_WEB.md` |

## Fluxo de login (Swing e console)

1. `AuthService.iniciarLogin(usuario, senha)` → `AUTENTICADO`, `MFA_NECESSARIA`, `BLOQUEADO` ou `NEGADO`.
2. Se `MFA_NECESSARIA`, a tela mostra o código **rotulado como DEMONSTRAÇÃO** e chama `concluirMfa(idDesafio, codigo)`.
3. Só `AUTENTICADO` chega a `SessaoUsuario.setUsuario(...)`.
   `INVALIDO` permite nova tentativa; `EXPIRADO`, `ESGOTADO`, `JA_UTILIZADO` e `INEXISTENTE` exigem refazer o passo 1.

`AuthService.autenticar(nome, senha)` foi mantido (usado por `CoreRegression`), mas é **somente o primeiro fator**:
aplica bloqueio, trilha e migração de hash, porém **não** aplica MFA. Telas e servidores devem usar `iniciarLogin`.

## Decisões a confirmar (Arthur / equipe)

- **Política de MFA:** `AuthService.PERFIS_COM_MFA = {ADMIN}` (VENDEDOR entra só com senha).
- **Permissões novas:** `CONSULTAR_EVENTOS_SEGURANCA` e `CONSULTAR_METRICAS_TREINAMENTO` no enum de `AutorizacaoService` (ADMIN recebe via `allOf`; VENDEDOR não).
- **Migração 014:** contém só a parte de João. As colunas de cifra/índice HMAC de `clientes` são do Arthur.

## Atenção: `schema.sql` (instalação limpa)

`usuarios.senha_hash` precisa ser `VARCHAR(255)`. Com `CHAR(64)` o hash PBKDF2 (~100 caracteres) não cabe e `cadastrar()` falha.
A migração `014` já faz `ALTER TABLE usuarios MODIFY senha_hash VARCHAR(255) NOT NULL`; a instalação limpa precisa da mesma mudança
na definição de `usuarios` do `schema.sql` (linha a ser ajustada por Arthur, dono do schema).

## Como testar

```bash
# compilar (JDK 17+; o driver JDBC só é exigido para rodar contra o banco)
mkdir -p build/classes build/tests
javac -encoding UTF-8 -Xlint:all -d build/classes $(find src/autobotz -name '*.java')
javac -encoding UTF-8 -Xlint:all -cp build/classes -d build/tests tests/D14SegurancaTest.java tests/D14SessaoWebTest.java tests/D14JdbcIntegrationTest.java

# sem banco (relógio controlável + repositório em memória): rode na raiz do projeto
java -cp build/classes:build/tests:src D14SegurancaTest     # "D14_OK checks=70"
java -cp build/classes:build/tests:src D14SessaoWebTest     # "D14_SESSAO_WEB_OK checks=52"

# com MariaDB de TESTE (recusa qualquer URL que não seja autobotz_*_test)
export AUTOBOTZ_DB_URL=jdbc:mariadb://localhost:3306/autobotz_d14_test
export AUTOBOTZ_DB_USER=...   AUTOBOTZ_DB_PASSWORD=...
java -cp build/classes:build/tests:<driver-mariadb>.jar:src D14JdbcIntegrationTest   # "D14_JDBC_OK ..."
```

Os `.properties` do projeto estão em ISO-8859-1; os testes leem como o `ResourceBundle` do JDK (UTF-8 e, se inválido, ISO-8859-1).

## Limitações declaradas

- MFA é **simulado**: não há envio real de SMS/e-mail. O código aparece na tela como demonstração.
- `D14JdbcIntegrationTest` foi compilado, mas **não foi executado** (sem MariaDB onde foi escrito). O resultado real precisa ser registrado por quem rodar.
- Não há função "antirransomware"; o plano de resposta a incidentes (identificação, contenção, recuperação, comunicação, pós-incidente) é documento à parte.
- Não existe painel Swing de eventos de segurança; `SegurancaService` está pronto para o Arthur ligar a um `JPanel`.
- Hash PBKDF2 com menos iterações que o padrão é reforçado no próximo login.
