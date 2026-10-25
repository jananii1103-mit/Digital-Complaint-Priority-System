#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$ROOT_DIR/project"
MAVEN="$ROOT_DIR/tools/apache-maven-3.9.16/bin/mvn"
MAVEN_REPO="$ROOT_DIR/tools/maven-repository"
CATALINA_HOME="$ROOT_DIR/tools/apache-tomcat-10.1.61"
APP_URL="http://localhost:8080/digital-complaint-system/login.jsp"

if ! command -v java >/dev/null 2>&1; then
  echo "Java JDK 11 or newer is required. Install a JDK, then run this script again."
  exit 1
fi
if [[ ! -x "$MAVEN" || ! -x "$CATALINA_HOME/bin/catalina.sh" ]]; then
  echo "The bundled Maven or Tomcat files are missing. Re-extract the complete package."
  exit 1
fi

mkdir -p "$CATALINA_HOME/logs" "$CATALINA_HOME/temp" "$CATALINA_HOME/webapps" "$CATALINA_HOME/work"

cd "$PROJECT_DIR"
echo "Building the project with bundled Maven..."
"$MAVEN" -o -Dmaven.repo.local="$MAVEN_REPO" -DskipTests package
cp "$PROJECT_DIR/target/digital-complaint-system.war" "$CATALINA_HOME/webapps/digital-complaint-system.war"
echo "Starting Tomcat. Keep this Terminal window open; press Control+C to stop it."
(sleep 5; open "$APP_URL" >/dev/null 2>&1 || true) &
"$CATALINA_HOME/bin/catalina.sh" run
