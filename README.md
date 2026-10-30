# AutoBotz Motors

ERP acadêmico da **Equipe B — Liga do Mal**, desenvolvido em Java 21, Swing, JDBC e MariaDB na disciplina Administração para Computação (2026.2). O projeto começou com operações de veículos e oficina (D6–D10) e foi ampliado com compras, assinaturas, contratos, segurança e treinamento (D11–D15).

O código-fonte da entrega está na branch `main`. O sistema é um **protótipo acadêmico**, não uma solução certificada para produção.

## Funcionalidades

| Área | Implementação |
| --- | --- |
| Operação (D6–D10) | Clientes, veículos, vendas com transação e controle de estoque, ordens de serviço, garantia, CRM, projetos, relatórios e auditoria |
| Idiomas e perfis | Interface PT-BR/EN-US, login e autorização ADMIN/VENDEDOR |
| Compras (D11) | Fornecedores, orçamentos, solicitações, aprovação/rejeição, geração de pedido e rollback |
| Assinaturas (D12) | Planos e módulos opcionais, persistência, churn, MRR e simulador de migração 30/50/20 no Swing |
| Contratos (D13) | Contratos de fornecedores, alertas, ledger local de hashes SHA-256 e API didática |
| Segurança (D14) | Bloqueio por tentativas, MFA demonstrativo, sessões HTTP, eventos, AES-GCM/HMAC para dados pessoais |
| Treinamento (D15) | Portal web local, catálogo e FAQ, eventos persistidos e métricas com autorização administrativa |

O simulador D12 utiliza dados fictícios e não modifica contratos reais. O ledger de D13 não é uma blockchain distribuída; o MFA é didático. Não foram comprovadas economias reais de energia nem redução de chamados.

## Estrutura do repositório

```text
src/autobotz/          Código Java: modelos, DAOs, serviços, Swing, console e portal
src/messages_*.properties   Recursos de internacionalização
portal-treinamento/    Página web e materiais publicados
db/migrations/         Migrações SQL 011 a 016
schema.sql             Esquema SQL para instalação nova
scripts/               Execução e testes
tests/                 Suítes de regressão e integração
lib/                   Driver MariaDB Connector/J versionado
docs/                  Guias técnicos, relatórios e documentação dos desafios
```

## Requisitos

- **Java JDK 21** ou superior (inclui `javac` e `java`);
- **MariaDB** com banco e usuário de acesso configurados;
- Bash (Linux);
- Driver MariaDB Connector/J: o launcher usa `AUTOBOTZ_JDBC_JAR`, se definido; caso contrário, utiliza o JAR incluído em `lib/` e, como alternativa, a dependência local Maven.

Não é necessário Maven, Gradle, Eclipse ou VS Code para compilar pelo launcher. Arquivos gerados de IDE e bytecode não fazem parte da árvore versionada.

## Preparar o banco

A instalação nova requer um **banco previamente criado**, com usuário e senha definidos pelo responsável. O arquivo `schema.sql` apenas define as tabelas; **não cria nem seleciona automaticamente o banco**.

Exemplo para um banco de desenvolvimento vazio já criado:

```bash
mariadb -u SEU_USUARIO -p -h 127.0.0.1 autobotz_dev < schema.sql
```

Em um banco existente, **não importe o esquema completo sobre dados reais**. Leia [a ordem e as condições das migrations](db/migrations/README.md) antes de aplicar `011` a `016`. A migration 016 adiciona estrutura de PII, mas a cifragem de dados legados exige uma operação separada e explícita.

Configure as seguintes variáveis na sessão de execução, sem publicar valores secretos no Git:

