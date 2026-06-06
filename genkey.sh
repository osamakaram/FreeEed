#!/usr/bin/env bash
#
# Generate FreeEed free activation key(s) for one or more user emails.
#
# Usage:
#   ./genkey.sh user@example.com [another@example.com ...]
#
# Use this when replying to a user's registration email: run it on their
# address and paste the printed key into your reply. The app verifies the key
# offline against that same email (see org.freeeed.services.Activation).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROC_DIR="$SCRIPT_DIR/freeeed-processing"

JAR="$PROC_DIR/target/freeeed-processing-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASSES="$PROC_DIR/target/classes"

if [ "$#" -lt 1 ]; then
  echo "Usage: $(basename "$0") <email> [<email> ...]" >&2
  exit 1
fi

# Prefer the compiled classes (fast); fall back to the full jar.
if [ -f "$CLASSES/org/freeeed/services/Activation.class" ]; then
  CP="$CLASSES"
elif [ -f "$JAR" ]; then
  CP="$JAR"
else
  echo "Could not find compiled classes or the jar." >&2
  echo "Build first:  mvn -pl freeeed-processing package -DskipTests" >&2
  exit 1
fi

exec java -cp "$CP" org.freeeed.services.Activation "$@"
