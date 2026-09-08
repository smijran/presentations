# If It Ain't Broke: The Hidden Cost of Your Java Comfort Zone

## Abstract

Many Java teams treat their runtime version as a settled question. It isn't. Every major Java release since 8 has delivered measurable performance improvements — in garbage collection, JIT compilation, memory efficiency, and concurrency — and staying behind means paying for that choice in CPU time, memory bills, and operational complexity.

Before exploring what you're missing, we'll clear up a widely misunderstood clue hiding in your build tool: the difference between the Java version your code is written against and the JVM version it actually runs on — a distinction with real consequences that most teams discover at the wrong moment.

From there, we trace a decade of JVM evolution: from G1 to ZGC and Shenandoah, JIT advances, compact strings, and Project Loom's virtual threads — each backed by concrete numbers.

The case is simple: staying on an old Java version isn't caution. It's a tax.

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

### 2. Opening — The Comfort Zone Trap

- The psychology of "it works, don't touch it"
- How Java version decisions get made (or avoided) in organizations
- The real cost of staying put: CPU time, memory bills, missed concurrency improvements
- Framing: this isn't a migration talk — it's an economics talk

> **Interlude — AhaSlides:** True/False — "My organization pays for Java support (Oracle, Red Hat, Azul, etc.) on the version we run in production."

---

### 3. Who Actually Has Your Back?

- What "supported" really means: Oracle, Red Hat, Azul, Microsoft, Amazon Corretto
- Free builds vs. paid support — what you get and what you don't
- Security patches, bug fixes, and who delivers them on your version
- The uncomfortable truth: most teams on "old stable" are running unsupported builds

---

### 4. The Clue in Your Build Tool

- A brief analogy: C compilers, `-std`, and `-march` — two distinct concerns
- `--source` vs `--target` vs `--release` in javac, Maven, Gradle
- Why targeting an old bytecode version is not a compatibility guarantee
- `--release` as the only flag that gives you a real contract
- The runtime surprise: compiling for Java 8 bytecode but running on Java 8 JVM — what can go wrong

---

### 5. A Decade of JVM Performance — What You're Leaving Behind

#### 5.1 Garbage Collection

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
> **Demo idea:** Run the same allocation-heavy workload on single-socket vs. multi-socket (NUMA) hardware, with and without `-XX:+UseNUMA`. Capture throughput and pause data via `-Xlog:gc*` and compare side by side.

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
> **Demo idea:** Same allocation-heavy workload under `-XX:+UseG1GC` vs `-XX:+UseZGC`. Capture pause-time histograms via JFR or `-Xlog:gc` and plot them side by side — G1's pause spikes against ZGC's flat sub-millisecond line make the point without needing narration.

##### Shenandoah
*Concurrent compaction — low pauses without sacrificing heap compactness.*

- JEP 189: Shenandoah — Experimental (Java 12)
- JEP 379: Shenandoah — Production (Java 15)
- JEP 404: Generational Shenandoah — Experimental (Java 24)
- JEP 521: Generational Shenandoah (Java 25)

> **Gains:** ~1ms pauses; competitive throughput with G1; generational mode improves throughput further. Unlike ZGC, compacts heap concurrently — avoids fragmentation under long-running workloads.
> **Conditions:** Latency-sensitive apps. Red Hat-maintained; available in OpenJDK. Higher CPU overhead than G1 due to concurrent work. Not available in Oracle JDK.
> **Demo idea:** Same setup as the ZGC demo, run under `-XX:+UseShenandoahGC`. Requires a non-Oracle build (Red Hat build of OpenJDK, Eclipse Temurin, etc.) — worth calling out live as a concrete, lived-in example of the vendor fragmentation from Section 3.

##### Other
- JEP 363: Remove CMS Garbage Collector (Java 14)
- JEP 387: Elastic Metaspace (Java 16)

> **Gains (Elastic Metaspace):** Reduces native memory overhead from class metadata — especially significant for apps with many classloaders (app servers, OSGi, plugin systems). Memory returned to OS promptly after class unloading.
> **Conditions:** Most impactful for dynamically loading/unloading classes. No behavioral change for simple apps.

---

#### 5.2 JIT & Runtime

##### Compact Strings
*Store ASCII strings as bytes, not chars — halve their memory footprint.*

- JEP 254: Compact Strings (Java 9)
- JEP 280: Indify String Concatenation (Java 9)

> **Gains:** Latin-1/ASCII strings use `byte[]` instead of `char[]` — ~50% heap reduction for typical string-heavy workloads; proportionally less GC pressure. Indify concat replaces StringBuilder chains with invokedynamic — faster string building, smaller bytecode.
> **Conditions:** Apps with predominantly ASCII/Latin-1 strings benefit most. Heavy multi-byte character workloads (CJK, emoji-heavy) see minimal Compact Strings benefit but still gain from faster concat.
> **Demo idea:** Load a large ASCII/Latin-1 text corpus into `String[]` on Java 8 vs Java 9+. Compare per-object size with JOL (Java Object Layout) or `jmap -histo` — the `byte[]` vs `char[]` backing is a clean, visual before/after.

