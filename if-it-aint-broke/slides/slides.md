# If It Ain't Broke
## The Hidden Cost of Your Java Comfort Zone

--

## About me

**Konrad Szałkowski**

- Principal software engineer (Egnyte)
- Java enthusiast & Poznań JUG leader
- Java performance freak

---

## 🔴 Live Poll

### Which Java version do you run in production?

Join at **AhaSlides** — see below

:notes:
Switch to the live AhaSlides session (presentation 10026566, slide 1) — do not embed this as an iframe, the presenter URL requires login and isn't the audience join view. Pull the actual audience join link/QR before the talk and paste it here.
Consider keeping this poll cumulative across every run of this talk to build a running dataset.
Options shown to the audience (single-select, 11 bucketed options): 8 · 9–10 · 11 (LTS) · 12–16 · 17 (LTS) · 18–20 · 21 (LTS) · 22–24 · 25 (LTS) · 26–27 · Not on Java / other.

---

## 🔴 Live Poll

### True or False

"My organization pays for Java support"

:notes:
Switch to AhaSlides slide 2 (True/False poll). Most rooms land heavily on "False" or "I don't know" — that's the setup for the next section.

--

<!-- .slide: data-visibility="hidden" -->

## Who Actually Has Your Back?

- Free builds vs. paid support: what you get, what you don't
- Security patches and bug fixes — who actually ships them for *your* version
- The uncomfortable truth: most teams on "old stable" are running **unsupported** builds

--

## What Paying Actually Buys You

Oracle · Red Hat · Azul · Microsoft · Amazon Corretto

1. **Extended security patches** — Java 8's last free update was **8u202** (Jan 2019); Oracle's shipped paying customers all the way to **8u503** since. That gap is CVE fixes free users never got.
2. **SLA-backed support** — an engineer on the hook, not a mailing list
3. **TCK-certified builds** — a real guarantee of Java SE spec compliance
4. **Fleet management tooling** — patching/inventory across your whole JVM estate
5. **Legal/licensing cover** *(Oracle-specific)*

:notes:
Red Hat's and Azul's free OpenJDK builds are fully open source, unrestricted for production — you only pay for support/tooling on top. Oracle ties more value to license compliance itself.

--

## The Punchline

None of this buys you **performance**.

ZGC, Virtual Threads, Compact Strings — all free, in every distribution, regardless of support tier.

Paying gets you patched and covered. Staying current gets you fast — for free.

--

<!-- .slide: data-visibility="hidden" -->

## The Clue in Your Build Tool

A C compiler has two distinct concerns:

- `-std` — *language* version
- `-march` — *target CPU*

Java conflates them in one confusing set of flags.

--

<!-- .slide: data-visibility="hidden" -->

## `--source` vs `--target` vs `--release`

| Flag | Controls |
|---|---|
| `--source` | Language features the compiler accepts |
| `--target` | Bytecode version emitted |
| `--release` | Both, **plus** the correct JDK API signatures |

Targeting old bytecode is **not** a compatibility guarantee.

--

<!-- .slide: data-visibility="hidden" -->

## The runtime surprise

Compiling for Java 19 bytecode, running on a Java 19 *JVM* —

what could still go wrong?

--

<!-- .slide: data-visibility="hidden" -->

## The runtime surprise — Demo

```java
int x = Math.clamp(15, 0, 10);   // Math.clamp — added in Java 21
```

```
javac -source 19 -target 25 Hello.java
javac --release 19 Hello.java
```

Same file. Two ways to "target Java 19."

--

<!-- .slide: data-visibility="hidden" -->

## The runtime surprise — Results

**`-source 19 -target 25`** — compiles clean, one warning:
`--release 19 is recommended instead...`

**`--release 19`** —
```
error: cannot find symbol
  symbol:   method clamp(int,int,int)
  location: class Math
```

`-source`/`-target` don't restrict the API surface — only `--release` does. That silent compile is a `NoSuchMethodError` waiting to happen on a real Java 19 JVM.

:notes:
Verified on Java 26 (Temurin). Math.clamp is a clean, non-preview example (added JEP-standard in Java 21, no --enable-preview complications) — Thread.ofVirtual() also works for this if you want a Loom tie-in instead, but it needs --enable-preview since it was preview in 19/20/21, which adds a confusing extra variable to the demo.

---

## The Comfort Zone Trap

- "It works, don't touch it"
- Version decisions get made by *not* deciding
- Real cost: CPU time, memory bills, missed concurrency gains
- This isn't a migration talk — it's an economics talk

--

## The case, in one sentence

Staying on an old Java version isn't caution.

