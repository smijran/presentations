# AOT Cache Demo

Measures "time to first request" — cold JVM start to first HTTP response —
with and without an AOT cache (`-XX:AOTCacheOutput` / `-XX:AOTCache`, the
one-step training/use ergonomics from JEP 514, Java 25).

## Requirements

- Java 24+ for the AOT cache flags (JEP 483); this repo uses Java 26
- Linux/macOS/Windows all fine — no OS-specific mechanism used here

## The app

`App.java` is a minimal "web app" using only JDK-builtin
`com.sun.net.httpserver.HttpServer` and `java.net.http.HttpClient` — zero
external dependencies. It starts a server, fires one request against
itself, and reports the time from the OS-level **process start** (not
`main()` — that would exclude JVM bootstrap, which is exactly what AOT
class loading/linking targets) to the first HTTP response.

`HttpClient` in particular has real initialization cost (selector thread,
HTTP/2 support setup) even for a single plaintext GET, which is what
makes this a fair, non-trivial cold-start benchmark rather than a toy.

## Usage

```bash
cd code/aot-demo
./compare-aot.sh        # 10 runs each by default
./compare-aot.sh 20     # or any run count
```

The script builds the jar, times N cold runs, does one training run to
produce `build/app.aot`, then times N runs reusing that cache.

## Verified results (Java 26 Temurin, 10 runs each)

| | Time to first request |
|---|---|
| Cold | 913 ms (range: 903–923 ms) |
| With AOT cache | 757 ms (range: 752–764 ms) |
| **Speedup** | **1.21x** (~17% faster) |

The two ranges don't overlap at all across 10 runs each — small but a
real, consistent effect. It's more modest than the Vector API or Virtual
Threads numbers elsewhere in this talk, which is itself worth saying on
slide: AOT class loading/linking (JEP 483) shaves fixed linking overhead
off startup, but this app only loads a few hundred classes total. A
heavier real app (Spring Boot, a large dependency graph) has far more
class loading/linking to skip and would show a bigger absolute and
relative win — this demo undersells AOT if presented without that
context.
