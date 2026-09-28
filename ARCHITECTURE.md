# Architecture

This document explains how RiemannLab is put together and why. It is a
guide to the software's design, not to the mathematics — see the
project's math background material (Phase 9.4) for that — and it is not,
under any reading, evidence toward the Riemann Hypothesis.

## 1. Purpose & Disclaimer

RiemannLab is a computational research and visualization project for
numerically evaluating the Riemann zeta function and investigating its
non-trivial zeros. It is a numerical software engineering exercise, not a
mathematical proof of anything. Nothing in this document, or in the
codebase it describes, establishes, claims, or moves toward establishing
the Riemann Hypothesis. Numerical agreement with known results is
evidence that the software works; it is not proof, and no amount of it
becomes proof.

## 2. Architectural Overview

The codebase is organized into layers, each depending only on the layers
above it:

```text
core
  |
  v
zeta
  |
  v
zeros / analysis
  |
  v
validation
  |
  v
viz
  |
  v
app.cli / bench
```

Each arrow is a dependency: `zeta` depends on `core`, `zeros` and
`analysis` depend on `zeta`, and so on down to `app.cli` and `bench`,
which depend on everything below them but are depended on by nothing.
This gives the project a single, acyclic direction of dependency from
foundational math primitives at the top to user-facing entry points at
the bottom.

## 3. Package Structure & Dependency Direction

| Package | Responsibility |
|---|---|
| `core.complex` | Complex number arithmetic (`Complex`, `ComplexMath`) — the arithmetic every other package is built on. |
| `core.numeric` | General-purpose numerical utilities not specific to zeta: root finding (`RootFindingResult` and its finders), series acceleration (`AlternatingSeriesAccelerator`). |
| `core.special` | Special functions used by zeta evaluation but not specific to it, principally the gamma function (`GammaFunction`). |
| `zeta` | Evaluating the zeta function itself: the Dirichlet series, the eta function, and the three evaluator implementations described in Section 5. |
| `zeros` | Finding zero locations on the critical line: scanning (`CriticalLineScanner`) and refining (`ZeroRefiner`) candidates. |
| `analysis` | Sampling and analyzing behaviour along the critical line independent of zero-finding: `CriticalLineSampler`, zero spacing statistics. |
| `validation` | Comparing computed results against published reference values, strictly after the fact (Section 8). |
| `viz` | Rendering results as images: domain coloring and critical-line plots. |
| `app.cli` | Runnable entry points (`main` methods) that demonstrate each capability. |
| `bench` | JMH benchmarks characterizing throughput; not part of the tested production surface. |

The dependency rule: **a package may depend only on packages above it in
this table** (equivalently, lower in the Section 2 diagram). `core.complex`
knows nothing about `zeta`; `zeta` knows nothing about `zeros`, `viz`, or
`app.cli`; and so on. Dependencies flow from foundational packages toward
higher-level application packages, never the reverse. This is what keeps
the math core testable and reusable independent of any particular
consumer (a CLI demo, a benchmark, a renderer).

## 4. Core Design Principles

- **Immutable value objects.** Every data-carrying type in the codebase
  (`Complex`, `ZetaResult`, `ZeroCandidate`, `RefinedZero`,
  `CriticalLineSample`, the `validation` report types, and others) is a
  Java `record`. There is no setter anywhere in the domain model.
- **Stateless, thread-safe components.** Every `ZetaEvaluator`
  implementation is documented and tested as stateless and thread-safe.
  This is not incidental: it is what makes parallel scanning
  (`CriticalLineScanner.scanForZerosParallel`) and parallel rendering
  (`DomainColourRenderer`) correct without locks, copies, or
  synchronization of any kind.
- **Explicit dependencies.** Collaborators are passed in through
  constructors (a `ZetaEvaluator` into a `CriticalLineScanner`, a
  `MeterRegistry` into the classes that report against it), never
  looked up from a global or a framework container. There is no
  dependency-injection framework in this project; explicit constructor
  wiring is enough at this scale and keeps every class's dependencies
  visible at its call site.
- **Meaningful names over comments.** Method and variable names carry
  intent (`worstDeviationFromCriticalLine`, `evaluationsPerZero`) so
  that JavaDoc explains *why*, not *what the next line does*.
- **No magic numbers.** Every numeric constant with meaning (term
  counts, step sizes, margins) is a named constant, not a literal
  buried in a method body.

## 5. Evaluator Progression

Three `ZetaEvaluator` implementations exist in the `zeta` package, each
solving a limitation of the one before it:

