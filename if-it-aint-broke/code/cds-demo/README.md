# CDS (Class Data Sharing) Demo

Runs several JVM processes concurrently on a single host, with and without
a Dynamic CDS archive, and measures real memory footprint via
`/proc/<pid>/smaps_rollup` — RSS and, more importantly, PSS (proportional
set size), which correctly attributes shared/mapped pages instead of
double-counting them per process the way plain RSS does.

## Requirements

- Java 26 (any JDK 13+ for Dynamic CDS Archives; JEP 350)
- Linux (`/proc/<pid>/smaps_rollup` — this is Linux-specific; on macOS use
  `vmmap` or similar instead)

## Why this needed a real dependency

A first version of this demo used a toy app with a handful of classes and
`-Xshare:off` as the "without CDS" baseline. Result: a **1.4%** PSS
reduction — real, but not a demo-worthy number, because (a) a trivial app
gives CDS almost nothing to share, and (b) heap dominates memory for such
a workload, not metaspace. Adding a real dependency (Jackson databind) so
the app actually loads a realistic framework-sized class graph — the same
shape as a real microservice — is what makes the effect show up.

## Usage

```bash
cd code/cds-demo
./compare-cds.sh        # 8 concurrent instances by default
./compare-cds.sh 16     # or any instance count
```

The script builds the jar + collects runtime deps into `build/cds-libs/`
(CDS dumping rejects a plain `classes/` directory as classpath — needs a
stable jar-based one), generates a dynamic archive via
`-XX:ArchiveClassesAtExit`, then launches N instances without CDS
(`-Xshare:off`) and N with it (`-XX:SharedArchiveFile=...`), sampling
`/proc/<pid>/smaps_rollup` for all of them 2 seconds after launch (while
each is holding itself resident in a sleep, so they're all alive at
sample time).

## Verified results (Java 26 Temurin, 8 concurrent instances)

**Memory (PSS, correctly accounts for shared pages):**

| | Total PSS (8 instances) | Avg PSS/instance |
|---|---|---|
| Without CDS | 459.2 MB | 58,781 kB |
| With Dynamic CDS | 358.5 MB | 45,883 kB |
| **Reduction** | **21.9%** | |

**Startup time** (in-process, JVM entry to end of a Jackson
serialize/deserialize round-trip, under the load of 8 concurrent
launches):

| | Avg startup |
|---|---|
| Without CDS | 434.1 ms |
| With Dynamic CDS | 234.2 ms |
| **Speedup** | **1.85x** |

Both effects come from the same mechanism: class metadata is memory-mapped
from a read-only archive shared across processes, instead of each JVM
parsing, verifying, and allocating its own copy — cheaper to load (faster
startup) and shared rather than duplicated in memory (lower PSS).

## What this does *not* show

Per the talk's own caveat: this saving is an **OS page-cache sharing
effect between JVM processes on the same host reading the same archive
file** — it is not inherently a Kubernetes feature. Running this same
comparison as separate Kubernetes pods would only show the effect if the
pods are co-located on the *same node* and the archive file is on a
layer/volume they actually share; across nodes there is nothing to share
at all. If you extend this demo to k8s, caption that explicitly.
