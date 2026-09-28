# Math Background

This document explains, in plain English, the mathematics behind
RiemannLab's code. It is written for a reader comfortable with code but
not assumed to have a background in analytic number theory — the same
spirit the rest of this project was built in. It complements
[ARCHITECTURE.md](ARCHITECTURE.md), which explains how the code is
organized; this explains why the code computes what it computes.

## 1. Purpose & Disclaimer

RiemannLab numerically evaluates the Riemann zeta function and searches
for its non-trivial zeros. This document explains the mathematics that
makes that possible. **It is not a proof of anything, and neither is the
software it describes.** The Riemann Hypothesis remains an open problem.
Nothing here, however many digits of agreement it demonstrates with
published results, moves that problem toward resolution. Numerical
agreement is evidence that the software is correct; it is not evidence
toward the hypothesis itself.

## 2. The Riemann Zeta Function

The zeta function is defined, for a complex number `s`, as an infinite
sum:

```text
zeta(s) = 1/1^s + 1/2^s + 1/3^s + 1/4^s + ...
```

For real values of `s` greater than 1, this is a sum of shrinking
positive terms that converges to a finite number. A concrete example:
at `s = 2`,

```text
zeta(2) = 1/1 + 1/4 + 1/9 + 1/16 + ... = pi^2 / 6 ~= 1.6449
```

— a famous result (the "Basel problem"). But at `s = 1`, the sum is
`1 + 1/2 + 1/3 + 1/4 + ...`, the harmonic series, which grows without
bound. The defining series simply does not converge for `Re(s) <= 1`,
which means it cannot be used, as written, to compute `zeta(s)` for most
of the values RiemannLab cares about. Everything in Sections 4–6 exists
to solve exactly this problem.

`s` throughout this document (and this codebase) is a **complex**
number, written `s = a + bi`, where `a` is the real part (`Re(s)`) and
`b` is the imaginary part (`Im(s)`). "`Re(s) > 1`" means the real part
of `s` is greater than 1 — `s` can still have any imaginary part at all.

## 3. The Critical Strip and the Critical Line

The **critical strip** is the region of the complex plane where
`0 <= Re(s) <= 1`. It matters because it is known (proven, not
conjectured) that every non-trivial zero of zeta lies in this strip. The
**critical line** is the single vertical line down its middle, where
`Re(s) = 1/2` exactly. The Riemann Hypothesis (Section 11) is a claim
about that one line: that every non-trivial zero lies precisely on it,
not merely somewhere in the strip around it.

## 4. The Dirichlet Eta Function

The Dirichlet eta function is the same series as zeta, with alternating
signs:

```text
eta(s) = 1/1^s - 1/2^s + 1/3^s - 1/4^s + ...
```

Alternating series converge under much weaker conditions than series of
all-positive terms — the eta series converges for `Re(s) > 0`, a region
that includes the entire critical strip. This single change of sign is
what makes any computation on the critical strip possible at all, and it
is why `EtaFunction` and `NaiveEtaEvaluator` compute this series rather
than zeta's own defining sum. Zeta and eta are related by a simple
formula for `Re(s) > 0` (excluding `s = 1`, where zeta has a pole):

```text
zeta(s) = eta(s) / (1 - 2^(1-s))
```

so computing eta accurately on the critical strip is enough to recover
zeta there too.

## 5. Convergence Acceleration

Even though the eta series converges for `Re(s) > 0`, it converges
*slowly* near the critical line — Section 10 makes this precise. Summing
raw terms one at a time is not practical.

`AlternatingSeriesAccelerator` speeds this up using an idea common to
many acceleration techniques for alternating series: instead of trusting
any single partial sum, repeatedly average adjacent partial sums against
each other. Each round of averaging cancels out more of the oscillation
between "too high" and "too low" that alternating terms produce, and the
sequence of averages converges to the true answer far faster than the
original series does. The result is the same mathematical value the raw
series would eventually reach — the accelerator does not change what is
being computed, only how quickly the computation gets close to it.

## 6. Analytic Continuation via the Functional Equation

**Analytic continuation** is the general idea of extending a function's
definition beyond the region where its original formula works, in a way
that: (a) agrees exactly with the original formula wherever both are
defined, and (b) stays as smooth (complex-differentiable) as possible
everywhere it's extended to. Zeta has a unique such extension to the
entire complex plane except for a single pole at `s = 1` — meaning there
is exactly one sensible way to define "zeta" outside `Re(s) > 1`, not
several competing choices.

The tool that provides this extension is the **functional equation**, a
classical identity relating `zeta(s)` to `zeta(1 - s)`:

```text
zeta(s) = 2^s * pi^(s-1) * sin(pi*s/2) * Gamma(1-s) * zeta(1-s)
```

