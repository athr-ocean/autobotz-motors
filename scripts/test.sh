#!/bin/bash
set -e

# Adiciona o parâmetro para desativar SSL/TLS, conforme exigido pelo driver MariaDB 3.x sem certificados locais
DB_URL_BASE="${AUTOBOTZ_DB_URL:-jdbc:mariadb://127.0.0.1:3306/autobotz_db_test}"
if [[ "$DB_URL_BASE" != *"?sslMode=disable"* ]]; then
  export AUTOBOTZ_DB_URL="${DB_URL_BASE}?sslMode=disable"
fi

echo "=================================================="
echo "Iniciando verificacao e execucao segura de testes"
echo "URL do Banco: ${AUTOBOTZ_DB_URL}"
echo "Utilizador:  ${AUTOBOTZ_DB_USER:-root}"
echo "=================================================="

# 1. Validação de segurança do banco de dados
if [[ "$AUTOBOTZ_DB_URL" != *"_test"* ]]; then
  echo "ERRO DE SEGURANCA: O banco de dados deve terminar com '_test'!"
  exit 3
fi

# 2. Garantir pasta lib/ e descarregar a versão exata do driver JDBC exigida pelo D15 (3.5.10)
mkdir -p lib
if ! ls lib/mariadb-java-client-3.5.10.jar 1> /dev/null 2>&1; then
  echo "Driver MariaDB 3.5.10 nao encontrado. Descarregando driver JDBC..."
  # Limpa versões antigas para evitar conflitos no classpath
  rm -f lib/mariadb-java-client*.jar
  curl -sSL -o lib/mariadb-java-client-3.5.10.jar https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar || true
fi

# 3. Criar pasta bin e limpar ficheiros compilados antigos
echo "[1/3] Limpando e compilando o projeto unificado..."
mkdir -p bin
rm -rf bin/*

# Compilação de todas as classes Java
javac -Xlint:all -cp "lib/*" -sourcepath src -d bin $(find src tests -name "*.java")

# Copiar ficheiros .properties para bin (para resolver o erro I18nUtils MissingResourceException)
(cd src && find . -name "*.properties" -exec cp --parents {} ../bin/ \;) 2>/dev/null || true

# 4. Execução do CoreRegression
echo "[2/3] Executando CoreRegression..."
java -cp "bin:src:lib/*" CoreRegression

# 5. Execução do D11ComprasTest
echo "[3/3] Executando D11ComprasTest..."
java -cp "bin:src:lib/*" D11ComprasTest

echo "=================================================="
echo "SUCCESS: Todos os testes passaram na base isolada!"
echo "=================================================="