It's a tax.

:notes:
This isn't a migration talk — it's an economics talk. Every release since 8 is free performance sitting on the table.

--

## Why Now

AI is eating every spare CPU cycle, GPU, and gigabyte on the planet.

Compute isn't getting cheaper. Memory isn't getting cheaper.

The "just add more machines" era is ending.

:notes:
Sets up the economics framing right before we dive into the enhancement tour. Ties forward to the Cost Impact section later — the same scarcity story, with real GCP numbers instead of vibes. Keep this slide short and punchy; it's a mood-setter, not a claim that needs a citation.

---

# A Decade of JVM Performance
## What You're Leaving Behind

---

## Compact Strings
*Store ASCII strings as bytes, not chars*

- JEP 254 — Compact Strings (Java 9)
- JEP 280 — Indify String Concatenation (Java 9)

--

## Compact Strings — Demo

```
cd code/compact-strings
gradle runWithCompactStrings
gradle runWithoutCompactStrings
```

5,000,000 ASCII strings, 40 chars each — one flag changes.

--

## Compact Strings — Results

| | Heap used | Bytes/string |
|---|---|---|
| `-XX:+CompactStrings` (default) | 401.51 MB | 84.2 |
| `-XX:-CompactStrings` | 592.25 MB | 124.2 |

**~32% less heap.** Zero application code changes.

--

## Compact Strings — Gains & Conditions

**Gains:** ~50% smaller backing array for Latin-1/ASCII content; less GC pressure. Indify concat: faster string building, smaller bytecode.

**Conditions:** apps with predominantly ASCII/Latin-1 content benefit most. Heavy CJK/emoji content sees far less array saving (but still gains from concat).

---

## Compact Object Headers
*Shrink every object's header from 96–128 bits to 64*

- JEP 450 — Experimental (Java 24)
- JEP 519 — Production (Java 25)
- JEP 534 — **On by default** (Java 27)

--

## Compact Object Headers — Demo

```
cd ../project_liliput/code
./gradlew runWithLilliput
./gradlew runWithoutLilliput
```

`-XX:+UseCompactObjectHeaders` toggled over a large object population.

:notes:
On Java 27+, add a third run with no flags at all to show the same reduction now happens with zero configuration.

--

## Compact Object Headers — Gains & Conditions

**Gains:** ~10–20% heap reduction for object-heavy workloads; fewer cache-line evictions; less GC pressure; **~5–10% CPU time**. Scales with object *count*, not size.

**Conditions:** experimental in 24, production (opt-in) in 25, **default in 27**. Transparent — no app code changes.

---

## Garbage Collection

G1 · ZGC · Shenandoah — one decade, three very different answers to the same problem.

--

## Why the GC demo actually shows a difference

Pure allocation churn is exactly what *every* modern collector handles well — it wouldn't tell G1 apart from ZGC.

Four choices make the difference real:

1. A **retained set** (~300MB of a 1GB heap), not just throwaway garbage
2. A **small, fixed heap** — real pressure in a live-demo window
3. Latency timed **on the working thread itself** — a stop-the-world pause shows up directly
4. Reporting **percentiles (p99, max)**, not averages — averages bury rare, large pauses

--

## G1 GC
*Make the default GC faster, smarter, less wasteful*

- JEP 248 — Default GC (Java 9)
- JEP 307 — Parallel Full GC (Java 10)
- JEP 344 / 346 — Abortable mixed collections, prompt memory return (Java 12)
- JEP 345 — NUMA-aware allocation (Java 14)
- JEP 423 — Region pinning (Java 22)
- JEP 475 — Late barrier expansion (Java 24)

--

## G1 GC — Demo

```
cd code/gc-comparison
gradle runG1        # baseline
gradle runG1Numa    # + -XX:+UseNUMA
```

Same allocation-heavy workload, only the NUMA flag changes.

:notes:
Fallback numbers if the live run misbehaves: on this (single-socket) dev machine, runG1 and runG1Numa reported near-identical max latency (~1.8–2.0ms) — exactly the "NUMA only matters on multi-socket hardware" caveat. Re-run on real multi-socket hardware before the talk for an actual delta, or narrate the caveat honestly if using single-socket hardware live.

--

## G1 GC — Gains & Conditions

**Gains:** better pause-time predictability, proactive memory return, NUMA-aware throughput on multi-socket servers, JNI-pinning stalls eliminated.

**Conditions:** NUMA gains only on multi-socket hardware. JNI pinning fix only matters with frequent native calls.

--

## ZGC
*Sub-millisecond pauses, any heap size*

