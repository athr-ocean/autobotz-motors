#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "\${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

for command_name in java javac; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Erro: %s não foi encontrado. Instale o JDK 21 ou superior.\n' "$command_name" >&2
    exit 1
  fi
done

classes_dir="$(mktemp -d "\${TMPDIR:-/tmp}/autobotz-d2-govconnect.XXXXXX")"
trap 'rm -rf "$classes_dir"' EXIT

printf '%s\n' 'Compilando o teste D2 (JDK 21+, sem banco de dados)...'
javac --release 21 --add-modules jdk.httpserver -encoding UTF-8 \
  -sourcepath src -d "$classes_dir" \
  src/autobotz/model/Fornecedor.java \
  src/autobotz/model/PedidoCompra.java \
  src/autobotz/integracao/govconnect/GovConnectIntegracao.java \
  src/autobotz/integracao/govconnect/OAuth2ClientCredentialsTokenProvider.java \
  tests/GovConnectIntegracaoTest.java

printf '\n%s\n' 'Executando os testes locais do GovConnect...'
java --add-modules jdk.httpserver -Dfile.encoding=UTF-8 -ea \
  -cp "$classes_dir" GovConnectIntegracaoTest
