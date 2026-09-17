# Virtual Threads Demo

Two acts, both from the talk's Virtual Threads (Project Loom) demo idea.

## Requirements

- Java 21 for Act 2's "pre-fix" run (installed here via
  `sdk install java 21.0.12+1.1-tem`)
- Java 24 for Act 2's "post-fix" run — the exact release JEP 491 landed in.
  Java 24 is EOL and no longer offered by `sdk list java`, but Temurin still
  publishes it in Adoptium's release archive
  (`https://api.adoptium.net/v3/assets/feature_releases/24/ga?...`); this repo
  expects it unpacked at `~/.sdkman/candidates/java/24.0.2-tem`
- Java 24+ for everything else — this repo's Java 26 default works
- Class files are compiled with `--release 21` (see build.gradle) so the
  exact same bytecode runs unmodified on both JDKs for Act 2

## Act 1 — throughput: platform pool vs virtual threads

`ThroughputApp` fires a large number of simulated blocking tasks
(`Thread.sleep`) at a fixed-size platform thread pool, then at a
virtual-thread-per-task executor, and reports total wall-clock time.

```bash
cd code/virtual-threads-demo
gradle runThroughput
```

Tune it:

```bash
JAVA_TOOL_OPTIONS="-Ddemo.taskCount=5000 -Ddemo.blockingMillis=50 -Ddemo.platformPoolSize=200" gradle runThroughput
```

### Verified results (Java 26 Temurin)

| Task count | Platform pool (200 threads) | Virtual threads | Speedup |
|---|---|---|---|
| 5,000 | 1,279 ms | 97 ms | 13.2x |
| 50,000 | 12,569 ms | 278 ms | 45.2x |

The platform pool's time tracks its theoretical bound almost exactly
(50,000 tasks × 50ms ÷ 200 threads = 12,500ms predicted vs. 12,569ms
measured) — it really is purely a "how many threads can be in flight"
limit, not anything smarter. Virtual threads aren't bounded by that at
all: a sleeping virtual thread doesn't occupy a carrier thread.

## Act 2 — JEP 491: pinning, proven by timing alone

`PinningApp` runs many virtual threads that each enter a `synchronized`
block on their **own private lock** (so there's no real lock contention —
only carrier-thread pinning can limit concurrency) and then block inside
it via `Thread.sleep`.

- **Pre-JEP-491 (Java 21):** entering `synchronized` and then blocking
  inside it pins the virtual thread to its carrier thread for the
  duration — so only as many tasks as there are carrier threads (CPU
  cores) can make progress at once.
- **Post-JEP-491 (Java 24+):** no pinning — all tasks proceed concurrently,
  same as without the `synchronized` block at all.

```bash
# Java 21 — pinning still happens
gradle runPinningPre491

# Java 24 — the exact release JEP 491 landed in — no pinning
gradle runPinningPost491
```

Both tasks point at JDK installs under `~/.sdkman/candidates/java/`;
override with `-Pjava21Home=...` / `-Pjava24Home=...` if yours live
elsewhere.

### Verified results (500 tasks, 200ms sleep each, 16 CPU cores)

| JDK | Actual | Predicted |
|---|---|---|
| Java 21 (pre-491) | 6,419 ms | ~6,400 ms (500 ÷ 16 carriers × 200ms) |
| Java 24 (post-491) | 214 ms | ~200 ms (no pinning at all) |

Identical source code, only the JDK changed — the ~30x gap between the
two runs *is* JEP 491. No JFR parsing needed to make the point, though a
`jdk.VirtualThreadPinned` JFR event trace (`-XX:StartFlightRecording`)
would confirm the same story at the event level for anyone who wants to
go deeper.

The post-491 run (Java 24) is fast (~200ms total) and rock-solid across
repeated runs regardless of background system load — it barely touches
the CPU long enough for noise to matter. The pre-491 run (Java 21) is the
opposite: it needs ~32 sequential 200ms batches back-to-back, so any
background CPU contention on the demo machine stretches it well past the
~6,400ms theoretical figure. Re-verify the pre-491 number close to the
actual talk, on as quiet a machine as you can get, rather than trusting
a number captured days/weeks earlier under different conditions.
