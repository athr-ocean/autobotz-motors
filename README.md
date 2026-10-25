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
- relatórios e consulta de auditoria;
- interface em português do Brasil e inglês dos Estados Unidos.

As permissões são definidas por `AutorizacaoService`. A validação e as limitações conhecidas estão registradas em [docs/VALIDACAO.md](docs/VALIDACAO.md); a documentação de autoria e integração está em [docs/AUTORIA_INTEGRACAO.md](docs/AUTORIA_INTEGRACAO.md).

`src/` é a árvore fonte usada pelo launcher. As classes compiladas são gravadas em `build/classes`, e os recursos são lidos de `src/`. `bin/` e `autobotz-motors-main/` são árvores antigas rastreadas e não são usadas por esse launcher. No Eclipse, configure `M2_REPO` para o repositório Maven local e selecione JDK 21 ou superior.

A divergência acadêmica sobre a exigência de Swing no D6 deve ser confirmada com o professor antes da entrega.