- JEP 333 — Experimental (Java 11)
- JEP 377 — Production (Java 15)
- JEP 439 — Generational ZGC (Java 21)
- JEP 474 — Generational by default (Java 23)
- JEP 490 — Non-generational mode removed (Java 24)

--

## ZGC — Demo

```
cd code/gc-comparison
gradle runG1
gradle runZGC
```

**Verified:** G1 `max` ≈ **2.0ms** vs ZGC `max` ≈ **0.37ms** — same workload, same heap.

--

## ZGC — Gains & Conditions

**Gains:** <1ms pauses at any heap size. Generational mode (21+) adds 10–20% throughput on top.

**Conditions:** best for latency-sensitive workloads. ~10–20% higher memory overhead than G1. No non-generational opt-out since Java 24.

--

## Shenandoah
*Concurrent compaction — low pauses, no fragmentation*

- JEP 189 — Experimental (Java 12)
- JEP 379 — Production (Java 15)
- JEP 404 / 521 — Generational Shenandoah (Java 24 → 25)

--

## Shenandoah — Demo

```
cd code/gc-comparison
gradle runShenandoah   # needs a non-Oracle JDK build!
```

**Verified:** `max` ≈ **0.42ms** — comparable to ZGC, well under G1.

:notes:
Not available in Oracle JDK — needs Red Hat build of OpenJDK, Eclipse Temurin, etc. Worth calling out live: "notice I had to switch JDK vendors for this one" — a lived-in example of the vendor fragmentation from the earlier section.

--

## Shenandoah — Gains & Conditions

**Gains:** ~1ms pauses, throughput competitive with G1, concurrent compaction avoids fragmentation under long-running load.

**Conditions:** higher CPU overhead than G1 (concurrent work). Not in Oracle JDK.

--

## GC — Other notable JEPs

- JEP 363 — Remove CMS Garbage Collector (Java 14)
- JEP 387 — Elastic Metaspace (Java 16)

**Gains (Elastic Metaspace):** class-metadata memory returned to the OS promptly after unloading — matters most for app servers, OSGi, plugin systems.

---

## Class Data Sharing
*Pre-load class metadata once, share it across JVM instances*

- JEP 310 — Application CDS (Java 10)
- JEP 341 — Default CDS Archives (Java 12)
- JEP 350 — Dynamic CDS Archives (Java 13)

--

## CDS — Demo

```
cd code/cds-demo
./compare-cds.sh 8
```

8 concurrent JVM instances, real Jackson dependency for a realistic class graph, PSS measured from `/proc/<pid>/smaps_rollup`.

:notes:
A toy app first only showed 1.4% — too few classes to matter. Adding a real dependency is what made the effect real. Worth explaining if asked.

--

## CDS — Results

| | Total PSS (8 instances) | Startup |
|---|---|---|
| Without CDS | 459.2 MB | 434.1 ms |
| With Dynamic CDS | 358.5 MB | 234.2 ms |
| **Delta** | **−21.9%** | **1.85x faster** |

--

## CDS — Gains & Conditions

**Gains:** faster startup, lower memory across multiple JVM instances of the same app sharing one archive.

**Conditions:** it's an **OS page-cache sharing effect between processes on the same host** — not inherently a Kubernetes feature. Only holds for same-node co-location sharing the archive file, never across nodes.

---

## Ahead-of-Time Compilation
*Eliminate JIT warmup — or at least pre-load its profile*

- JEP 295 — Graal-based AOT, experimental (Java 9) ← abandoned
- JEP 410 — Removed (Java 17)
- JEP 483 — AOT class loading & linking (Java 24) ← new approach
- JEP 514 / 515 — CLI ergonomics & method profiling (Java 25)

--

## AOT — Demo

```
cd code/aot-demo
./compare-aot.sh 10
```

JDK-builtin `HttpServer`/`HttpClient`, one-step `-XX:AOTCacheOutput` / `-XX:AOTCache`.

--

## AOT — Results

| | Time to first request |
|---|---|
| Cold | 913 ms |
| With AOT cache | 757 ms |
| **Speedup** | **1.21x** |

Consistent across 10 runs each, no overlap between the two ranges.

:notes:
This app only loads a few hundred classes — a heavier real app (Spring Boot, large dependency graph) has far more startup linking to skip and would show a bigger win. Say this out loud so the modest number doesn't undersell the feature.

--

## AOT — Gains & Conditions

**Gains:** faster time-to-peak performance, reduced warmup latency — most impactful for containerized/serverless cold starts.

**Conditions:** needs a training run on a representative workload. Initially x64 Linux only.

---

