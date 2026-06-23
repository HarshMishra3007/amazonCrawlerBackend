#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$DIR/.env"
  set +a
fi

# Use Java 17 for Spring Boot (avoid JDK 25 + Lombok issues)
if [ -z "${JAVA_HOME:-}" ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
fi

"$DIR/install-playwright.sh"
exec mvn spring-boot:run "$@"
