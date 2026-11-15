#!/usr/bin/env bash
set -euo pipefail

# Este ensaio altera somente duas bases criadas no serviço MariaDB do CI.
if [[ "${GITHUB_ACTIONS:-}" != "true" || "${AUTOBOTZ_D5_ALLOW_EPHEMERAL_RESTORE:-}" != "1" ]]; then
  echo "D5: este teste completo de backup só pode rodar no ambiente descartável do CI." >&2
  exit 2
fi
if [[ "${AUTOBOTZ_DB_URL:-}" != "jdbc:mariadb://127.0.0.1:3306/autobotz_d5_test?sslMode=disable" ]]; then
  echo "D5: o banco de origem deve ser autobotz_d5_test em localhost." >&2
  exit 2
fi
if [[ -z "${AUTOBOTZ_DB_PASSWORD:-}" ]]; then
  echo "D5: credenciais do banco de teste ausentes." >&2
  exit 2
fi

if command -v mariadb-dump >/dev/null 2>&1; then
  dump_cmd="mariadb-dump"
elif command -v mysqldump >/dev/null 2>&1; then
  dump_cmd="mysqldump"
else
  echo "D5: utilitário mariadb-dump ou mysqldump não encontrado." >&2
  exit 1
fi

export MYSQL_PWD="$AUTOBOTZ_DB_PASSWORD"
usuario="${AUTOBOTZ_DB_USER:-root}"
mysql_args=(-h 127.0.0.1 -P 3306 -u "$usuario")
origem="autobotz_d5_test"
destino="autobotz_d5_restore_test"
temporario="$(mktemp -d)"
tabela_criada=0
banco_criado=0

limpar() {
  if (( tabela_criada )); then
    mysql "${mysql_args[@]}" "$origem" -e "DROP TABLE IF EXISTS d5_restore_exemplo" >/dev/null 2>&1 || true
  fi
  if (( banco_criado )); then
    mysql "${mysql_args[@]}" -e "DROP DATABASE IF EXISTS autobotz_d5_restore_test" >/dev/null 2>&1 || true
  fi
  rm -rf -- "$temporario"
}
trap limpar EXIT

tabela="$(mysql "${mysql_args[@]}" -Nse \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='autobotz_d5_test' AND table_name='logs_auditoria'")"
if [[ "$tabela" != "1" ]]; then
  echo "D5: a base descartável não possui o esquema esperado." >&2
  exit 1
fi

existe="$(mysql "${mysql_args[@]}" -Nse \
  "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='autobotz_d5_restore_test'")"
if [[ "$existe" != "0" ]]; then
  echo "D5: banco de recuperação já existe; nada será sobrescrito." >&2
  exit 1
fi

mysql "${mysql_args[@]}" "$origem" -e \
  "CREATE TABLE d5_restore_exemplo (id INT PRIMARY KEY, descricao VARCHAR(60) NOT NULL) ENGINE=InnoDB"
tabela_criada=1
mysql "${mysql_args[@]}" "$origem" -e \
  "INSERT INTO d5_restore_exemplo VALUES (1,'registro_inicial'), (2,'registro_intacto')"

"$dump_cmd" "${mysql_args[@]}" --single-transaction --quick --no-tablespaces \
  "$origem" > "$temporario/backup.sql"
test -s "$temporario/backup.sql"
sha256sum "$temporario/backup.sql" > "$temporario/backup.sha256"
(cd "$temporario" && sha256sum --check --status backup.sha256)

mysql "${mysql_args[@]}" "$origem" -e \
  "UPDATE d5_restore_exemplo SET descricao='alterado_indevidamente' WHERE id=2"

mysql "${mysql_args[@]}" -e \
  "CREATE DATABASE autobotz_d5_restore_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
banco_criado=1
mysql "${mysql_args[@]}" "$destino" < "$temporario/backup.sql"

alterado="$(mysql "${mysql_args[@]}" "$origem" -Nse \
  "SELECT descricao FROM d5_restore_exemplo WHERE id=2")"
recuperado="$(mysql "${mysql_args[@]}" "$destino" -Nse \
  "SELECT descricao FROM d5_restore_exemplo WHERE id=2")"
quantidade="$(mysql "${mysql_args[@]}" "$destino" -Nse \
  "SELECT COUNT(*) FROM d5_restore_exemplo")"
auditoria="$(mysql "${mysql_args[@]}" -Nse \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='autobotz_d5_restore_test' AND table_name='logs_auditoria'")"

if [[ "$alterado" != "alterado_indevidamente" || "$recuperado" != "registro_intacto" ||
      "$quantidade" != "2" || "$auditoria" != "1" ]]; then
  echo "D5: a cópia restaurada não corresponde aos dados de origem." >&2
  exit 1
fi
echo "D5_BACKUP_MARIADB_OK: backup SHA-256, falha de atualização e restauração em nova base."
