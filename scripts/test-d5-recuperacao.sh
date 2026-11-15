#!/usr/bin/env bash
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$raiz"

url="${AUTOBOTZ_DB_URL:-}"
if [[ ! "$url" =~ ^jdbc:mariadb://(127\.0\.0\.1|localhost):3306/autobotz_d5_test(\?.*)?$ ]]; then
  echo "D5: defina AUTOBOTZ_DB_URL para autobotz_d5_test em localhost:3306." >&2
  exit 2
fi
if [[ -z "${AUTOBOTZ_DB_PASSWORD:-}" ]]; then
  echo "D5: defina AUTOBOTZ_DB_PASSWORD para o banco descartável." >&2
  exit 2
fi

echo "Compilando o ERP..."
bash scripts/run.sh build

mkdir -p build/d5-tests
javac -encoding UTF-8 -Xlint:all -cp "build/classes:lib/*" \
  -d build/d5-tests tests/D5RecuperacaoJdbcTest.java

echo "Testando integridade, falha e recuperação em autobotz_d5_test..."
java -cp "build/d5-tests:build/classes:src:lib/*" D5RecuperacaoJdbcTest
