# Migrações do AutoBotz Motors

As migrações não selecionam nem criam um banco. Escolha explicitamente a base e aplique os arquivos na ordem abaixo. A instalação nova usa `schema.sql`; bancos existentes recebem as migrações sem importar esse schema sobre dados existentes.

| Versão | Desafio | Dependência principal |
|---|---|---|
| 011 | D11 — fornecedores e compras | tabelas-base de clientes/ERP |
| 012 | D12 — planos e assinaturas | `clientes` |
| 013 | D13 — contratos e ledger | `fornecedores(id)` da 011 |
| 014 | D14 — autenticação, MFA e eventos de segurança | `usuarios` |
| 015 | D15 — eventos de treinamento | nenhuma tabela D11–D14 |
| 016 | D14 — colunas cifradas e índice HMAC de CPF | `clientes`; não cifra linhas existentes |

Exemplo de atualização, executado de dentro do repositório e com o banco correto já selecionado:

```sh
for migration in db/migrations/0{11,12,13,14,15,16}_*.sql; do
  mariadb "$AUTOBOTZ_DB_NAME" < "$migration"
done
```

Para instalação nova, crie/seleciona-se a base fora do `schema.sql` e importa-se o schema completo. Em seguida, a sequência acima pode ser reaplicada: as definições `CREATE TABLE IF NOT EXISTS`, alterações condicionais e índice repetível foram testados em MariaDB 13.0.2.

```sh
mariadb -e 'CREATE DATABASE autobotz_db'
mariadb autobotz_db < schema.sql
```

Não use os exemplos em banco com dados reais durante validação. Os testes de instalação, upgrade e JDBC desta entrega foram executados em bancos com sufixo `_test`, usuário fictício e dados descartáveis. A migração 016 só amplia/adiciona estrutura. A conversão dos dados legados é uma ação separada e deliberada: primeiro `MigracaoDadosPessoais --check`, depois `--apply --confirm-db=NOME_EXATO`, com a chave já administrada pelo responsável do ambiente. A chave nunca deve ser copiada para este repositório.

## Evidência de integração

Em MariaDB 13.0.2, a candidata integrada passou por:

- importação de `schema.sql` em banco descartável vazio e aplicação de 011–016;
- criação de banco a partir do `schema.sql` de `origin/main` e upgrade 011–016;
- reaplicação de 011–016 sem erro;
- testes JDBC D11, D12, D13, D14 de autenticação e D15 de persistência/autorização.

Essa evidência não é benchmark, não contém dados reais e não substitui revisão independente da migration PII nem regressão de todos os fluxos D6–D10.