The practical consequence, and the one `ContinuedZetaEvaluator` relies
on: if `Re(s) <= 0`, then `Re(1 - s) >= 1`, which is comfortably inside
the region the eta relation (Section 4) and the accelerated evaluator
already handle correctly. So to evaluate zeta anywhere `Re(s) <= 0`,
`ContinuedZetaEvaluator` evaluates at `1 - s` instead — where it already
knows how to get an accurate answer — and applies the functional
equation's formula to translate that answer back to the original point
`s`. This is why the project needed no new evaluation algorithm to cover
the whole plane: it needed only this reflection, built on top of the
evaluator it already had.

## 7. The Gamma Function

The gamma function, `Gamma(z)`, generalizes the factorial to numbers
that aren't positive integers: for a positive integer `n`,
`Gamma(n) = (n-1)!`. It shows up in the functional equation above simply
because that is where the classical derivation of the equation puts it
— `GammaFunction` exists in this codebase because `ContinuedZetaEvaluator`
cannot apply the functional equation without it. Nothing else in this
project depends on the gamma function directly.

## 8. Trivial vs. Non-Trivial Zeros

A **zero** of zeta is any `s` where `zeta(s) = 0`. Two kinds exist:

- **Trivial zeros**, at `s = -2, -4, -6, ...` (every negative even
  integer). These fall directly out of the functional equation: the
  `sin(pi*s/2)` factor is exactly zero at these points, forcing the
  whole right-hand side to zero. They are well understood, infinite in
  number, and not interesting for this project.
- **Non-trivial zeros**, which lie in the critical strip described in
  Section 3. These are the zeros RiemannLab's scanner and refiner search
  for, and the ones the Riemann Hypothesis makes a claim about.

## 9. Root Finding

A **root finder** is an algorithm that, given a function and a starting
point, searches for an input where the function's output is zero.
`ZeroRefiner` uses a secant-method-based finder: rather than requiring
the function's exact derivative (as Newton's method does), it estimates
the local slope from two nearby points it has already evaluated, and
uses that estimate to guess where the function crosses zero next. It
repeats this, using its two most recent guesses each time, until the
guesses stop changing meaningfully.

Critically, as [ARCHITECTURE.md](ARCHITECTURE.md#7-root-finding-without-a-critical-line-constraint)
explains from the software side, this root finder is never told to stay
on `Re(s) = 0.5`. It searches the full complex plane from wherever it is
started. That every zero it finds converges onto the critical line
regardless is a measured outcome, not a built-in assumption.

## 10. Precision and Error Bounds

The **alternating series estimation theorem** gives a simple, useful
guarantee: for an alternating series whose terms shrink toward zero, the
error after stopping at `N` terms is smaller than the size of the very
next term, term `N + 1`. For the eta series, that next term has
magnitude `(N+1)^(-Re(s))`. On the critical line, where `Re(s) = 1/2`,
this works out to roughly `1/sqrt(N)` — meaning to gain one more decimal
digit of accuracy, `N` must grow by a factor of 100. This is precisely
why `NaiveEtaEvaluator`, summing raw terms, needs on the order of
`10^15` terms to reach machine precision near the critical line, and why
`AlternatingSeriesAccelerator` — which reaches the same precision in a
few dozen terms — is not an optimization but a necessity for any
practical use of this project.

This error bound is also the basis for the `validation` package's
accuracy checks (`PrecisionChecker`, `ZetaAccuracyChecker`): they verify
that the evaluators' actual errors against known values stay within the
bounds this theorem predicts, not merely that the answers "look close."

## 11. The Riemann Hypothesis, Precisely Stated

The Riemann Hypothesis states:

> Every non-trivial zero of the Riemann zeta function has real part
> exactly equal to `1/2`.

It was conjectured in 1859 and remains unproven. It is one of the
seven Millennium Prize Problems.

**What RiemannLab does:** locates individual non-trivial zeros within a
scanned height range, refines them to roughly fifteen decimal digits of
precision using a root finder unconstrained to the critical line
(Section 9), and measures — for each zero it finds — how close its real
part landed to `1/2`.

**What RiemannLab does not do, and cannot do:** prove that *every*
non-trivial zero (there are infinitely many) lies on the critical line.
No finite computation can establish a universal claim over an infinite
set. Locating some number of zeros on the line, however large, narrows
nothing about the zeros that were never scanned. This is not a
limitation specific to this project's implementation — it is why the
Riemann Hypothesis is still open after more than a century of numerical
verification by many independent efforts, this one included in spirit
only. Numerical agreement is evidence the software works correctly. It
is not, and cannot become, a proof.