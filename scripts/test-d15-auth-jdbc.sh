#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

for name in AUTOBOTZ_TEST_DB_URL AUTOBOTZ_TEST_DB_USER AUTOBOTZ_TEST_DB_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Defina %s para executar o teste JDBC em banco descartável.\n' "$name" >&2
    exit 2
  fi
done
if [[ ! "$AUTOBOTZ_TEST_DB_URL" =~ ^jdbc:(mariadb|mysql)://[^/]+/[^/?]*_test([?].*)?$ ]]; then
  printf '%s\n' 'Recusado: a URL JDBC deve apontar para um banco terminado em _test.' >&2
  exit 3
fi

jdbc_jar="${AUTOBOTZ_JDBC_JAR:-$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar}"
if [[ ! -f "$jdbc_jar" ]]; then
  printf '%s\n' 'Driver MariaDB ausente; defina AUTOBOTZ_JDBC_JAR.' >&2
  exit 1
fi

out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
mapfile -d '' java_sources < <(find src -name '*.java' -print0)
javac -encoding UTF-8 -cp "$jdbc_jar" -d "$out" "${java_sources[@]}" tests/D15AuthHttpJdbcTest.java
AUTOBOTZ_DB_URL="$AUTOBOTZ_TEST_DB_URL" \
  AUTOBOTZ_DB_USER="$AUTOBOTZ_TEST_DB_USER" \
  AUTOBOTZ_DB_PASSWORD="$AUTOBOTZ_TEST_DB_PASSWORD" \
  java -cp "$out:src:$jdbc_jar" D15AuthHttpJdbcTest
