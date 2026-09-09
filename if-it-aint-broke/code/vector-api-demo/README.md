# Vector API Demo

Dot product of two large `float[]` arrays, computed two ways: a plain
scalar loop, and an explicit `jdk.incubator.vector` version of the same
algorithm.

## Requirements

- Java 26 (any JDK works — the Vector API is a JDK-shipped incubator
  module, not vendor-specific)
- A CPU with vector instruction support (x86 AVX2/AVX-512, ARM SVE/SVE2)
  for the speedup to show up at all

## The idea

The Vector API expresses data-parallel operations explicitly, so the JIT
doesn't have to guess at auto-vectorization — it maps straight to CPU SIMD
instructions. `App.java` runs a JIT warm-up phase first (an unwarmed run
mostly measures the interpreter/C1, not steady-state throughput), then
times both implementations over the same random input and reports
ms/iteration and the speedup ratio.

The vector version uses `fma` (fused multiply-add: `sum = a*b + sum`) so
each iteration is one instruction per lane instead of a separate multiply
and add — a small extra edge on top of the lane-width parallelism itself.

Note: an older, simpler Vector API sum demo already exists in this repo at
`../../../vector-api/code/geecon2023` (Maven, targets Java 20). This one
is a from-scratch, Java 26 / Gradle version using dot product + `fma`
instead of a plain sum, kept self-contained to match this talk's other
demos (no external dependencies).

## Usage

```bash
cd code/vector-api-demo
gradle run
```

Tune the workload without touching code:

```bash
JAVA_TOOL_OPTIONS="-Ddemo.arrayLength=10000000 -Ddemo.warmupIterations=5 -Ddemo.measuredIterations=10" gradle run
```

## Verified results (Java 26 Temurin, AVX-512 host — 16 lanes/vector)

| Array length | Scalar | Vector API | Speedup |
|---|---|---|---|
| 5,000,000 | 8.95 ms/iter | 1.62 ms/iter | 5.52x |
| 50,000,000 | 66.35 ms/iter | 14.63 ms/iter | 4.53x |

Both squarely in the talk's claimed 2–10x range. The larger array shows a
smaller speedup because it becomes memory-bandwidth-bound rather than
compute-bound — worth mentioning live if someone asks why the ratio isn't
constant. The two results also differ by a tiny relative amount (~0.04–
0.06%) because floating-point addition isn't associative — summing in a
different order changes rounding, not correctness.
