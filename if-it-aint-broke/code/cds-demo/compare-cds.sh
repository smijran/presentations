#!/usr/bin/env bash
# Compares per-process memory footprint of N concurrent JVM instances with
# and without a Dynamic CDS archive (-XX:SharedArchiveFile), reading real
# RSS and PSS (proportional set size -- correctly attributes shared pages)
# from /proc/<pid>/smaps_rollup while all instances are resident at once.
#
# Usage: ./compare-cds.sh [instance_count]
set -euo pipefail
cd "$(dirname "$0")"

INSTANCES="${1:-8}"
LIBS_DIR="build/cds-libs"
CP="${LIBS_DIR}/*"
ARCHIVE="build/app-cds.jsa"
RESIDENT_SECONDS=8
SAMPLE_DELAY_SECONDS=2

mkdir -p build

echo "Building app jar + collecting runtime deps into ${LIBS_DIR} (CDS needs a stable, real classpath -- a plain classes/ dir is rejected)..."
gradle -q copyRuntimeLibs

echo "Generating dynamic CDS archive (${ARCHIVE})..."
rm -f "$ARCHIVE"
java -XX:ArchiveClassesAtExit="$ARCHIVE" -Ddemo.sleepSeconds=0 -cp "$CP" cds.demo.App

sample_pids() {
    local label="$1"; shift
    local pids=("$@")
    sleep "$SAMPLE_DELAY_SECONDS"
    local total_rss=0 total_pss=0 count=0
    for pid in "${pids[@]}"; do
        if [[ -r "/proc/$pid/smaps_rollup" ]]; then
            local rss pss
            rss=$(awk '/^Rss:/ {print $2}' "/proc/$pid/smaps_rollup")
            pss=$(awk '/^Pss:/ {print $2}' "/proc/$pid/smaps_rollup")
            total_rss=$((total_rss + rss))
            total_pss=$((total_pss + pss))
            count=$((count + 1))
        fi
    done
    echo "${label}: sampled ${count}/${INSTANCES} live processes"
    echo "${label}: total RSS = ${total_rss} kB, total PSS = ${total_pss} kB"
    if [[ "$count" -gt 0 ]]; then
        echo "${label}: avg RSS/process = $((total_rss / count)) kB, avg PSS/process = $((total_pss / count)) kB"
    fi
    printf '%s\n' "$total_pss" > "build/${label// /_}.pss"
}

echo
echo "=== Without CDS (-Xshare:off), ${INSTANCES} instances ==="
pids=()
for i in $(seq 1 "$INSTANCES"); do
    java -Xshare:off -Ddemo.sleepSeconds="$RESIDENT_SECONDS" -cp "$CP" cds.demo.App \
        > "build/out-nocds-$i.log" 2>&1 &
    pids+=($!)
done
sample_pids "No_CDS" "${pids[@]}"
wait

echo
echo "=== With Dynamic CDS (-XX:SharedArchiveFile), ${INSTANCES} instances ==="
pids=()
for i in $(seq 1 "$INSTANCES"); do
    java -XX:SharedArchiveFile="$ARCHIVE" -Ddemo.sleepSeconds="$RESIDENT_SECONDS" -cp "$CP" cds.demo.App \
        > "build/out-cds-$i.log" 2>&1 &
    pids+=($!)
done
sample_pids "With_CDS" "${pids[@]}"
wait

echo
no_cds_pss=$(cat build/No_CDS.pss)
with_cds_pss=$(cat build/With_CDS.pss)
awk -v no="$no_cds_pss" -v yes="$with_cds_pss" -v n="$INSTANCES" 'BEGIN {
    printf "Total PSS without CDS: %d kB (%.1f MB)\n", no, no/1024
    printf "Total PSS with CDS:    %d kB (%.1f MB)\n", yes, yes/1024
    printf "Reduction:             %.1f%%\n", 100.0 * (no - yes) / no
}'
echo
echo "Per-instance stdout logs: build/out-{nocds,cds}-*.log"
