#!/bin/bash
set -e

echo "=================================================="
echo "Iniciando verificacao e execucao segura de testes"
echo "URL do Banco: ${AUTOBOTZ_DB_URL:-jdbc:mariadb://127.0.0.1:3306/autobotz_db_test}"
echo "Utilizador:  ${AUTOBOTZ_DB_USER:-root}"
echo "=================================================="

# 1. Validação de segurança do banco de dados
DB_URL="${AUTOBOTZ_DB_URL:-jdbc:mariadb://127.0.0.1:3306/autobotz_db_test}"
if [[ "$DB_URL" != *"_test"* ]]; then
  echo "ERRO DE SEGURANCA: O banco de dados deve terminar com '_test'!"
  exit 3
fi

# 2. Criar pasta bin e limpar ficheiros compilados antigos
echo "[1/3] Limpando e compilando o projeto unificado..."
mkdir -p bin
rm -rf bin/*

# Compilação de todas as classes Java
javac -Xlint:all -cp "lib/*" -sourcepath src -d bin $(find src tests -name "*.java")

# Copiar ficheiros .properties mantendo a estrutura de diretórios para a pasta bin
(cd src && find . -name "*.properties" -exec cp --parents {} ../bin/ \;) 2>/dev/null || true

# 3. Execução do CoreRegression (incluindo bin, src e lib no classpath)
echo "[2/3] Executando CoreRegression..."
java -cp "bin:src:lib/*" CoreRegression

# 4. Execução do D11ComprasTest
echo "[3/3] Executando D11ComprasTest..."
java -cp "bin:src:lib/*" D11ComprasTest

echo "=================================================="
echo "SUCCESS: Todos os testes passaram na base isolada!"
echo "=================================================="