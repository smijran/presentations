# Compact Strings Demo

Reserve a lot of heap for ASCII strings, then measure exactly what they
cost — with Compact Strings (JEP 254, default since Java 9) on vs off.

## Requirements

- Java 26 (or any JDK 9+; the `-XX:+/-CompactStrings` flag has existed
  since Compact Strings shipped)

## The idea

Compact Strings is on by default: any `String` whose content fits Latin-1
(plain ASCII always does) is backed by a `byte[]` instead of a `char[]`,
halving the array's memory. `-XX:-CompactStrings` forces every string back
to the old `char[]` backing regardless of content, so the same workload on
the same heap size, with only that flag changed, makes the saving directly
measurable instead of asserted.

`App.java` allocates a large, retained population of unique ASCII strings,
forces a full GC before and after, and reports the heap-used delta and the
average bytes per string.

## Usage

```bash
cd code/compact-strings

# Compact Strings on (default) — ASCII strings backed by byte[]
gradle runWithCompactStrings

# Compact Strings off — same strings, forced back to char[]
gradle runWithoutCompactStrings
```

Tune the workload without touching code:

```bash
JAVA_TOOL_OPTIONS="-Ddemo.stringCount=2000000 -Ddemo.stringLength=60" gradle runWithCompactStrings
```

## Verified results (5,000,000 strings, 40 chars each, Java 26 Temurin)

| Flag | Heap delta | Avg bytes / string |
|---|---|---|
| `-XX:+CompactStrings` (default) | 401.51 MB | 84.2 |
| `-XX:-CompactStrings` | 592.25 MB | 124.2 |

~32% less heap for identical ASCII content — nothing in application code
changed, only the internal representation. The gap isn't a clean 2x
because each `String` also carries fixed object overhead (header, hash
cache, coder byte, array reference) that doesn't shrink with the flag —
only the backing array does.