## JIT & Runtime — Other Notable JEPs

- JEP 197 — Segmented Code Cache (Java 9)
- JEP 312 — Thread-Local Handshakes (Java 10)
- JEP 315 — AArch64 Intrinsics (Java 11)
- JEP 416 — Reflection via Method Handles (Java 18)

--

## Other JIT/Runtime — Gains & Conditions

**Gains:** thread-local handshakes cut latency for profiling/deopt ops. Segmented code cache reduces JIT overhead under load. AArch64 intrinsics matter for ARM (Graviton, Apple Silicon). Reflection speedup shows up most in Spring/Hibernate-style frameworks.

**Conditions:** AArch64 gains only on ARM hardware.

---

## Concurrency

Project Loom: rethinking what a "thread" costs.

--

## Virtual Threads
*M:N threading — millions of cheap threads, a small OS thread pool*

- JEP 425 / 436 — Preview, 2nd Preview (Java 19 / 20)
- JEP 444 — Virtual Threads (Java 21)
- JEP 491 — Synchronize without pinning (Java 24)

--

## Virtual Threads — Demo, Act 1: Throughput

```
cd code/virtual-threads-demo
gradle runThroughput
```

50,000 blocking tasks × 50ms — fixed platform pool (200 threads) vs. one virtual thread per task.

**Verified:** 12,569ms vs **278ms** — **45.2x**.

--

## Virtual Threads — Demo, Act 2: The JEP 491 Fix

```
gradle runPinningPre491    # Java 21
gradle runPinningPost491   # Java 26
```

Identical bytecode (`--release 21`), each virtual thread `synchronized` on its own private lock.

| JDK | Actual |
|---|---|
| Java 21 (pre-491) | 6,419 ms |
| Java 26 (post-491) | 210 ms |

Nothing changed but the JDK. That gap **is** the JEP.

--

## Virtual Threads — Gains & Conditions

**Gains:** IO-bound throughput scales without tuning pool sizes — orders of magnitude more concurrent requests. JEP 491 removes `synchronized` pinning — critical for Spring/JDBC.

**Conditions:** IO-intensive workloads only. CPU-bound tasks see no benefit. JEP 491 (Java 24) is a practical prerequisite for real frameworks.

--

## Locking
*Reduce monitor overhead — from acquisition to edge cases*

- JEP 143 — Improve Contended Locking (Java 9)
- JEP 285 — Spin-Wait Hints (Java 9)
- JEP 270 — Reserved Stack Areas (Java 9)
- JEP 374 — Deprecate & disable Biased Locking (Java 15)

--

## Locking — Gains & Conditions

**Gains:** faster monitor entry/exit under contention. CPU PAUSE hints cut power/latency in spin-waits. Reserved stack areas stop StackOverflowError from corrupting critical sections.

**Conditions:** contended-locking gains most visible under heavy shared-state concurrency. Biased-locking removal was a net win despite removing an "optimization."

---

## Vector & CPU Intrinsics

--

## Vector API
*Explicit SIMD — map straight to CPU vector instructions*

12 incubator rounds and counting (Java 16 → 27) — still not final after 11 years.

--

## Vector API — Demo

```
cd code/vector-api-demo
gradle run
```

Dot product of two large `float[]` arrays — scalar loop vs. `jdk.incubator.vector` with `fma`.

--

## Vector API — Results

| Array length | Scalar | Vector API | Speedup |
|---|---|---|---|
| 5,000,000 | 8.95 ms/iter | 1.62 ms/iter | **5.52x** |
| 50,000,000 | 66.35 ms/iter | 14.63 ms/iter | **4.53x** |

16-lane AVX-512. Smaller speedup at scale — becomes memory-bandwidth-bound.

--

## Vector API — Gains & Conditions

**Gains:** 2–10x speedup for explicitly vectorized code — ML inference, signal/image processing, crypto, numerics.

**Conditions:** needs CPU vector support (AVX2/AVX-512, SVE/SVE2). Not general-purpose — only code rewritten to use it benefits. Long incubation is a Valhalla dependency. Not stable — can't ship in a library.

--

## CPU Intrinsics
*Route specific ops straight to hardware instructions*

- JEP 246 — CPU instructions for GHASH & RSA (Java 9)

**Gains:** 2–10x throughput for AES-GCM and RSA — directly relevant for TLS-heavy services.

**Conditions:** x86 only, needs AES-NI/PCLMULQDQ. Transparent — zero code changes.

---

## Value Types — What's Coming Next
### Project Valhalla

--

## Value Classes and Objects
*Give up object identity, gain a flat, header-free layout*

