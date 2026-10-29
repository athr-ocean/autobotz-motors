#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

jdbc_jar="${AUTOBOTZ_JDBC_JAR:-$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar}"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

mapfile -d '' portal_sources < <(find src/autobotz/portal -name '*.java' -print0)
mapfile -d '' test_sources < <(find tests -maxdepth 1 -type f \( -name 'D15PortalTest.java' -o -name 'D15ConteudoTest.java' -o -name 'D15AtalhoTest.java' -o -name 'D15PersistenciaTest.java' -o -name 'D14SegurancaTest.java' -o -name 'D14JdbcIntegrationTest.java' \) -print0)

printf '%s\n' '== Compilando portal e testes D15 =='
javac -encoding UTF-8 -sourcepath src -d "$out" "${portal_sources[@]}" "${test_sources[@]}"

cp="$out:src"
if [[ -f "$jdbc_jar" ]]; then
  cp="$cp:$jdbc_jar"
fi

printf '%s\n' '== D15PortalTest =='
java -cp "$cp" D15PortalTest
printf '%s\n' '== D15ConteudoTest =='
java -cp "$cp" D15ConteudoTest
printf '%s\n' '== D15AtalhoTest =='
java -cp "$cp" D15AtalhoTest

if [[ -n "${AUTOBOTZ_TEST_DB_URL:-}" && -n "${AUTOBOTZ_TEST_DB_USER:-}" && -n "${AUTOBOTZ_TEST_DB_PASSWORD:-}" ]]; then
  if [[ ! -f "$jdbc_jar" ]]; then
    printf '%s\n' 'ERRO: o teste de persistência precisa do MariaDB Connector/J. Defina AUTOBOTZ_JDBC_JAR.' >&2
    exit 1
  fi
  printf '%s\n' '== D15PersistenciaTest (banco isolado _test) =='
  AUTOBOTZ_DB_URL="$AUTOBOTZ_TEST_DB_URL" \
    AUTOBOTZ_DB_USER="$AUTOBOTZ_TEST_DB_USER" \
    AUTOBOTZ_DB_PASSWORD="$AUTOBOTZ_TEST_DB_PASSWORD" \
    java -cp "$cp" D15PersistenciaTest
  printf '%s\n' '== D14JdbcIntegrationTest (portal HTTP real) =='
  AUTOBOTZ_DB_URL="$AUTOBOTZ_TEST_DB_URL" \
    AUTOBOTZ_DB_USER="$AUTOBOTZ_TEST_DB_USER" \
    AUTOBOTZ_DB_PASSWORD="$AUTOBOTZ_TEST_DB_PASSWORD" \
    java -cp "$cp" D14JdbcIntegrationTest
else
  printf '%s\n' 'PULADO: D15PersistenciaTest requer AUTOBOTZ_TEST_DB_URL, AUTOBOTZ_TEST_DB_USER e AUTOBOTZ_TEST_DB_PASSWORD.'
fi

printf '%s\n' 'Testes D15 sem banco concluídos. Persistência só fica validada se D15PersistenciaTest for executado e passar.'
