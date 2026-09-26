#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
jdbc_jar="${AUTOBOTZ_JDBC_JAR:-$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar}"
if [[ ! -f "$jdbc_jar" ]]; then
  printf '%s\n' 'Driver JDBC ausente. Defina AUTOBOTZ_JDBC_JAR com o caminho do MariaDB Connector/J 3.5.10.' >&2
  exit 1
fi
mkdir -p build/classes
mapfile -d '' source_files < <(find src/autobotz -name '*.java' -print0)
javac -encoding UTF-8 -cp "$jdbc_jar" -d build/classes "${source_files[@]}"
if [[ "${1:-swing}" == build ]]; then exit 0; fi
case "${1:-swing}" in
  console) main_class=autobotz.Main ;;
  swing) main_class=autobotz.ui.swing.SwingMain ;;
  *) printf '%s\n' 'Uso: bash scripts/run.sh [swing|console|build]' >&2; exit 2 ;;
esac
exec java -cp "build/classes:src:$jdbc_jar" "$main_class"
