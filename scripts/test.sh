#!/usr/bin/env bash
# ==============================================================================
# Script de Execucao Segura de Testes - AutoBotz Motors (Adryan / D11)
# ==============================================================================

set -e

# 1. Configuracao da URL e Credenciais do Banco de Teste (EXPORT OBRIGATORIO)
export AUTOBOTZ_DB_URL="${AUTOBOTZ_DB_URL:-jdbc:mariadb://localhost:3308/autobotz_db_test}"
export AUTOBOTZ_DB_USER="${AUTOBOTZ_DB_USER:-root}"
# Coloque a sua palavra-passe do MariaDB entre as aspas (deixe "" se nao usar palavra-passe)
export AUTOBOTZ_DB_PASSWORD="${AUTOBOTZ_DB_PASSWORD:-root}"

echo "=================================================="
echo "Iniciando verificacao e execucao segura de testes"
echo "URL do Banco: $AUTOBOTZ_DB_URL"
echo "Utilizador:   $AUTOBOTZ_DB_USER"
echo "=================================================="

# 2. VALIDACAO OBRIGATORIA
if [[ ! "$AUTOBOTZ_DB_URL" =~ _test$ && ! "$AUTOBOTZ_DB_URL" =~ _test\? ]]; then
    echo "ERROR: EXECUCAO ABORTADA POR SEGURANCA!"
    echo "A URL do banco de dados deve obrigatoriamente terminar com '_test'."
    exit 1
fi

export AUTOBOTZ_JDBC_JAR="lib/mariadb-java-client-4.0.0.jar"

# 3. Compilacao Unificada e Limpa
echo "[1/3] Limpando e compilando o projeto unificado..."
rm -rf bin/*
mkdir -p bin
javac -cp "lib/*" -d bin $(find src tests -name "*.java")

# Copiar ficheiros de traducao (.properties) para o classpath (bin/)
cp src/*.properties bin/ 2>/dev/null || true

# 4. Execucao da Regressao Antiga
echo "[2/3] Executando CoreRegression..."
java -cp "bin;lib/*" CoreRegression

# 5. Execucao dos Testes do D11
if [ -f "bin/D11ComprasTest.class" ]; then
    echo "[3/3] Executando D11ComprasTest..."
    java -cp "bin;lib/*" D11ComprasTest
else
    echo "[3/3] D11ComprasTest ainda nao compilado ou em construcao."
fi

echo "=================================================="
echo "SUCCESS: Todos os testes passaram na base isolada!"
echo "=================================================="