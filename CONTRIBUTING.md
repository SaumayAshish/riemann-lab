# Contributing to RiemannLab

RiemannLab is a computational research and visualization project for
numerically evaluating the Riemann zeta function and investigating its
non-trivial zeros. It does not claim, and no contribution to it may
claim, to prove or disprove the Riemann Hypothesis.

This document explains how to build the project, the conventions its
code follows, and how to propose a change.

## Getting Started

Requirements: JDK 25, Maven.

```bash
git clone https://github.com/SaumayAshish/riemann-lab.git
cd riemann-lab
mvn clean verify
```

`mvn clean verify` compiles the project, runs the full JUnit 5 test
suite, and packages the jar. A change is not ready for review unless
this command succeeds with all tests passing.

To regenerate the JavaDoc site and check for warnings:

```bash
mvn javadoc:javadoc
```

## Understanding the Project Before Changing It

Read these before making a non-trivial change:

- [ARCHITECTURE.md](ARCHITECTURE.md) — package structure, dependency
  direction, and the design decisions behind the code.
- [MATH_BACKGROUND.md](MATH_BACKGROUND.md) — the mathematics the code
  implements, in plain English.

A change that doesn't fit the layering described in ARCHITECTURE.md's
dependency rule (lower-level packages never depend on higher-level
ones) will be asked to be restructured before it's merged.

## Code Conventions

- **Immutable value objects.** Data-carrying types are Java `record`s.
  No setters.
- **Meaningful names over comments.** Prefer a clearer method or
  variable name to a comment explaining an unclear one.
- **No magic numbers.** Any numeric literal with meaning is a named
  constant.
- **JavaDoc on public mathematical APIs.** Every public class, method,
  constructor, and record component that doclint would flag needs a
  real doc comment — not just enough to silence the warning. Run
  `mvn javadoc:javadoc` before opening a PR; it should report zero
  warnings.
- **No unnecessary frameworks.** This project uses Maven, JUnit 5,
  SLF4J/Logback, and Micrometer, and nothing else by default. A PR
  adding a new dependency should explain, in its description, why the
  existing stack can't do the job.
- **Stateless, thread-safe evaluators.** Any class implementing
  `ZetaEvaluator` must be stateless and safe to call concurrently from
  multiple threads without synchronization — this is relied on by
  parallel scanning and parallel rendering. Document this explicitly in
  the class's JavaDoc.

## Testing

- Every class gets JUnit 5 tests **except** pure wiring/entry-point
  classes (`app.cli` demos) and benchmark classes (`bench`), which are
  measured, not unit-tested.
- Use `@Nested` classes to group related test cases, following the
  existing test suite's style (see any class under `src/test/java`).
- If you're implementing an existing interface (`ZetaEvaluator`,
  `ComplexRootFinder`), its behavior is checked by a shared contract
  test (`ZetaEvaluatorContractTest`, `ComplexRootFinderContractTest`).
  New implementations should pass against that contract test, not just
  a set of ad hoc cases.

## Commit Messages

Write commit messages that explain *why* a change was made, not just
*what* changed. Keep the summary line concise; use the body for
detail if needed.

## Submitting a Change

1. Fork the repository and create a branch from `main`.
2. Make your change, following the conventions above.
3. Run `mvn clean verify` and `mvn javadoc:javadoc` — both must be
   clean.
4. Open a pull request using the provided template. Fill in every
   section; an incomplete template will be sent back before review.

## Reporting Bugs or Proposing Features

Use the issue templates provided when opening a new issue — they'll be
offered automatically when you click "New Issue" on GitHub.