# Revisão Técnica Independente — Desafio 5 (D5): Integridade, Recuperação e Rollback

- **Revisor Independente:** Adryan (Nerydeveloper)
- **Data da Revisão:** 20/11/2026
- **Branch Auditada:** `main` (commit `2fe94f0`)
- **Escopo:** Integridade referencial/transacional, recuperação JDBC, rollback do portal, scripts de backup/restore MariaDB e integração contínua (CI).

---

## 1. Preparação e Isolamento do Ambiente Local

- **Banco de Dados:** MariaDB local na porta `3306`, utilizando exclusivamente o banco descartável `autobotz_d5_test` e utilizador dedicado (`d5_test`).
- **Compilação Estática:** Todos os fontes (`src/`) e testes (`tests/`) compilados sem erros com `javac -Xlint:all`.

---

## 2. Validação das Suítes Automatizadas e Travas de Segurança

| Componente / Suíte | Tipo de Teste | Critério Validado | Resultado |
| :--- | :--- | :--- | :---: |
| `D5IntegridadeTest` | Positivo (Determinístico) | Validação de integridade estrutural e regras de negócio do D5 | **PASS** |
| `D5RecuperacaoJdbcTest` | Positivo (Integração JDBC) | Persistência e recuperação consistente na base `autobotz_d5_test` | **PASS** |
| `D5RollbackPortalTest` | Positivo (Determinístico) | Garantia de rollback e consistência de estado no portal | **PASS** |
| `scripts/test-d5-backup-ci.sh` (sem variável) | Negativo (Guarda de Segurança) | Bloqueio automático quando `AUTOBOTZ_DB_URL` não está definida | **PASS (Bloqueado)** |
| `scripts/test-d5-backup-ci.sh` (banco não-`_test`) | Negativo (Guarda de Segurança) | Recusa imediata de URLs fora do padrão `autobotz_d5_test` | **PASS (Bloqueado)** |
| `scripts/test-d5-backup-ci.sh` (local sem flag efémera) | Negativo (Guarda de Segurança) | Bloqueio de restauração destrutiva sem `AUTOBOTZ_D5_ALLOW_EPHEMERAL_RESTORE=1` | **PASS (Bloqueado)** |

---

## 3. Auditoria do Pipeline de Integração Contínua (CI)

A inspeção do ficheiro `.github/workflows/integration-regression.yml` e a verificação da execução `#89` (`Integrated ERP Regression`, commit `2fe94f0`) no GitHub Actions confirmaram a integração completa do D5 na esteira oficial:

1. **Provisionamento Isolado:** O *job* cria e migra explicitamente o banco descartável `autobotz_d5_test` no serviço `mariadb:10.11` (`schema.sql` + migrações `011` a `016`).
2. **Suítes Determinísticas:** `D5IntegridadeTest` e `D5RollbackPortalTest` são executados automaticamente na etapa *Run deterministic service and portal suites*.
3. **Backup e Restauração Efémera (`Check D5 MariaDB backup and restore`):** O workflow executa `bash scripts/test-d5-backup-ci.sh` apontando exclusivamente para `jdbc:mariadb://127.0.0.1:3306/autobotz_d5_test?sslMode=disable` com a flag explícita `AUTOBOTZ_D5_ALLOW_EPHEMERAL_RESTORE: '1'`.

---

## 4. Parecer Final

As implementações, travas de segurança contra destruição acidental de dados e suítes de regressão do **D5** encontram-se **aprovadas** na revisão independente e devidamente protegidas pela esteira de CI da branch `main`.
