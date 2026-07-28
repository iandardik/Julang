#!/usr/bin/env bash
# Repair a broken Gradle test worker cache (GradleWorkerMain ClassNotFoundException).
# Symptom: empty workerMain dir or lock file without gradle-worker.jar.
#
# Cursor often sets GRADLE_USER_HOME to a sandbox under /var/folders/.../cursor-sandbox-cache/.
# That cache goes stale easily; this script resets workers and forces ~/.gradle for the repair run.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "${GRADLE_USER_HOME:-}" == *cursor-sandbox-cache* ]]; then
  echo "Note: GRADLE_USER_HOME is a Cursor sandbox cache:"
  echo "  $GRADLE_USER_HOME"
  echo "Repair will use \$HOME/.gradle instead for a durable fix."
fi

# Durable home for repair + subsequent advice
export GRADLE_USER_HOME="${HOME}/.gradle"

echo "Stopping Gradle daemons..."
./gradlew --stop >/dev/null 2>&1 || true

# Prefer the Gradle version from the wrapper.
GVER="$(./gradlew --version 2>/dev/null | awk '/^Gradle /{print $2; exit}')"
GVER="${GVER:-8.5}"
echo "Gradle version: $GVER"

TARGETS=(
  "$HOME/.gradle/caches/$GVER/workerMain"
)

# Also clear any Cursor sandbox worker caches that may still be in use by other terminals.
while IFS= read -r d; do
  TARGETS+=("$d")
done < <(find /var/folders -type d -path '*/cursor-sandbox-cache/*/gradle/caches/*/workerMain' 2>/dev/null || true)

for d in "${TARGETS[@]}"; do
  [[ -z "$d" ]] && continue
  if [[ -d "$d" ]]; then
    echo "Removing: $d"
    rm -rf "$d"
  fi
done

echo "Regenerating worker by running a tiny test (GRADLE_USER_HOME=$GRADLE_USER_HOME)..."
./gradlew test --tests julay.compiler.ParseTreeSmokeTest --rerun-tasks

echo
echo "Done. For future runs in this terminal, avoid the Cursor sandbox cache:"
echo "  export GRADLE_USER_HOME=\"\$HOME/.gradle\""
echo "Then re-run: ./gradlew test"
