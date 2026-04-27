	# Project Liliput

## First free lunch

--

## About me

**Konrad Szałkowski**

- Principal software engineer (Egnyte)
- Java enthusiast & Poznań JUG leader
- Dreams of marrying Java & GPUs

---

## Costs

--

## Cloud costs

--

## Ingredients

- Storage
- CPU time
- Memory

--

## GCP Cloud CUD

| vCPUs | C4-Standard (USD/hour) | C4-Highmem (USD/hour) |
|-------|------------------------|-----------------------|
| 2     | 0.061047               | 0.080892              |
| 16    | 0.498141               | 0.656901              |
| 48    | 1.494360               | 1.970703              |
| 192   | 5.977566               | 7.882875              |

~ +32% more expensive

--

## AWS Reserved Instances (1yr)

| vCPUs | M7i-Standard (USD/hour) | R7i-Highmem (USD/hour) |
|-------|-------------------------|------------------------|
| 2     | 0.067                   | 0.088                  |
| 16    | 0.533                   | 0.700                  |
| 48    | 1.600                   | 2.100                  |
| 192   | 6.401                   | 8.402                  |

~ +31% more expensive

--

<div align="center">
  <img src="RAM_price_trend.png"/>
</div>

--

## Memory is also cost driver

--

## What if we can spare some $$?

--

## For almost no cost?

---

## Java

--

## Java as platform

- Language
- JVM

--

## Enterprise choice no.1

--

## Sentenced to death

- Legacy code graveyards
- Verbose, boilerplate code
- Years without meaningful updates
- Slow, bloated JVM
- "Java is dead"

--

<div align="center">
  <img src="a10.jpg"/>
</div>

--

## Still in service

- WORA (hardware independence)
- Backwards compatibility
- Security
- Rich ecosystem
- Battle proven

--

## Still getting upgrades

Since 2017 Java is released twice a year.

Every 4 releases 1 is marked as LTS.

- New language constructs
- Virtual Threads
- Better JIT Compiler
- Faster machine code

--

## Java meets cloud

- Container-aware JVM (CPU & memory limits respected)
- Virtual Threads — millions of concurrent connections
- GraalVM Native Image — instant startup, low memory footprint
- Project CRaC — checkpoint/restore for near-native startup
- Cloud-native frameworks: Quarkus, Micronaut, Helidon

--

## You cannot retire what works

---

## Java

Java is Object Oriented Programming Language

--

## Objects

Pieces of related data stored in memory  

JVM memory

--

## JVM is objects all the way down

Not just your code — the runtime itself too

- Loaded classes → `Class` objects
- Threads → `Thread` objects
- Arrays → objects (with headers!)
- Strings → `String` objects
- Exceptions → objects

Every one of them carries an object header

--

## Object header

Handler of object  

Occupies memory

--

## Object header

- Mark Word (64 Bits)
  - Identity hash code (31 bits)
  - GC Data / age (4 bits)
  - Lock tag bits (2 bits)
  - Reserved bits / biased locking bits (27 bits)
- Class pointer (32 bits, compressed)

--

## Project Liliput

Compact Object Headers  
JEP 450/519

--

## Version 1

96 bits -> 64 bits

--

## Why was it possible?

- JEP 374 - Deprecate & remove biased locking

- Mark Word (64 Bits)
  - Identity hash code (31 bits)
  - GC Data / age (4 bits)
  - Lock tag bits (2 bits)
  - ~~Reserved bits / biased locking bits (27 bits)~~
- Class pointer (32 bits, compressed)

--

## Why was it possible?

- Class metadata lives in a dedicated 4 GB space
- JVM places each Klass at a 1 KB boundary
- Store block index, not byte address: 4 GB / 1 KB = **2^22** blocks → 22 bits

- Mark Word ~~(64 Bits)~~ -> 37 bits
  - Identity hash code (31 bits)
  - GC Data / age (4 bits)
  - Lock tag bits (2 bits)
  - ~~Reserved bits / biased locking bits (27 bits)~~
- ~~Class pointer (32 bits)~~ -> (22 bits)

---

## Code demo

--

## Results from around the world

10-20% shrink in memory size  
10% less CPU time

--

## Project Liliput
## Version 2 (hold my beer)

96 bits -> 32 bits

--

## How?

Identity hash code is the problem — **31 bits** in every header

2 state bits replace them:

| State | Meaning |
|-------|---------|
| `00` | Never hashed — nothing stored |
| `01` | Hashed, object not moved — recompute from address |
| `10` | Hashed, GC moved it — 4 bytes appended to object |

Most objects are never passed to `System.identityHashCode()` — why pay for all of them?

---

## Links

**Project Lilliput**  
https://wiki.openjdk.org/display/lilliput/Main  
https://openjdk.org/jeps/450  
https://openjdk.org/jeps/519  
https://openjdk.org/jeps/8349069  

**Object headers deep dive**  
https://www.happycoders.eu/java/compact-object-headers/  
https://www.baeldung.com/java-object-header-reduced-size-save-memory  

**Talks**  
https://www.youtube.com/watch?v=kHJ1moNLwao  
https://www.youtube.com/watch?v=9ioh6kprnPE  

**Cloud pricing**  
https://cloud.google.com/compute/vm-instance-pricing  
https://instances.vantage.sh  

--

## Q&A

