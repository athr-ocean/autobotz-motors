#!/bin/bash
set -e

# Adiciona o parâmetro para desativar SSL/TLS, conforme exigido pelo driver MariaDB 3.x sem certificados locais
DB_URL_BASE="${AUTOBOTZ_DB_URL:-jdbc:mariadb://127.0.0.1:3306/autobotz_db_test}"
if [[ "$DB_URL_BASE" != *"?sslMode=disable"* ]]; then
  export AUTOBOTZ_DB_URL="${DB_URL_BASE}?sslMode=disable"
fi

echo "=================================================="
echo "Iniciando verificacao e execucao segura de testes"
echo "=================================================="

# 1. Validação do nome do banco de dados
DB_URL_WITHOUT_QUERY="${AUTOBOTZ_DB_URL%%\?*}"
DB_PATH="${DB_URL_WITHOUT_QUERY#*://}"
DB_PATH="${DB_PATH#*/}"
DB_NAME="${DB_PATH##*/}"
if [[ "$DB_NAME" != *_test ]]; then
  echo "ERRO DE SEGURANCA: O banco de dados deve terminar com '_test'!"
  exit 3
fi
echo "Banco de teste: ${DB_NAME}"

# 2. Preparar os artefatos de teste em uma pasta temporária própria
TEST_TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/autobotz-test.XXXXXX")"
trap 'rm -rf "$TEST_TMP_DIR"' EXIT
BUILD_DIR="$TEST_TMP_DIR/bin"
mkdir -p "$BUILD_DIR"

# Usar o driver local, se houver; caso contrário, baixá-lo apenas para a pasta temporária
JDBC_DRIVER="${AUTOBOTZ_JDBC_JAR:-lib/mariadb-java-client-4.0.0.jar}"
if [[ ! -f "$JDBC_DRIVER" ]]; then
  if [[ -n "${AUTOBOTZ_JDBC_JAR:-}" ]]; then
    echo "ERRO: AUTOBOTZ_JDBC_JAR aponta para arquivo inexistente." >&2
    exit 1
  fi
  JDBC_DRIVER="$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar"
fi
if [[ ! -f "$JDBC_DRIVER" ]]; then
  JDBC_DRIVER="$TEST_TMP_DIR/mariadb-java-client-3.5.10.jar"
  echo "Driver JDBC local ausente. Baixando Connector/J apenas para a pasta de teste..."
  curl -fsSL -o "$JDBC_DRIVER" https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar
fi

echo "[1/3] Compilando o projeto unificado em pasta temporaria..."
javac -Xlint:all -cp "$JDBC_DRIVER:lib/*" -sourcepath src -d "$BUILD_DIR" $(find src tests -name "*.java")

# Copiar traducoes para a pasta temporaria de classes
(cd src && find . -name "*.properties" -exec cp --parents {} "$BUILD_DIR"/ \;)

# 4. Execução do CoreRegression
echo "[2/3] Executando CoreRegression..."
java -cp "$BUILD_DIR:src:$JDBC_DRIVER:lib/*" CoreRegression

# 5. Execução do D11ComprasTest
echo "[3/3] Executando D11ComprasTest..."
java -cp "$BUILD_DIR:src:$JDBC_DRIVER:lib/*" D11ComprasTest

echo "=================================================="
echo "SUCCESS: Todos os testes passaram na base isolada!"
echo "=================================================="