| Evaluator | Problem | Design solution | Consequence |
|---|---|---|---|
| `NaiveEtaEvaluator` | The defining Dirichlet series for zeta only converges for `Re(s) > 1`, which excludes the entire critical strip. | Evaluate the Dirichlet eta function instead — an alternating series with the same terms, convergent for `Re(s) > 0`. | Correct on the critical strip, but slow: reaching machine precision near the critical line needs on the order of `10^15` terms, per the alternating-series error bound documented on `EtaFunction`. Usable only as a cross-check against small term counts, never as the primary evaluator. |
| `AcceleratedEtaEvaluator` | The naive evaluator's convergence is too slow to use for real work. | Apply `AlternatingSeriesAccelerator` to the same eta series, reaching machine precision in a few dozen terms instead of quadrillions. | Fast and accurate for `Re(s) > 0`, but still cannot reach `Re(s) <= 0` — which is where the trivial zeros and most of the interesting analytic structure live. |
| `ContinuedZetaEvaluator` | Neither prior evaluator is defined for `Re(s) <= 0`. | Apply the functional equation to reflect an evaluation at `s` with `Re(s) <= 0` back onto an evaluation at `1 - s`, where `Re(s) > 0` and the accelerated evaluator applies, then delegate to it. | A single evaluator usable across the full complex plane (aside from the pole at `s = 1`), built entirely out of the two simpler evaluators rather than a new algorithm. This is the evaluator every other package (`zeros`, `analysis`, `viz`) is written against. |

Each evaluator is a small, independently testable unit (see
`ZetaEvaluatorContractTest`, which every evaluator is verified against),
and the final evaluator is a composition of the earlier two rather than
a rewrite — a direct consequence of the immutability and statelessness
principle in Section 4: composing evaluators by delegation is safe
exactly because none of them hold mutable state that composition could
corrupt.

## 6. Detection vs. Refinement

Finding a zero is split into two distinct classes with different cost
and accuracy characteristics, rather than one "find a zero" operation:

- **`CriticalLineScanner`** walks the critical line at a fixed step
  size, looking for local minima in `|zeta(1/2 + it)|` that drop enough
  to plausibly be a zero rather than noise. This is cheap per point
  (one evaluation) but only approximate: it produces a `ZeroCandidate`
  — a height estimate, not a verified zero.
- **`ZeroRefiner`** takes a candidate and polishes it to machine
  precision using a free-form complex root finder (Section 7), which is
  expensive per candidate (many evaluations and derivative estimates)
  but accurate to roughly fifteen decimal digits.

Separating these lets the expensive step run only on the small number
of candidates the cheap step identified, instead of running expensive
root-finding at every sampled point along the line. `ZeroFinderDemo`
prints exactly this cost breakdown — zeta evaluations spent on
detection versus refinement — for a live run; the shape of the result
is consistent across runs even though the exact counts depend on the
scan range and step size, so it is reproducible but not reproduced here
as a fixed number.

## 7. Root-Finding Without a Critical-Line Constraint

The root finder `ZeroRefiner` delegates to is free to move anywhere in
the complex plane. Nothing in its implementation encodes:

```text
Re(s) = 0.5
```

as a requirement, a starting assumption, or a projection applied to its
output. It is handed a starting point (either a scanner's height
estimate on the line, or, in `ZeroFinderDemo`'s off-line experiment, a
point deliberately displaced a fifth of the way across the critical
strip) and it converges wherever the mathematics takes it.

This is a deliberate architectural choice, not an oversight: if the real
part of a converged root were constrained or nudged toward `0.5` by the
implementation, landing on the critical line would be a foregone
conclusion built into the code rather than a result of it. Because the
constraint is absent, the real part of every `RefinedZero` is a measured
output — `deviationFromCriticalLine()` on that value is a computed
quantity, not a tautology. This is also why the disclaimer in Section 1
holds regardless of how many zeros the program locates: agreement with
`Re(s) = 0.5` here is numerical evidence for a range of heights,
produced by an implementation that could in principle have shown
otherwise, and it stops being evidence of anything the moment someone
reads it as more than that.

## 8. Validation Philosophy

The `validation` package strictly follows:

```text
computation → result → validation
```

and never:

```text
known answer → computation → forced result
```

`KnownZetaValue` and `KnownZeroCatalog` hold published reference data,
but no class in `zeta`, `zeros`, or `analysis` ever reads from either.
Reference data is consumed exclusively by `validation` package classes
(`PrecisionChecker`, `ZetaAccuracyChecker`) and by `app.cli` demo
classes, always as the last step, always for comparison against a
result that was already produced independently. `ZeroFinderDemo` is the
clearest example: its scan and refinement pipeline runs to completion
using only the evaluator and the root finder, and `REFERENCE_ZEROS`
enters the program only in the printed comparison table afterward. This
ordering is what makes the numerical agreement in Section 7 meaningful:
the published heights could not have influenced the search that found
them.

## 9. Observability