- JEP 401 — Value Classes and Objects — Preview (targeting **Java 28**, March 2027)
- JEP 539 — Strict Field Initialization — Preview (Java 28)

**Not in Java 27.**

--

## Valhalla — What it changes

Value objects flatten directly into arrays/fields — no header, no identity, no pointer-chasing.

The JDK eats its own dog food: 30 platform classes (`Integer`, `LocalDate`, `Optional`, …) are already value classes under `--enable-preview`.

--

## Valhalla — Gains & Conditions

**Gains:** leaner standard library, zero app code changes for platform types.

**Conditions:** opt-in only (`value` modifier + `--enable-preview`). Brian Goetz: "optimistic" to expect this out of preview even by JDK 29. Budget 12–18 months of evaluation.

This is also **why Vector API is still incubating** — Valhalla ships first, then Vector API stabilizes on top of it.

---

## Cost Impact
### Putting a $ Number on It

Baseline: 100 × GCP `n2-standard-4` ≈ **$13,870/month** ($166,440/year)

--

## Cost Impact — By the numbers

| Improvement | Measured | Realistic fleet reduction |
|---|---|---|
| Compact Strings | 32% less heap | ~13–24 machines |
| Vector API | 4.53x (pure kernel) | ~23–78 machines |
| Virtual Threads | 45.2x (pool-bound) | ~50–75 machines |
| GC (ZGC/Shenandoah vs G1) | ~80% lower max pause | ~15–30 machines (headroom) |

**Not additive** — different workload slices, not stackable.

--

## Cost Impact — The honest number

A blended, non-double-counted estimate:

**~25–40 machines reclaimed → ~$3,500–$5,500/month, ~$42,000–$66,000/year**

Zero new hardware. Zero app rewrites for three of the four.

:notes:
Lead with Virtual Threads — "scaled out because platform threads ran out" is an extremely common real pattern, the most defensible headline number. Vector API and Compact Strings are workload-specific — say so explicitly, don't let 4.53x/45x get quoted as a general fleet number. GC is the softest claim (reclaimed headroom, not measured) — flag as assumption, not fact.

--

## The Price of Memory, Isolated

Same vCPUs, same CPU class — only memory differs. GCP's own catalog prices RAM directly.

| | vCPUs | Memory | $/hr |
|---|---|---|---|
| `c4-standard-16` | 16 | 60 GB | $0.7907 |
| `c4-highmem-16` | 16 | 124 GB | $1.0427 |

**64 GB extra for $0.2520/hr → $2.87 / GB / machine / month.**

--

## The Price of Memory — 100 Machines

| | $/month |
|---|---|
| Standard | $57,721 |
| Highmem | $76,117 |
| **Delta** | **$18,396/mo ($220,752/yr)** |

Compact Strings, Compact Object Headers, and CDS are exactly the savings that decide whether a fleet needs that jump at all.

:notes:
This is a tier-boundary argument, not a linear one — a workload already comfortably inside Standard's 60GB gets $0 from this framing no matter how much heap it saves. The value only shows up for a fleet sized right at the boundary. Don't let "~32% less heap" get misread as "~32% cheaper machines" — it's the tier jump that's worth money.
Sources: cloudprice.net c4-standard-16 / c4-highmem-16, us-central1, on-demand.
Sources: GCP e2/n2-standard-4 pricing pages, CloudZero Compute Engine Pricing Guide (2026).

---

## 🔴 Live Poll

### Which of these free, opt-in wins are you actually using today?

Multi-select — pick all that apply

:notes:
Switch to AhaSlides slide 3 (multi-select poll). Options: G1 NUMA-aware allocation · ZGC · Shenandoah · AppCDS/Dynamic CDS · AOT cache · Compact Object Headers pre-27 · Virtual Threads · Vector API · None of the above.
The likely result (mostly "none of the above") IS the closing punchline — the money is already sitting there, unclaimed.

--

## Closing Argument

- Every release since Java 8 is **free performance** — you just have to show up
- The upgrade path is manageable: `--release`, multi-release JARs, incremental rollout
- Staying behind isn't conservative.

It's a compounding tax.

---

## Links

**This talk's demos**
`if-it-aint-broke/code/` — gc-comparison, compact-strings, cds-demo, aot-demo, virtual-threads-demo, vector-api-demo

**Project Lilliput**
https://wiki.openjdk.org/display/lilliput/Main
https://openjdk.org/jeps/450 · https://openjdk.org/jeps/519

**Cloud pricing**
https://cloud.google.com/compute/vm-instance-pricing
https://instances.vantage.sh

--

## Q&A

https://app.sli.do/
