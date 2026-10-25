# Banco limpo e legado: D9

Banco novo: `schema.sql` cria `membros_projeto.id_projeto` obrigatório, com FK para projetos e exclusão em cascata.

Banco legado: o trecho de compatibilidade do schema adiciona a coluna, mas não consegue descobrir a qual projeto cada lista antiga pertence. Não se deve atribuir todas as listas a um projeto arbitrário.

A ferramenta `autobotz.util.MigracaoMembrosProjeto` verifica os vínculos sem alterar dados por padrão. Após backup e associação explícita dos membros antigos aos projetos corretos, `--apply` torna a coluna obrigatória e adiciona a FK ausente. A execução repetida foi testada. Havendo membro sem projeto válido, a ferramenta recusa a migração.

Depois de `bash scripts/run.sh build`, com as variáveis JDBC já definidas:

```bash
java -cp "build/classes:src:${AUTOBOTZ_JDBC_JAR:-$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar}" autobotz.util.MigracaoMembrosProjeto
```

Acrescente `--apply` somente ao migrar deliberadamente um banco com backup. DDL MariaDB pode fazer commit implícito. Nenhuma migração foi aplicada ao banco real de Arthur durante esta intervenção.
