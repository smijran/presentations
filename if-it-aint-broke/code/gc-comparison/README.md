# GC Comparison Demo

Same allocation-heavy, latency-sensitive workload run under four different GC
configurations, to make the pause-time story from the talk visible instead of
just asserted.

## Requirements

- Java 26 (or any JDK with G1, ZGC, and Shenandoah built in — Shenandoah is
  **not** available on Oracle JDK; use Eclipse Temurin, Red Hat build of
  OpenJDK, Amazon Corretto, etc.)

## The workload

`App.java` runs two things concurrently for a fixed duration (default 30s):

- A background thread continuously allocates short-lived garbage plus a
  growing retained set (~300MB by default), to put real pressure on the GC.
- The main thread simulates fixed-rate "request processing" (one every
  200µs by default) and times each one.

At the end it reports min / p50 / p99 / p99.9 / max latency. GC pauses show
up directly as spikes in that report — the `max` line in particular is "the
worst pause a real request would have felt." Each run also writes a detailed
`-Xlog:gc*` log to `build/gc-logs/<name>.log` for anyone who wants to look
deeper (e.g. with a GC log viewer).

Tune the workload without touching code:

```bash
JAVA_TOOL_OPTIONS="-Ddemo.durationSeconds=15 -Ddemo.retainedHeapMb=500 -Ddemo.requestIntervalMicros=100" \
  gradle runG1
```

## Usage

```bash
cd code/gc-comparison

# Baseline: G1 (default GC since Java 9)
gradle runG1

# G1 with NUMA-aware allocation (JEP 345) — only differs from runG1 on
# real multi-socket hardware; on a single-socket box expect near-identical
# numbers (confirmed locally: this machine reports "NUMA Support: Disabled").
gradle runG1Numa

# ZGC — sub-millisecond pauses at any heap size
gradle runZGC

# Shenandoah — concurrent compaction, ~1ms pauses (needs a non-Oracle JDK)
gradle runShenandoah
```

Run tests:

```bash
gradle test
```

## What to expect

On a quick local run (5s, defaults otherwise, single-socket dev machine):

| Task           | max latency |
|----------------|-------------|
| `runG1`        | ~2.0 ms     |
| `runG1Numa`    | ~1.8 ms (no NUMA hardware here — expected to match `runG1`) |
| `runZGC`       | ~0.37 ms    |
| `runShenandoah`| ~0.42 ms    |

Numbers will vary by machine and run length — run each task for the full
default 30s (or longer) live for a more stable comparison. The gap between
G1's `max` and ZGC/Shenandoah's is the demo: same workload, same heap size,
only the GC changed.
