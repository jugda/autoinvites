#!/usr/bin/env bash
# Convenience wrapper. `jbang test/Tests.java` does the same thing.
set -euo pipefail
cd "$(dirname "$0")/.."
exec jbang test/Tests.java "$@"