| Variável | Finalidade |
| --- | --- |
| `AUTOBOTZ_DB_URL` | URL JDBC, por exemplo `jdbc:mariadb://127.0.0.1:3306/autobotz_dev` |
| `AUTOBOTZ_DB_USER` | Usuário do MariaDB com permissões no banco escolhido |
| `AUTOBOTZ_DB_PASSWORD` | Senha; obrigatória e não vazia |
| `AUTOBOTZ_PII_KEY` | Chave externa de criptografia; mantenha a mesma chave para reler dados cifrados |
| `AUTOBOTZ_JDBC_JAR` | Opcional, para usar outro MariaDB Connector/J |

Para configurar a chave PII, consulte [criptografia e migração](docs/D14_CIFRAGEM_DADOS_PESSOAIS.md). Nunca versionar senhas, chaves, backups ou bancos com informações pessoais.

## Executar

Na raiz do repositório, com Java, banco e variáveis de ambiente preparados:

```bash
bash scripts/run.sh build       # Compilar
bash scripts/run.sh swing       # Interface desktop (padrão)
bash scripts/run.sh console     # Menu em terminal
bash scripts/run.sh portal      # Portal de treinamento local
```

O portal é servido somente em **http://127.0.0.1:8085** por padrão. É possível definir outra porta com `AUTOBOTZ_PORTAL_PORT`. O atalho no Swing abre o portal sob demanda; não existe hospedagem pública nem SSO automático entre Swing e navegador.

## Testes

```bash
bash scripts/run.sh build
bash scripts/test-d15.sh
```

A regressão integrada de D6–D15 está em [`.github/workflows/integration-regression.yml`](.github/workflows/integration-regression.yml), com banco MariaDB descartável. Os testes JDBC locais precisam de uma URL que aponte **exclusivamente para banco de teste**; confira os pré-requisitos de cada script antes de executá-lo. Nunca rode suítes destrutivas sobre `autobotz_db` ou dados reais.

**Evidência de fechamento:** em 29/10/2026, o ZIP correspondente ao commit `fa580cde` compilou e passou em **11/11 suítes offline** no CachyOS. A regressão JDBC de **8/8 suítes** foi executada na integração anterior; a PR #39 passou no GitHub Actions com testes JDBC após a inclusão do simulador. O ZIP final da segunda rodada foi verificado quanto à extração e integridade. Isso não substitui teste visual completo ou auditoria de segurança independente.

## Documentos essenciais

- [Relatório geral da segunda rodada](RELATORIO_GERAL_RODADA2.md)
- [Matriz de aceite do professor: requisitos e evidências D11–D15](docs/MATRIZ_ACEITE_PROFESSOR.md)
- [Checklist de entregas e limites](CHECKLIST_FINAL.md)
- [Roteiro de apresentação individual](ROTEIRO_VIDEO.md)
- [D11 — compras e qualidade](docs/D11_COMPRAS_RELATORIO.md)
- [D12 — assinaturas e simulação](docs/D12_PLANOS_SIMULACAO.md)
- [D13 — contratos e integridade](docs/D13_CONTRATOS_LEDGER.md)
- [D14 — autenticação, MFA e proteção de dados](docs/D14_LOGIN_MFA.md)
- [D15 — portal de treinamento](docs/D15_PORTAL.md)
- [D15 — tickets e pesquisa simulada](docs/D15_ANALISE_TICKETS_PESQUISA.md)

## Equipe e histórico

| Frente da segunda rodada | Responsáveis |
| --- | --- |
| D11 | Alexandre (compras), Adryan (testes e CI) |
| D12 | André (assinaturas) |
| D13 | Gabriel (contratos) |
| D14 | João (autenticação e MFA), Arthur (dados pessoais e integração) |
| D15 | Mauro (portal), Higor (conteúdo e métricas), Arthur (integração) |

A atribuição das frentes descreve a divisão interna; a autoria efetiva de cada arquivo está no histórico do Git. A integração foi realizada por pull requests sem reescrever commits dos colaboradores.

**Limites da entrega:** valores de churn, tickets e pesquisa são simulados; pagamentos e blockchain de D13 também. O trabalho não comprova redução medida de tempo manual, consumo energético ou tickets, e não inclui webinar ou fórum de produção.
