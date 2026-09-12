# If It Ain't Broke: The Hidden Cost of Your Java Comfort Zone

## Abstract

Many Java teams treat their runtime version as a settled question. It isn't. Every major Java release since 8 has delivered measurable performance improvements — in garbage collection, JIT compilation, memory efficiency, and concurrency — and staying behind means paying for that choice in CPU time, memory bills, and operational complexity.

Before exploring what you're missing, we'll clear up a widely misunderstood clue hiding in your build tool: the difference between the Java version your code is written against and the JVM version it actually runs on — a distinction with real consequences that most teams discover at the wrong moment.

From there, we trace a decade of JVM evolution: from G1 to ZGC and Shenandoah, JIT advances, compact strings, and Project Loom's virtual threads — each backed by concrete numbers.

The case is simple: staying on an old Java version isn't caution. It's a tax.

**AhaSlides:** [Interlude polls](https://presenter.ahaslides.com/presentation/10026566) — the three live interludes below (Java version survey, support True/False, closing "which wins are you using" poll).

---

## Outline

### 1. Welcome & Introduction to Myself

> **Interlude — AhaSlides:** Survey which Java version attendees run in production. Consider keeping this cumulative across every run of this talk.
>
> AhaSlides' Poll slide supports up to 30 answer options — enough headroom to list every version 8–27 individually (20 options) if you'd rather skip bucketing. Grouped version kept below for readability; single-select:
> 1. 8
> 2. 9–10
> 3. 11 (LTS)
> 4. 12–16
> 5. 17 (LTS)
> 6. 18–20
> 7. 21 (LTS)
> 8. 22–24
> 9. 25 (LTS)
> 10. 26–27
> 11. Not on Java / other

---

### 2. Who Actually Has Your Back?

> **Interlude — AhaSlides:** True/False — "My organization pays for Java support"

> **PARKED — hidden in the deck for now** (matching `data-visibility="hidden"` on the "Who Actually Has Your Back?" slide in `slides.md`). Superseded by the "What paying for support actually buys you" callout below, which covers the same ground in more depth.
> - Free builds vs. paid support — what you get and what you don't
> - Security patches, bug fixes, and who delivers them on your version
> - The uncomfortable truth: most teams on "old stable" are running unsupported builds

> **What paying for support actually buys you** (verified against current Oracle/Red Hat/Azul offerings). The vendors selling it: **Oracle, Red Hat, Azul, Microsoft, Amazon Corretto**.
> 1. **Extended/backported security patches** — once free public updates end for a version, CVE fixes only keep shipping to paying customers: Oracle's quarterly Critical Patch Updates (CPUs) plus out-of-cycle Bundled Patch Releases (BPRs) for urgent issues. Concrete example: **Java 8's last free public update was 8u202** (January 2019, under the old BCL license) — every update since, **8u211 through the current 8u503**, has required an active Oracle Java SE subscription for commercial use. That's ~300 update numbers of CVE fixes a free/unsupported user never received. This is the real teeth behind "unsupported build" — nobody is patching known CVEs against it anymore.
> 2. **SLA-backed vendor support** — an actual engineer on a response-time SLA for production incidents, instead of a mailing list or Stack Overflow.
> 3. **TCK-certified builds** — Azul and Red Hat license Oracle's OpenJDK Technology Compatibility Kit and certify their builds pass it, a real guarantee of Java SE spec compliance (matters for regulated industries and audits).
> 4. **Fleet management tooling** — e.g. Oracle's Fleet Automation & Lifecycle Management: inventory, automated patching, and scanning across an entire JVM estate. Arguably worth more than the patches themselves at scale.
> 5. **Legal/licensing cover** (Oracle-specific) — clarity on usage terms and audit protection, which matters given Oracle's increasingly aggressive licensing enforcement since the 2023 employee-count-based pricing change.
>
> **The free tier isn't nothing:** Red Hat's and Azul's *free* OpenJDK builds are fully open source and unrestricted for production use — you only pay for support/patches/tooling layered on top. Oracle's model ties more value to license compliance itself, a real difference from the other two.
>
> **The punchline:** none of this buys the *performance* wins covered later in this talk (ZGC, Virtual Threads, Compact Strings, etc.) — those ship free in every JDK distribution regardless of support tier. Paying for support gets you patched and legally covered; it doesn't get you fast. Staying current gets you fast for free.

---

### 3. The Clue in Your Build Tool

> **PARKED — hidden in the deck for now** (matching `data-visibility="hidden"` on the corresponding slides in `slides.md`). Kept here rather than deleted since the underlying demo is real and verified; revisit if this angle earns a place in the talk later.

- A brief analogy: C compilers, `-std`, and `-march` — two distinct concerns
- `--source` vs `--target` vs `--release` in javac, Maven, Gradle
- Why targeting an old bytecode version is not a compatibility guarantee
- `--release` as the only flag that gives you a real contract
- The runtime surprise: compiling for an old bytecode version but running on that same old JVM — what can go wrong

> **Demo — verified on Java 26 (Temurin):** `javac -source 19 -target 25 Hello.java` compiles clean with only a warning (`--release 19 is recommended instead...`). Add `int x = Math.clamp(15, 0, 10);` (`Math.clamp` — added standard, non-preview, in Java 21) to that file and it *still* compiles clean under `-source 19 -target 25` — a `NoSuchMethodError` waiting to happen on a real Java 19 JVM. Swap to `javac --release 19 Hello.java` and it fails immediately: `error: cannot find symbol — method clamp(int,int,int) — location: class Math`. `-source`/`-target` control language syntax and bytecode version only; `--release` is the only flag that also restricts the API surface to match. (`Thread.ofVirtual()` also works for this if a Project Loom tie-in is preferred, but it needs `--enable-preview` since it was preview in 19–21 — an extra variable that muddies the demo; `Math.clamp` is cleaner.)

---

### 4. Opening — The Comfort Zone Trap

*Positioned right before the JVM performance tour, not at the top of the talk — it's the pivot from "here's the landscape" to "here's what it's costing you."*

- The psychology of "it works, don't touch it"
- How Java version decisions get made (or avoided) in organizations
- The real cost of staying put: CPU time, memory bills, missed concurrency improvements
- Framing: this isn't a migration talk — it's an economics talk
- "The case, in one sentence" beat: staying on an old Java version isn't caution — it's a tax
- "Why Now" beat: AI is eating every spare CPU cycle, GPU, and gigabyte on the planet — compute and memory aren't getting cheaper, the "just add more machines" era is ending. A mood-setter, not a cited claim; ties forward to the Cost Impact section where the same scarcity story gets real GCP numbers.

---

### 5. A Decade of JVM Performance — What You're Leaving Behind

#### 5.1 Compact Strings

##### Compact Strings
*Store ASCII strings as bytes, not chars — halve their memory footprint.*

- JEP 254: Compact Strings (Java 9)
- JEP 280: Indify String Concatenation (Java 9)

> **Gains:** Latin-1/ASCII strings use `byte[]` instead of `char[]` — ~50% heap reduction for typical string-heavy workloads; proportionally less GC pressure. Indify concat replaces StringBuilder chains with invokedynamic — faster string building, smaller bytecode.
> **Conditions:** Apps with predominantly ASCII/Latin-1 strings benefit most. Heavy multi-byte character workloads (CJK, emoji-heavy) see minimal Compact Strings benefit but still gain from faster concat.
> **Demo idea:** Reserve a lot of heap for ASCII strings, then measure what they cost with `-XX:+CompactStrings` vs `-XX:-CompactStrings` — cleaner than a Java 8 vs 9+ comparison since it isolates the one variable on a single JDK instead of also changing everything else about the runtime. **Built:** `code/compact-strings` (Java 26) — verified: 5,000,000 ASCII strings (40 chars each) cost 401.51 MB with Compact Strings on vs 592.25 MB off (84.2 vs 124.2 bytes/string) — a real ~32% heap reduction with zero application code changes.
> **$ math:** 190.74 MB saved, at this exact benchmark's scale. Using $2.87/GB/month (the isolated cost of RAM on GCP C4 — same vCPUs, only memory differs between `c4-standard-16` and `c4-highmem-16`): **$0.54/machine/month → $53.53/month ($642/year) across 100 machines**, for this specific 5M-string population. Scales linearly with how much string data a real service actually holds in heap — a service with 10x the ASCII string footprint sees roughly 10x this figure.

---

#### 5.2 Compact Object Headers

##### Compact Object Headers
*Shrink every object's header from 96–128 bits down to 64 bits.*

- JEP 450: Compact Object Headers — Experimental (Java 24)
- JEP 519: Compact Object Headers — Production (Java 25)
- JEP 534: Compact Object Headers by Default (Java 27)

> **Gains:** ~10–20% heap reduction for object-heavy workloads; fewer cache line evictions; less GC pressure; ~5–10% CPU time reduction (fewer cache-line evictions and less GC work translate into real CPU savings, not just a memory-footprint number). Benefit scales with object count, not object size — most impactful for apps with millions of small objects. From Java 27, this is on **by default** — the win requires no flag at all.
> **Conditions:** Experimental (opt-in) in Java 24, production (opt-in) in Java 25, default in Java 27. Part of Project Lilliput. No application code changes needed — transparent JVM optimization.
> **Demo idea:** `../project_liliput`'s Gradle `runWithLilliput` / `runWithoutLilliput` tasks toggle `-XX:+UseCompactObjectHeaders` (plus `-XX:hashCode=4` to keep identity hashcode generation constant between runs) over a large object population. Compare heap footprint and object header size directly, e.g. via JOL or a heap histogram (`jmap -histo`), with and without the flag. On Java 27+, add a third run with no flags at all to show the same reduction now happens by default.
> **$ math:** the header shrink (96–128 bits → 64 bits) saves 4–8 bytes per object, depending on the pre-Lilliput baseline. For an illustrative 100 million live objects (a plausible mid-size heap population — not a measured number, stated as an assumption): 0.37–0.75 GB saved per machine. At $2.87/GB/month (GCP C4's isolated cost of RAM): **$1.07–$2.14/machine/month → $107–$214/month ($1,285–$2,570/year) across 100 machines.** Scales with object count: a heap with 500M live objects sees ~5x these figures.

---

#### 5.3 Class Data Sharing

##### Class Data Sharing
*Pre-load class metadata once, share it across JVM instances via memory-mapped archive.*

- JEP 310: Application Class-Data Sharing (Java 10)
- JEP 341: Default CDS Archives (Java 12)
- JEP 350: Dynamic CDS Archives (Java 13)

> **Gains:** Faster startup (skip class loading from disk); lower memory when multiple JVM instances run the same app (shared read-only archive). AppCDS extends this to application classes, not just JDK core.
> **Conditions:** Greatest impact on microservices and containers with many JVM processes. Short-lived processes (CLI tools, lambdas) see the most startup benefit. Requires archive generation step; dynamic archives (Java 13) remove the need for a separate dump run.
> **Demo idea:** The memory saving is an OS page-cache sharing effect between JVM processes reading the same archive file — it isn't inherently a Kubernetes feature, and a k8s demo risks implying otherwise. **Build the base case first without k8s**: run several plain JVM processes on a single host sharing one `-XX:SharedArchiveFile`, and compare per-process RSS (`ps`/`smem`) against the same processes without CDS. That isolates the actual mechanism cleanly. Only after that, optionally layer on a k8s example for relatability — co-locate replica pods on the *same node*, measure with `kubectl top pod` and pod start time, and caption explicitly that the win only holds for same-node co-location sharing the archive, not across the cluster. **Built:** `code/cds-demo` (Java 26, 8 concurrent instances, PSS from `/proc/<pid>/smaps_rollup`). First attempt with a toy app only showed 1.4% — too few classes for CDS to matter. Added a real dependency (Jackson) for a realistic class graph: verified **21.9% lower PSS** (459.2MB → 358.5MB total) and a **1.85x faster startup** (434ms → 234ms) with a Dynamic CDS archive vs `-Xshare:off`, same workload.
> **$ math:** 100.7 MB saved *across the 8 co-located instances on one host* (12.6 MB/instance) — at $2.87/GB/month (GCP C4's isolated cost of RAM): **$0.28/host/month → $28/month ($339/year) across 100 such hosts** (800 JVM instances total, 8/host, matching this demo's setup). Unlike Compact Strings/Compact Object Headers, this does **not** scale with machine count alone — CDS only pays off with co-located instances sharing one archive. Denser co-location scales it up fast: 20 instances/host → ~$71/mo ($848/yr); 50/host → ~$177/mo ($2,120/yr) across the same 100 hosts.

---

#### 5.4 Garbage Collection

> **Why `code/gc-comparison`'s workload actually shows a GC difference:** it's tempting to write a demo that just allocates garbage as fast as possible, but pure young-gen churn is exactly what *every* modern collector handles well — it wouldn't differentiate G1 from ZGC/Shenandoah. Four choices make the difference visible instead:
> 1. **A growing retained set (~300MB of a 1GB heap), not just throwaway garbage.** Live data that survives into old gen forces real collection work (mixed/full GCs for G1; concurrent old-gen work for ZGC/Shenandoah) — this is where the collectors' strategies actually diverge.
> 2. **A small, fixed heap (`-Xms1g -Xmx1g`).** Keeps the heap under real pressure within a live-demo-sized window (5–30s) instead of needing minutes to fill a large heap.
> 3. **Latency is timed on the same thread doing the "work,"** not measured externally. A stop-the-world pause blocks *every* thread, including the one recording `System.nanoTime()` around each unit of work — so any pause shows up directly as a spike in that thread's own recorded latency, no separate profiler needed.
> 4. **Reporting percentiles (p99, p99.9, max), not throughput or averages.** G1's pauses are rare but large; an average buries them. The `max` line is deliberately captioned "the worst pause a real request would have felt" because that's the number a pause-based collector can't hide and a concurrent collector doesn't produce.

##### G1 GC
*Make the default GC faster, smarter, and less wasteful across all dimensions.*

- JEP 248: Make G1 the Default Garbage Collector (Java 9)
- JEP 307: Parallel Full GC for G1 (Java 10)
- JEP 344: Abortable Mixed Collections for G1 (Java 12)
- JEP 346: Promptly Return Unused Committed Memory from G1 (Java 12)
- JEP 345: NUMA-Aware Memory Allocation for G1 (Java 14)
- JEP 423: Region Pinning for G1 (Java 22)
- JEP 475: Late Barrier Expansion for G1 (Java 24)

> **Gains:** Better pause-time predictability; OS memory returned proactively (lower container footprint); NUMA-aware allocation improves throughput on multi-socket servers; region pinning eliminates JNI-triggered stop-the-world stalls.
> **Conditions:** NUMA gains only on multi-socket hardware. JNI pinning fix only relevant if native code called frequently.
> **Demo idea:** Run the same allocation-heavy workload on single-socket vs. multi-socket (NUMA) hardware, with and without `-XX:+UseNUMA`. Capture throughput and pause data via `-Xlog:gc*` and compare side by side. **Built:** `code/gc-comparison` (`gradle runG1` vs `runG1Numa`, Java 26) — verified on this (single-socket) dev machine the two report near-identical `max` latency, exactly the "no NUMA hardware, no difference" caveat from Conditions above; re-run on real multi-socket hardware before the talk to get a real delta.

##### ZGC
*Sub-millisecond GC pauses regardless of heap size — from gigabytes to terabytes.*

- JEP 333: ZGC — Experimental (Java 11)
- JEP 377: ZGC — Production (Java 15)
- JEP 376: ZGC: Concurrent Thread-Stack Processing (Java 16)
- JEP 439: Generational ZGC (Java 21)
- JEP 474: ZGC: Generational Mode by Default (Java 23)
- JEP 490: ZGC: Remove the Non-Generational Mode (Java 24)

> **Gains:** Pause times <1ms at any heap size. Generational mode (Java 21+) adds throughput on top of low latency — most apps see 10–20% throughput improvement vs non-generational ZGC.
> **Conditions:** Latency-sensitive workloads (APIs, trading, real-time). Higher memory overhead than G1 (~10–20%). Generational mode default from Java 23; non-generational removed in Java 24 — no opt-out.
> **Demo idea:** Same allocation-heavy workload under `-XX:+UseG1GC` vs `-XX:+UseZGC`. Capture pause-time histograms via JFR or `-Xlog:gc` and plot them side by side — G1's pause spikes against ZGC's flat sub-millisecond line make the point without needing narration. **Built:** `code/gc-comparison` (`gradle runG1` vs `runZGC`, Java 26) — smoke-tested locally: G1 `max` ~2.0ms vs ZGC `max` ~0.37ms on the same workload/heap.

##### Shenandoah
*Concurrent compaction — low pauses without sacrificing heap compactness.*

- JEP 189: Shenandoah — Experimental (Java 12)
- JEP 379: Shenandoah — Production (Java 15)
- JEP 404: Generational Shenandoah — Experimental (Java 24)
- JEP 521: Generational Shenandoah (Java 25)

> **Gains:** ~1ms pauses; competitive throughput with G1; generational mode improves throughput further. Unlike ZGC, compacts heap concurrently — avoids fragmentation under long-running workloads.
> **Conditions:** Latency-sensitive apps. Red Hat-maintained; available in OpenJDK. Higher CPU overhead than G1 due to concurrent work. Not available in Oracle JDK.
> **Demo idea:** Same setup as the ZGC demo, run under `-XX:+UseShenandoahGC`. Requires a non-Oracle build (Red Hat build of OpenJDK, Eclipse Temurin, etc.) — worth calling out live as a concrete, lived-in example of the vendor fragmentation from Section 2. **Built:** `code/gc-comparison` (`gradle runShenandoah`, Java 26 Temurin) — smoke-tested locally: `max` ~0.42ms, comparable to ZGC and well under G1's ~2.0ms.

##### Other
- JEP 363: Remove CMS Garbage Collector (Java 14)
- JEP 387: Elastic Metaspace (Java 16)

> **Gains (Elastic Metaspace):** Reduces native memory overhead from class metadata — especially significant for apps with many classloaders (app servers, OSGi, plugin systems). Memory returned to OS promptly after class unloading.
> **Conditions:** Most impactful for dynamically loading/unloading classes. No behavioral change for simple apps.

---

#### 5.5 JIT & Runtime

##### Ahead-of-Time Compilation
*Eliminate JIT warmup by pre-compiling code — or at minimum, pre-loading its profile.*

- JEP 295: Ahead-of-Time Compilation — Graal-based, Experimental (Java 9)
- JEP 410: Remove the Experimental AOT and JIT Compiler (Java 17) ← original approach abandoned
- JEP 483: Ahead-of-Time Class Loading & Linking (Java 24) ← new approach
- JEP 514: Ahead-of-Time Command-Line Ergonomics (Java 25)
- JEP 515: Ahead-of-Time Method Profiling (Java 25)

> **Gains:** Faster time-to-peak performance; reduced warmup latency. JEP 483 pre-resolves class loading/linking — saves repeated work at startup. JEP 515 pre-populates JIT profiles from a training run — JIT hits peak optimization sooner.
> **Conditions:** Requires a training/profiling run on representative workload. Platform-specific (initially x64 Linux). Most impactful for containerized/serverless workloads where cold-start matters. JEP 295 approach was abandoned; JEP 483+ is an entirely different mechanism.
> **Demo idea:** Measure time-to-first-request for a Spring Boot (or similar) app: cold JVM startup vs. `-XX:AOTMode=record` training run followed by `-XX:AOTMode=use`. Directly relevant to the serverless/pod cold-start pain most of the audience will already recognize. **Built:** `code/aot-demo` (Java 26) — used JDK-builtin `HttpServer`/`HttpClient` instead of Spring Boot to stay dependency-free, and the simpler one-step `-XX:AOTCacheOutput`/`-XX:AOTCache` ergonomics (JEP 514) instead of the older record/create split. Verified over 10 runs each: 913ms cold vs 757ms with AOT cache — a real but modest **1.21x** (~17%), consistent across every run (no overlap in the two ranges). Caveat worth stating on slide: this app only loads a few hundred classes; a heavier real app has far more startup linking to skip and would show a bigger win — this demo undersells AOT if shown without that context.

##### Other
*Targeted JVM internals improvements — each eliminating a specific overhead.*

- JEP 197: Segmented Code Cache (Java 9) — separate caches for JIT-compiled code tiers; reduces contention, improves JIT management
- JEP 312: Thread-Local Handshakes (Java 10) — per-thread safepoint operations without global stop-the-world; reduces pause frequency
- JEP 317: Experimental Java-Based JIT Compiler (Java 10) — Graal as JIT, later removed (JEP 410)
- JEP 315: Improve Aarch64 Intrinsics (Java 11) — hardware-accelerated math/crypto on ARM
- JEP 416: Reimplement Core Reflection with Method Handles (Java 18) — removes inflation mechanism; faster reflection, lower overhead

> **Gains:** Thread-local handshakes cut latency for profiling/deoptimization ops. Segmented code cache reduces JIT compilation overhead under load. AArch64 intrinsics relevant for ARM deployments (AWS Graviton, Apple Silicon).
> **Conditions:** AArch64 gains only on ARM hardware. Reflection speedup most visible in frameworks doing heavy reflection (Spring, Hibernate).

---

#### 5.6 Concurrency

##### Virtual Threads (Project Loom)
*M:N threading — millions of cheap virtual threads multiplexed onto a small OS thread pool.*

- JEP 425: Virtual Threads — Preview (Java 19)
- JEP 436: Virtual Threads — Second Preview (Java 20)
- JEP 444: Virtual Threads (Java 21)
- JEP 491: Synchronize Virtual Threads without Pinning (Java 24)

> **Gains:** IO-bound throughput scales without tuning thread pool sizes. Eliminates the "thread per request" bottleneck — applications previously limited by OS thread count can handle orders of magnitude more concurrent requests. JEP 491 removes pinning on `synchronized` blocks — critical for Spring, JDBC, and most frameworks.
> **Conditions:** IO-intensive workloads only (HTTP, DB, file IO). CPU-bound tasks see no improvement — virtual threads don't add CPU parallelism. JEP 491 (Java 24) is a prerequisite for most real-world frameworks; Java 21 virtual threads had pinning issues with `synchronized`.
> **Demo idea:** Fire N concurrent IO-bound tasks (simulate a blocking call, e.g. `Thread.sleep` or a real HTTP/DB call) against a fixed-size platform thread pool (e.g. 200 threads) vs `Executors.newVirtualThreadPerTaskExecutor()`. Crank N into the tens of thousands — the platform pool queues and stalls while virtual threads complete near-linearly. Second act: reproduce the JEP 491 pinning fix live — wrap the blocking call in a `synchronized` block on Java 21 vs Java 24+, and show virtual-thread pinning events (JFR `jdk.VirtualThreadPinned`) appear pre-491 and disappear after. **Built:** `code/virtual-threads-demo`. Act 1 (`gradle runThroughput`, Java 26): 50,000 tasks × 50ms — platform pool (200 threads) 12,569ms vs virtual threads 278ms, a 45.2x speedup, tracking the pool's theoretical bound almost exactly. Act 2 turned out not to need JFR at all — timing alone proves JEP 491: same bytecode (compiled `--release 21`), each virtual thread `synchronized` on its own private lock then `Thread.sleep`s inside it; run on Java 21 (`gradle runPinningPre491`) takes 6,419ms (matches the pinned prediction of 6,400ms = 500 tasks ÷ 16 carrier threads × 200ms); the identical code on Java 26 (`gradle runPinningPost491`) takes 210ms. The ~30x gap between those two runs, nothing else changed, *is* the JEP.

##### Locking
*Reduce overhead of monitor operations — from acquisition to edge-case safety.*

- JEP 143: Improve Contended Locking (Java 9)
- JEP 285: Spin-Wait Hints (Java 9)
- JEP 270: Reserved Stack Areas for Critical Sections (Java 9)
- JEP 374: Deprecate and Disable Biased Locking (Java 15)

> **Gains:** Faster monitor entry/exit under contention; CPU PAUSE hints reduce power and latency in spin-wait loops; reserved stack areas prevent StackOverflowError from corrupting critical sections. Biased locking removal simplifies JVM internals — was net negative in multi-threaded apps.
> **Conditions:** Contended locking improvements most visible in highly concurrent apps with shared state. Biased locking removal may marginally hurt single-threaded lock access patterns (rare in practice).

---

#### 5.7 Vector & CPU Intrinsics

##### Vector API
*Explicit SIMD — express data-parallel operations that map directly to CPU vector instructions.*

- JEP 338: Vector API — 1st Incubator (Java 16)
- JEP 414: Vector API — 2nd Incubator (Java 17)
- JEP 417: Vector API — 3rd Incubator (Java 18)
- JEP 426: Vector API — 4th Incubator (Java 19)
- JEP 438: Vector API — 5th Incubator (Java 20)
- JEP 448: Vector API — 6th Incubator (Java 21)
- JEP 460: Vector API — 7th Incubator (Java 22)
- JEP 469: Vector API — 8th Incubator (Java 23)
- JEP 489: Vector API — 9th Incubator (Java 24)
- JEP 508: Vector API — 10th Incubator (Java 25)
- JEP 529: Vector API — 11th Incubator (Java 26)
- JEP 537: Vector API — 12th Incubator (Java 27) ← still not final after 11 years

> **Gains:** 2–10x speedup for explicitly vectorized code vs scalar loops — ML inference, signal processing, image processing, crypto, numerical computation.
> **Conditions:** Requires CPU with vector instruction support (x86 AVX2/AVX-512, ARM SVE/SVE2). Not general-purpose — only benefits code explicitly rewritten to use the API. Long incubation due to dependency on Project Valhalla (value types — see §5.8). Not stable API yet — cannot use in libraries shipped as dependencies.
> **Demo idea:** A simple numeric kernel (dot product, array sum, or a small image-processing filter) over a large `float[]`/`int[]`, run three ways: a plain scalar loop, an auto-vectorization-friendly scalar loop, and an explicit `jdk.incubator.vector` version (`--add-modules jdk.incubator.vector --enable-preview` as needed). Benchmark with JMH to avoid JIT/warmup noise, and report ops/sec — the 2–10x gap over the scalar baseline is the whole point. Worth noting on slide: needs `--add-modules jdk.incubator.vector` since it's still incubating. **Built:** `code/vector-api-demo` (Java 26) — dot product with `fma`, hand-rolled warm-up + timed loop instead of JMH (kept dependency-free like the other demos). Verified on this AVX-512 host (16 lanes/vector): 5.52x speedup at 5M elements, 4.53x at 50M (smaller at scale because it becomes memory-bandwidth-bound). An older, simpler sum-based demo also exists in this repo at `../vector-api/code/geecon2023` (Maven, Java 20) — this one is a fresh, self-contained version matching this talk's Java 26 baseline.

##### CPU Intrinsics
*Route specific operations directly to hardware instruction equivalents.*

- JEP 246: Leverage CPU Instructions for GHASH and RSA (Java 9)

> **Gains:** 2–10x throughput improvement for AES-GCM (GHASH) and RSA operations — directly relevant for TLS-heavy services.
> **Conditions:** x86 only; requires hardware AES-NI and PCLMULQDQ support. Transparent — no code changes needed.

---

#### 5.8 Value Types — What's Coming Next (Project Valhalla)

##### Value Classes and Objects
*Give up object identity to gain a flat, header-free memory layout — the JVM's biggest data-layout change in a decade.*

- JEP 401: Value Classes and Objects — Preview (targeted for Java 28, March 2027)
- JEP 539: Strict Field Initialization in the JVM — Preview (targeted for Java 28, March 2027)

> **Gains:** Value objects can be flattened directly into arrays and fields instead of stored as heap pointers — no object header, no identity, no pointer-chasing for simple data carriers (`Point`, `Complex`, wrapper-like types). The JDK is eating its own dog food: 30 platform classes — `Integer`, `LocalDate`, `Optional`, and others — are already declared as value classes under `--enable-preview`, so the standard library gets leaner with zero app code changes.
> **Conditions:** First preview targets JDK 28 (March 2027) — **not in Java 27**. Opt-in only via the `value` modifier plus `--enable-preview`; existing classes are unaffected until migrated. Brian Goetz has called it "optimistic" to expect this out of preview by JDK 29 (the Sept 2027 LTS) — budget a 12–18 month evaluation runway before production use. This is also the blocker that's kept the Vector API (§5.7) in incubation for 12 rounds and counting: Valhalla ships first, then Vector API can stabilize on top of it.

---

### 6. Closing Argument

#### Cost Impact — Putting a $ Number on It

Grounded in the demos above plus real GCP pricing (n2-standard-4: 4 vCPU/16GB, $0.19/hr ≈ $138.70/month at 730 hrs — the closer hardware match to what was measured, since it's the AVX-512 host the Vector API demo ran on). Baseline: **100 × n2-standard-4 ≈ $13,870/month ($166,440/year).**

| Improvement | Measured | Applicability caveat | Realistic fleet reduction | $/month saved | $/year saved |
|---|---|---|---|---|---|
| **Compact Strings** | 32% less heap (401.5MB vs 592.3MB @ 5M strings) | Only helps if memory-bound, and only for the string-heavy fraction of live heap | ~13–24 machines | $1,800–$3,330 | $21,600–$39,950 |
| **Vector API** | 4.53x on a pure vectorized kernel | Amdahl's law bites hard — if the vectorizable hot path is ~30% of total CPU time, whole-service speedup is only ~1.3x, not 4.53x | ~23 machines (mixed workload) up to 78 (kernel-only fleet) | $3,190–$10,820 | $38,280–$129,830 |
| **Virtual Threads** | 45.2x on a thread-pool-bound IO workload | The 45x is a ceiling specific to services literally bottlenecked on platform-thread count; the real bottleneck (DB, downstream, CPU) caps it far lower in practice — 2–5x is a defensible real-world range | ~50–75 machines | $6,935–$10,400 | $83,220–$124,830 |
| **GC (ZGC/Shenandoah vs G1)** | ~80% lower max pause (0.37–0.42ms vs 2.0ms) | Doesn't reduce machine count directly — the $ case is teams reclaiming capacity headroom they keep *purely* to protect p99 SLA from GC pauses (typically 15–30%, an assumption, not something the benchmark itself measures) | ~15–30 machines (illustrative) | $2,080–$4,160 | $24,970–$49,930 |

**Not simply additive** — a given machine doesn't get all four benefits at once; they apply to different workload slices of a real fleet. A blended, non-double-counted estimate for a mixed 100-machine fleet: roughly **25–40 machines reclaimed → ~$3,500–$5,500/month, or ~$42,000–$66,000/year** — from opt-in features already sitting in the JDK, zero new hardware, and (for three of the four) zero app rewrites.

Lead with **Virtual Threads** — "scaled out because platform threads ran out" is an extremely common real-world Java microservice pattern, making it the most defensible headline number. Vector API and Compact Strings are real but workload-specific — say so explicitly on the slide, don't let "4.53x" or "45x" get quoted out of context as a general fleet number. GC is the softest claim (reclaimed headroom, not measured savings) — flag it as an assumption, not a fact.

*Sources: [e2-standard-4 pricing](https://www.economize.cloud/resources/gcp/pricing/compute-engine/e2-standard-4/), [n2-standard-4 pricing](https://www.economize.cloud/resources/gcp/pricing/compute-engine/n2-standard-4/), [Google Cloud Compute Engine Pricing Guide (2026)](https://www.cloudzero.com/blog/google-cloud-compute-engine-pricing-guide/)*

> **Interlude — AhaSlides:** Poll slide, "Allow multiple answers" enabled — "Which of these free, opt-in wins are you actually using today?"
> 1. G1 NUMA-aware allocation (`-XX:+UseNUMA`)
> 2. ZGC (`-XX:+UseZGC`)
> 3. Shenandoah (`-XX:+UseShenandoahGC`)
> 4. AppCDS / Dynamic CDS archives (`-XX:SharedArchiveFile`)
> 5. AOT cache — training run + `-XX:AOTMode=record`/`use`
> 6. Compact Object Headers pre-Java 27 (`-XX:+UseCompactObjectHeaders`)
> 7. Virtual Threads (Project Loom)
> 8. Vector API (`--add-modules jdk.incubator.vector`)
> 9. None of the above
>
> All eight are things covered in this talk, all free, all opt-in — none of them are the default. The likely result (mostly "none of the above") *is* the closing punchline: the money is already sitting there, unclaimed.

- Every release since Java 8 is free performance — you just have to show up
- The upgrade path is manageable: `--release`, multi-release JARs, incremental rollout
- Staying behind isn't conservative. It's a compounding tax.
