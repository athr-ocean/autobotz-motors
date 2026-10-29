# AutoBotz Motors

ERP acadêmico da Equipe B, desenvolvido em Java, JDBC e MariaDB. O frontend desktop atual é Java Swing; a aplicação de console continua disponível.

## Requisitos

- JDK 21 ou superior;
- MariaDB;
- MariaDB Connector/J 3.5.10.

O launcher procura o driver em `~/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar`. Para usar outro local, defina `AUTOBOTZ_JDBC_JAR`.

Configure no ambiente, sem versionar credenciais:

- `AUTOBOTZ_DB_URL`: URL JDBC do banco preparado com `schema.sql`;
- `AUTOBOTZ_DB_USER`: usuário do banco;
- `AUTOBOTZ_DB_PASSWORD`: senha desse usuário.

## Executar

Na raiz do projeto, inicie a aplicação Swing:

```bash
bash scripts/run.sh swing
```

O console permanece disponível:

```bash
bash scripts/run.sh console
```

Para compilar sem iniciar a aplicação:

```bash
bash scripts/run.sh build
```

O launcher não cria o banco, não instala dependências e não carrega arquivos de credenciais. Aplique `schema.sql` somente ao banco pretendido; se ele já contém dados, faça backup antes.

## Módulos

- autenticação, sessão e autorização por perfil;
- cadastro, consulta, atualização e anonimização de clientes;
- cadastro, consulta, atualização e exclusão de veículos conforme as permissões;
- vendas transacionais e histórico de relacionamento com clientes;
- serviços e ordens de serviço com itens, totais e regra de garantia;
- projetos com responsáveis, equipes, status e membros vinculados ao projeto;
- compras D11 com fornecedores, orçamento, solicitação, aprovação transacional e pedido;
- relatórios e consulta de auditoria;
- interface em português do Brasil e inglês dos Estados Unidos.

As permissões são definidas por `AutorizacaoService`. A validação e as limitações conhecidas estão registradas em [docs/VALIDACAO.md](docs/VALIDACAO.md); a documentação de autoria e integração está em [docs/AUTORIA_INTEGRACAO.md](docs/AUTORIA_INTEGRACAO.md).

`src/` é a árvore fonte usada pelo launcher. As classes compiladas são gravadas em `build/classes`, e os recursos são lidos de `src/`. `bin/` e a antiga cópia `autobotz-motors-main/` foram removidas do versionamento por serem redundantes; o launcher utiliza somente `src/`. No Eclipse, configure `M2_REPO` para o repositório Maven local e selecione JDK 21 ou superior.

## D11 — Compras

A tela **Compras** é restrita a `ADMIN` na matriz atual e permite cadastrar fornecedor/orçamento, registrar solicitação, consultar estado, aprovar e gerar pedido ou rejeitar com motivo. Antes de usar o módulo, aplique `db/migrations/011_d11_compras.sql` ao banco de destino selecionado. Para instalação nova, as mesmas tabelas estão no `schema.sql`.

Para o teste de aceite JDBC, configure `AUTOBOTZ_DB_URL` para um banco descartável terminado em `_test`, `AUTOBOTZ_DB_USER`, `AUTOBOTZ_DB_PASSWORD` e, se necessário, `AUTOBOTZ_JDBC_JAR`. Depois execute `bash scripts/test-d11-compras.sh`. O script recusa destinos fora de `_test` e remove apenas registros criados pelo próprio teste. O mapa do fluxo, riscos e evidências esperadas está em [docs/D11_COMPRAS_RELATORIO.md](docs/D11_COMPRAS_RELATORIO.md).

A divergência acadêmica sobre a exigência de Swing no D6 deve ser confirmada com o professor antes da entrega.


## Proteção de dados pessoais — D14

Os campos pessoais dos clientes são cifrados na camada de persistência. Antes de usar o cadastro, configure uma chave persistente fora do repositório e mantenha a mesma chave para ler os dados já gravados.

Em instalação nova, `schema.sql` inclui as colunas necessárias. Em banco existente, aplique `db/migrations/016_d14_client_pii.sql` depois das migrations 014 e 015. A conversão dos registros antigos é uma etapa separada e explícita; ela não roda na inicialização do ERP.

O guia [D14 — Criptografia e migração de dados pessoais](docs/D14_CIFRAGEM_DADOS_PESSOAIS.md) explica a configuração e o backup da chave, a preparação do banco e a execução segura da migração e dos testes.

## Rodada 2 — D12 a D15

O simulador **Planos de Assinatura** está disponível na interface Swing para ADMIN e VENDEDOR. É um cálculo local com preços e módulos editáveis e distribuição 30%/50%/20%. **Não altera o banco de dados**. Veja [D12 — análise, simulação e comunicação](docs/D12_PLANOS_SIMULACAO.md). O teste `D12SimuladorUITest` verifica a criação do painel em modo headless, os totais e entradas inválidas.

Relatórios acadêmicos: [D11 — qualidade e sustentabilidade](docs/D11_QUALIDADE_SUSTENTABILIDADE.md), [D13 — contratos](docs/D13_CONTRATOS_LEDGER.md), [D14 — segurança](docs/D14_CIFRAGEM_DADOS_PESSOAIS.md), [D15 — tickets/pesquisa simulada](docs/D15_ANALISE_TICKETS_PESQUISA.md), [relatório geral](RELATORIO_GERAL_RODADA2.md) e [checklist](CHECKLIST_FINAL.md).

Não confunda dados sintéticos de D12/D15, API/ledger simulado de D13 ou MFA demonstrativo com dados reais ou certificação de produção.
