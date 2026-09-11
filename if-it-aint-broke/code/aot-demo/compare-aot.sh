#!/usr/bin/env bash
# Compares "time to first request" cold vs with an AOT cache
# (-XX:AOTCacheOutput / -XX:AOTCache, JEP 483/514/515), averaged over
# several runs since process startup timing is noisy.
#
# Usage: ./compare-aot.sh [run_count]
set -euo pipefail
cd "$(dirname "$0")"

RUNS="${1:-10}"
JAR="build/libs/aot-demo-1.0-SNAPSHOT.jar"
CACHE="build/app.aot"

mkdir -p build

echo "Building jar..."
gradle -q jar

extract_ms() {
    grep -oE 'TIME TO FIRST REQUEST: [0-9]+ ms' | grep -oE '[0-9]+'
}

echo
echo "=== Cold (no AOT cache), ${RUNS} runs ==="
cold_total=0
for i in $(seq 1 "$RUNS"); do
    ms=$(java -cp "$JAR" aot.demo.App | extract_ms)
    cold_total=$((cold_total + ms))
    echo "  run $i: ${ms} ms"
done
cold_avg=$((cold_total / RUNS))
echo "Average cold: ${cold_avg} ms"

echo
echo "=== Training run (creates ${CACHE}) ==="
rm -f "$CACHE"
java -XX:AOTCacheOutput="$CACHE" -cp "$JAR" aot.demo.App > /dev/null
ls -la "$CACHE"

echo
echo "=== With AOT cache, ${RUNS} runs ==="
aot_total=0
for i in $(seq 1 "$RUNS"); do
    ms=$(java -XX:AOTCache="$CACHE" -cp "$JAR" aot.demo.App | extract_ms)
    aot_total=$((aot_total + ms))
    echo "  run $i: ${ms} ms"
done
aot_avg=$((aot_total / RUNS))
echo "Average with AOT cache: ${aot_avg} ms"

echo
awk -v cold="$cold_avg" -v aot="$aot_avg" 'BEGIN {
    printf "Cold avg:      %d ms\n", cold
    printf "AOT cache avg: %d ms\n", aot
    printf "Speedup:       %.2fx\n", cold / aot
}'