##### Class Data Sharing
*Pre-load class metadata once, share it across JVM instances via memory-mapped archive.*

- JEP 310: Application Class-Data Sharing (Java 10)
- JEP 341: Default CDS Archives (Java 12)
- JEP 350: Dynamic CDS Archives (Java 13)

> **Gains:** Faster startup (skip class loading from disk); lower memory when multiple JVM instances run the same app (shared read-only archive). AppCDS extends this to application classes, not just JDK core.
> **Conditions:** Greatest impact on microservices and containers with many JVM processes. Short-lived processes (CLI tools, lambdas) see the most startup benefit. Requires archive generation step; dynamic archives (Java 13) remove the need for a separate dump run.
> **Demo idea:** The memory saving is an OS page-cache sharing effect between JVM processes reading the same archive file — it isn't inherently a Kubernetes feature, and a k8s demo risks implying otherwise. **Build the base case first without k8s**: run several plain JVM processes on a single host sharing one `-XX:SharedArchiveFile`, and compare per-process RSS (`ps`/`smem`) against the same processes without CDS. That isolates the actual mechanism cleanly. Only after that, optionally layer on a k8s example for relatability — co-locate replica pods on the *same node*, measure with `kubectl top pod` and pod start time, and caption explicitly that the win only holds for same-node co-location sharing the archive, not across the cluster.

##### Ahead-of-Time Compilation
*Eliminate JIT warmup by pre-compiling code — or at minimum, pre-loading its profile.*

- JEP 295: Ahead-of-Time Compilation — Graal-based, Experimental (Java 9)
- JEP 410: Remove the Experimental AOT and JIT Compiler (Java 17) ← original approach abandoned
- JEP 483: Ahead-of-Time Class Loading & Linking (Java 24) ← new approach
- JEP 514: Ahead-of-Time Command-Line Ergonomics (Java 25)
- JEP 515: Ahead-of-Time Method Profiling (Java 25)

> **Gains:** Faster time-to-peak performance; reduced warmup latency. JEP 483 pre-resolves class loading/linking — saves repeated work at startup. JEP 515 pre-populates JIT profiles from a training run — JIT hits peak optimization sooner.
> **Conditions:** Requires a training/profiling run on representative workload. Platform-specific (initially x64 Linux). Most impactful for containerized/serverless workloads where cold-start matters. JEP 295 approach was abandoned; JEP 483+ is an entirely different mechanism.
> **Demo idea:** Measure time-to-first-request for a Spring Boot (or similar) app: cold JVM startup vs. `-XX:AOTMode=record` training run followed by `-XX:AOTMode=use`. Directly relevant to the serverless/pod cold-start pain most of the audience will already recognize.

##### Compact Object Headers
*Shrink every object's header from 96–128 bits down to 64 bits.*

- JEP 450: Compact Object Headers — Experimental (Java 24)
- JEP 519: Compact Object Headers — Production (Java 25)
- JEP 534: Compact Object Headers by Default (Java 27)

> **Gains:** ~10–20% heap reduction for object-heavy workloads; fewer cache line evictions; less GC pressure. Benefit scales with object count, not object size — most impactful for apps with millions of small objects. From Java 27, this is on **by default** — the win requires no flag at all.
> **Conditions:** Experimental (opt-in) in Java 24, production (opt-in) in Java 25, default in Java 27. Part of Project Lilliput. No application code changes needed — transparent JVM optimization.

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

#### 5.3 Concurrency

##### Virtual Threads (Project Loom)
*M:N threading — millions of cheap virtual threads multiplexed onto a small OS thread pool.*

- JEP 425: Virtual Threads — Preview (Java 19)
- JEP 436: Virtual Threads — Second Preview (Java 20)
- JEP 444: Virtual Threads (Java 21)
- JEP 491: Synchronize Virtual Threads without Pinning (Java 24)

> **Gains:** IO-bound throughput scales without tuning thread pool sizes. Eliminates the "thread per request" bottleneck — applications previously limited by OS thread count can handle orders of magnitude more concurrent requests. JEP 491 removes pinning on `synchronized` blocks — critical for Spring, JDBC, and most frameworks.
> **Conditions:** IO-intensive workloads only (HTTP, DB, file IO). CPU-bound tasks see no improvement — virtual threads don't add CPU parallelism. JEP 491 (Java 24) is a prerequisite for most real-world frameworks; Java 21 virtual threads had pinning issues with `synchronized`.

##### Locking
*Reduce overhead of monitor operations — from acquisition to edge-case safety.*

- JEP 143: Improve Contended Locking (Java 9)
- JEP 285: Spin-Wait Hints (Java 9)
- JEP 270: Reserved Stack Areas for Critical Sections (Java 9)
- JEP 374: Deprecate and Disable Biased Locking (Java 15)

