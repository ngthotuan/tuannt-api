#!/bin/bash
# Local dev runner: loads DB env from ~/.env-dev (MySQL/TiDB part) and starts the app.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$SCRIPT_DIR"

ENV_FILE="$HOME/.env-dev"
if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing $ENV_FILE" >&2
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if [[ -z "${MYSQL_DB_URL:-}" ]]; then
    echo "MYSQL_DB_URL not found in $ENV_FILE" >&2
    exit 1
fi

if [[ "$MYSQL_DB_URL" =~ ^mysql://([^:]+):([^@]+)@([^:/]+):([0-9]+)/(.+)$ ]]; then
    export MYSQL_USERNAME="${BASH_REMATCH[1]}"
    export MYSQL_PASSWORD="${BASH_REMATCH[2]}"
    export MYSQL_HOST="${BASH_REMATCH[3]}"
    export MYSQL_PORT="${BASH_REMATCH[4]}"
    export MYSQL_DATABASE="${BASH_REMATCH[5]}"
else
    echo "Could not parse MYSQL_DB_URL from $ENV_FILE" >&2
    exit 1
fi

: "${JWT_SECRET:=dev-placeholder-jwt-secret-at-least-32-bytes}"
export JWT_SECRET

export PATH="/opt/homebrew/bin:$PATH"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home}"

echo "Starting tuannt-api (profiles: article,dev) with MySQL host=$MYSQL_HOST db=$MYSQL_DATABASE"
exec mvn spring-boot:run
