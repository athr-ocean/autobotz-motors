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

## Terceira rodada (D1–D5)

A terceira rodada reúne desafios sobre responsabilidade em decisões automatizadas (D1), integração governamental simulada (D2), concorrência (D3), usabilidade do portal (D4) e recuperação de falhas (D5). O material do D1 está documentado em [análise de responsabilidade algorítmica](entrega/rodada3/D1_RESPONSABILIDADE_ALGORITMICA.md) e [política de explicabilidade](entrega/rodada3/D1_POLITICA_EXPLICABILIDADE.md). Cada frente tem seu próprio desenvolvimento; um trabalho em pull request ainda não faz parte da `main`.

No D5, foram implementadas a conferência de logs com SHA-256, a recuperação de registros em MariaDB de teste e a simulação de uma atualização defeituosa do portal com retorno aos arquivos estáveis. O código entrou na `main` pelos [PRs #47](https://github.com/athr-ocean/autobotz-motors/pull/47) e [#48](https://github.com/athr-ocean/autobotz-motors/pull/48). O [plano de recuperação](entrega/rodada3/D5_PLANO_RECUPERACAO.md) descreve os testes e o procedimento proposto para incidentes. As duas cópias de logs usadas na demonstração são locais: não há replicação entre regiões nem failover em nuvem implementado.

O cliente GovConnect demonstrativo do D2 foi integrado pelo [PR #53](https://github.com/athr-ocean/autobotz-motors/pull/53). A navegação responsiva do D4 entrou pelo [PR #49](https://github.com/athr-ocean/autobotz-motors/pull/49). A documentação complementar do D2 segue no [PR #50](https://github.com/athr-ocean/autobotz-motors/pull/50), aguardando alinhamento. O plano de usabilidade do D4 ainda depende da execução e do registro de testes com participantes.

## Estrutura do repositório

```text
.github/               Workflows e modelo de pull request
src/                   Código Java e arquivos de idioma
tests/                 Testes automatizados
scripts/               Compilação, execução e verificações
db/migrations/         Migrações de banco de dados
schema.sql             Esquema para instalação nova
portal-treinamento/    Interface web e materiais publicados
lib/                   Driver JDBC incluído no projeto
docs/                  Planos, orientações e evidências técnicas
entrega/               Relatórios e documentos da disciplina
```

## Requisitos

- **Java JDK 21** ou superior (inclui `javac` e `java`);
- **MariaDB** com banco e usuário de acesso configurados;
- Bash (Linux ou Git Bash no Windows);
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

Para configurar a chave PII, consulte [criptografia e migração](entrega/D14_CIFRAGEM_DADOS_PESSOAIS.md). Nunca versionar senhas, chaves, backups ou bancos com informações pessoais.

## Primeiro administrador

O cadastro de usuários exige uma sessão de administrador. Em um banco novo e vazio, crie a primeira conta em um terminal interativo, depois de configurar as variáveis JDBC:

```bash
bash scripts/run.sh build
AUTOBOTZ_ADMIN_SETUP=1 java -cp "build/classes:src:lib/*" autobotz.BootstrapAdmin admin
```

O comando solicita a senha sem exibi-la e recusa bancos que já possuam usuários. Após entrar no aplicativo, o administrador pode cadastrar outras contas pelo menu lateral. Não mantenha `AUTOBOTZ_ADMIN_SETUP` habilitado durante a execução normal.

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
python3 scripts/check-doc-links.py
```

A regressão integrada está em [`.github/workflows/integration-regression.yml`](.github/workflows/integration-regression.yml), com MariaDB descartável. Ela também inclui testes de recuperação JDBC e de backup/restauração do D5. Para repetir esses testes localmente, consulte os pré-requisitos do [plano do D5](entrega/rodada3/D5_PLANO_RECUPERACAO.md): as rotinas de banco só devem ser usadas em bases descartáveis. Não execute testes destrutivos sobre `autobotz_db` ou dados reais.

Os resultados atuais da regressão integrada, D12 e D14 estão na aba [Actions](https://github.com/athr-ocean/autobotz-motors/actions). Os testes automatizados não substituem a inspeção visual do portal nem certificam o sistema para produção.

## Documentos essenciais

- [Índice dos documentos de entrega](entrega/README.md)
- [Documentação técnica e evidências](docs/README.md)
- [Relatório geral da segunda rodada](entrega/RELATORIO_GERAL_RODADA2.md)
- [Matriz de aceite do professor: requisitos e evidências D11–D15](entrega/MATRIZ_ACEITE_PROFESSOR.md)
- [Checklist de entregas e limites](entrega/CHECKLIST_FINAL.md)
- [D11 — compras e qualidade](entrega/D11_COMPRAS_RELATORIO.md)
- [D12 — assinaturas e simulação](entrega/D12_PLANOS_SIMULACAO.md)
- [D13 — contratos e integridade](entrega/D13_CONTRATOS_LEDGER.md)
- [D14 — autenticação, MFA e proteção de dados](entrega/D14_LOGIN_MFA.md)
- [D15 — portal de treinamento](entrega/D15_PORTAL.md)
- [D15 — tickets e pesquisa simulada](entrega/D15_ANALISE_TICKETS_PESQUISA.md)
- [D1 (3ª rodada) — responsabilidade algorítmica e auditoria](entrega/rodada3/D1_RESPONSABILIDADE_ALGORITMICA.md)
- [D1 (3ª rodada) — política de explicabilidade](entrega/rodada3/D1_POLITICA_EXPLICABILIDADE.md)
- [D5 (3ª rodada) — integridade dos logs e recuperação após falhas](entrega/rodada3/D5_PLANO_RECUPERACAO.md)

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