- **Metrics (Micrometer).** `CriticalLineScanner` records, per scan
  invocation, an evaluations counter (`METRIC_EVALUATIONS`), a duration
  timer (`METRIC_DURATION`), and counters for minima found and
  discarded (`METRIC_MINIMA_FOUND`, `METRIC_MINIMA_DISCARDED`), each
  tagged with `TAG_MODE` as either `MODE_SEQUENTIAL` or
  `MODE_PARALLEL`. `ScanMetricsReporter` reads these back into a
  `ScanMetricsSummary` per mode, using `MeterRegistry.find()` rather
  than `.get()` because summarizing a mode that was never scanned is a
  normal case, not an error condition.
- **Logging (SLF4J + Logback, with MDC).** Scan activity is logged with
  the active mode (`sequential` or `parallel`) attached via MDC, so log
  lines from concurrent sequential and parallel runs stay
  distinguishable without threading mode information through every log
  call by hand.

Together these answer the same question two ways: metrics for anything
consuming scan results programmatically (a benchmark, a dashboard),
logs for a human reading a single run's output.

## 10. Visualization Architecture

- **`PlaneRegion`** and **`PlotLayout`** are the coordinate-mapping
  value objects for the two renderers: `PlaneRegion` maps a rectangular
  region of the complex plane onto image pixels for domain coloring,
  and `PlotLayout` maps a height/magnitude range onto image pixels for
  the critical-line line plot. Neither renderer contains coordinate
  arithmetic inline — it is isolated in these two types so the
  numerical mapping can be tested (`PlaneRegionTest`, `PlotLayoutTest`)
  independently of anything about pixels or `Graphics2D`.
- **`DomainColourRenderer`** evaluates the supplied `ZetaEvaluator`
  once per pixel and renders the row loop in parallel
  (`IntStream.range(...).parallel()`). This is safe with no
  synchronization for two reasons that both trace back to Section 4:
  every evaluator is stateless and thread-safe by contract, and each
  parallel row writes to disjoint pixels in the output image, so there
  is no shared mutable state between threads to protect. The stream's
  terminal operation establishes the happens-before relationship that
  makes the writes visible once rendering completes.
- **`CriticalLinePlotRenderer`** draws `|zeta(1/2 + it)|` as a 2D line
  plot, with independently-established zero heights (not zeros the
  renderer itself infers) marked as vertical lines — a deliberate
  separation of "what the curve looks like" from "which points are
  confirmed zeros," consistent with the detection/refinement split in
  Section 6.

## 11. Key Architectural Decisions

| Decision | Reason |
|---|---|
| Immutable, stateless evaluators | Safe concurrent reuse across parallel scans and parallel rendering, with no locking |
| Separate scanning and refinement | Different cost/accuracy characteristics — cheap coarse detection, expensive precise polishing |
| No critical-line constraint in the root finder | Avoids embedding the hypothesis into the algorithm; landing on the line is an observed result |
| Validation separated from computation | Prevents known reference values from influencing the search that produces results to check |
| Domain/pixel coordinate separation | Keeps numerical mapping logic (`PlaneRegion`, `PlotLayout`) independently testable from rendering |
| No dependency-injection framework | Explicit constructor wiring is sufficient at this scale and keeps dependencies visible |

## 12. Testing Strategy

- **Contract tests** (`ZetaEvaluatorContractTest`,
  `ComplexRootFinderContractTest`) verify behaviour every
  implementation of an interface must satisfy, run once against each
  implementation, so a new evaluator or root finder is checked against
  the same rules as the existing ones automatically.
- **Unit tests** cover individual classes — value object validation and
  accessors, single-method behaviour — and follow JUnit 5's nested
  `@Nested` class convention to group related cases (as seen throughout
  `validation` and `zeros` test classes).
- **Integration-style tests** exist where a unit boundary would miss
  real behaviour: `CriticalLineScannerParallelCorrectnessTest` checks
  that sequential and parallel scans agree, and
  `CriticalLineScannerMdcTaggingTest` checks the actual log output
  carries the right mode tag.
- **Demos (`app.cli`) and benchmarks (`bench`) are deliberately
  untested** by JUnit. They are wiring and measurement code, not
  behaviour to verify: a demo's job is to call already-tested classes
  and print results, and a benchmark's job is to measure throughput of
  already-tested code, not to encode new logic worth a regression test.

## 13. Non-Goals

RiemannLab intentionally does not attempt:

- **Arbitrary-precision arithmetic** beyond what standard `double`
  complex arithmetic provides. Precision is bounded by IEEE 754 double
  precision throughout; this is sufficient for the fifteen-or-so digit
  agreement demonstrated against published zero heights, and extending
  it further was never a project goal.
- **A graphical user interface.** Output is images written to disk and
  text printed to the console; there is no interactive UI.
- **Distributed or multi-machine computation.** Parallelism is
  single-JVM, stream-based parallelism only.
- **Any formal proof, partial or otherwise, of the Riemann Hypothesis.**
  Restated here because it bears repeating: nothing in this codebase's
  design, however many zeros it locates or how precisely it locates
  them, constitutes progress toward proving the hypothesis.