#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

url="${AUTOBOTZ_DB_URL:-}"
if [[ ! "$url" =~ autobotz_.*_test ]]; then
  printf '%s\n' 'Recusado: defina AUTOBOTZ_DB_URL para um banco autobotz_*_test.' >&2
  exit 2
fi
jdbc_jar="${AUTOBOTZ_JDBC_JAR:-$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar}"
if [[ ! -f "$jdbc_jar" ]]; then
  printf '%s\n' 'Driver MariaDB ausente. Defina AUTOBOTZ_JDBC_JAR com o JAR do Connector/J.' >&2
  exit 2
fi

# Usa somente um diretório temporário criado por este teste; não apaga bin/, JARs ou build preexistente.
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/autobotz-d11-compras.XXXXXX")"
trap 'rm -rf -- "$tmp_dir"' EXIT
mkdir -p "$tmp_dir/classes" "$tmp_dir/tests"
mapfile -d '' source_files < <(find src/autobotz -name '*.java' -print0)
javac -Xlint:all -encoding UTF-8 -d "$tmp_dir/classes" "${source_files[@]}"
javac -Xlint:all -encoding UTF-8 -cp "$tmp_dir/classes:$jdbc_jar" \
  -d "$tmp_dir/tests" tests/D11ComprasPermissaoTest.java tests/D11ComprasJdbcTest.java
java -cp "$tmp_dir/classes:$tmp_dir/tests:src:$jdbc_jar" D11ComprasPermissaoTest
java -cp "$tmp_dir/classes:$tmp_dir/tests:src:$jdbc_jar" D11ComprasJdbcTest