> **Gains:** Faster monitor entry/exit under contention; CPU PAUSE hints reduce power and latency in spin-wait loops; reserved stack areas prevent StackOverflowError from corrupting critical sections. Biased locking removal simplifies JVM internals — was net negative in multi-threaded apps.
> **Conditions:** Contended locking improvements most visible in highly concurrent apps with shared state. Biased locking removal may marginally hurt single-threaded lock access patterns (rare in practice).

---

#### 5.4 Vector & CPU Intrinsics

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
> **Conditions:** Requires CPU with vector instruction support (x86 AVX2/AVX-512, ARM SVE/SVE2). Not general-purpose — only benefits code explicitly rewritten to use the API. Long incubation due to dependency on Project Valhalla (value types — see §5.5). Not stable API yet — cannot use in libraries shipped as dependencies.

##### CPU Intrinsics
*Route specific operations directly to hardware instruction equivalents.*

- JEP 246: Leverage CPU Instructions for GHASH and RSA (Java 9)

> **Gains:** 2–10x throughput improvement for AES-GCM (GHASH) and RSA operations — directly relevant for TLS-heavy services.
> **Conditions:** x86 only; requires hardware AES-NI and PCLMULQDQ support. Transparent — no code changes needed.

---

#### 5.5 Value Types — What's Coming Next (Project Valhalla)

##### Value Classes and Objects
*Give up object identity to gain a flat, header-free memory layout — the JVM's biggest data-layout change in a decade.*

- JEP 401: Value Classes and Objects — Preview (targeted for Java 28, March 2027)
- JEP 539: Strict Field Initialization in the JVM — Preview (targeted for Java 28, March 2027)

> **Gains:** Value objects can be flattened directly into arrays and fields instead of stored as heap pointers — no object header, no identity, no pointer-chasing for simple data carriers (`Point`, `Complex`, wrapper-like types). The JDK is eating its own dog food: 30 platform classes — `Integer`, `LocalDate`, `Optional`, and others — are already declared as value classes under `--enable-preview`, so the standard library gets leaner with zero app code changes.
> **Conditions:** First preview targets JDK 28 (March 2027) — **not in Java 27**. Opt-in only via the `value` modifier plus `--enable-preview`; existing classes are unaffected until migrated. Brian Goetz has called it "optimistic" to expect this out of preview by JDK 29 (the Sept 2027 LTS) — budget a 12–18 month evaluation runway before production use. This is also the blocker that's kept the Vector API (§5.4) in incubation for 12 rounds and counting: Valhalla ships first, then Vector API can stabilize on top of it.

---

#### 5.6 Observability & Profiling

##### Java Flight Recorder
*Always-on, production-safe profiling and diagnostics built into the JVM.*

- JEP 328: Flight Recorder (Java 11)
- JEP 331: Low-Overhead Heap Profiling (Java 11)
- JEP 349: JFR Event Streaming (Java 14)
- JEP 509: JFR CPU-Time Profiling — Experimental (Java 25)
- JEP 518: JFR Cooperative Sampling (Java 25)
- JEP 520: JFR Method Timing & Tracing (Java 25)

> **Gains:** <1% overhead in production; continuous recording catches intermittent issues. Streaming (Java 14) enables real-time dashboards without post-hoc dump analysis. CPU-time profiling (Java 25) gives accurate per-method CPU attribution. Method timing enables pinpointing hot paths without external agents.
> **Conditions:** Available as open-source since Java 11 (was Oracle-commercial in Java 8 — a common blocker). CPU-time profiling requires OS support for CPU-time clocks. Streaming requires consumer code; not zero-config out of the box.

---

### 6. Closing Argument

> **Interlude — AhaSlides:** Poll slide, "Allow multiple answers" enabled — "Which of these free, opt-in wins are you actually using today?"
> 1. G1 NUMA-aware allocation (`-XX:+UseNUMA`)
> 2. ZGC (`-XX:+UseZGC`)
> 3. Shenandoah (`-XX:+UseShenandoahGC`)
> 4. AppCDS / Dynamic CDS archives (`-XX:SharedArchiveFile`)
> 5. AOT cache — training run + `-XX:AOTMode=record`/`use`
> 6. Compact Object Headers pre-Java 27 (`-XX:+UseCompactObjectHeaders`)
> 7. Virtual Threads (Project Loom)
> 8. Vector API (`--add-modules jdk.incubator.vector`)
> 9. JFR continuous recording (`-XX:StartFlightRecording`)
> 10. None of the above
>
> All nine are things covered in this talk, all free, all opt-in — none of them are the default. The likely result (mostly "none of the above") *is* the closing punchline: the money is already sitting there, unclaimed.

- Every release since Java 8 is free performance — you just have to show up
- The upgrade path is manageable: `--release`, multi-release JARs, incremental rollout
- Staying behind isn't conservative. It's a compounding tax.
