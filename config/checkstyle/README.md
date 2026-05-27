# Checkstyle Configuration — Teaching Guide

This project uses [Checkstyle](https://checkstyle.org/) to enforce code-quality conventions at build time. The ruleset (`config/checkstyle/checkstyle.xml`) is intentionally focused: about 25 rules covering naming, imports, structure, and common bug patterns. It is **not** a full style guide — formatting choices like indentation depth are not enforced.

The pedagogical goal is to highlight **real correctness problems** (missing braces, broken naming, fall-through, `==` on strings), not flood students with formatting warnings.

## Running Checkstyle

```bash
# Lint main sources
./gradlew checkstyleMain

# Lint test sources
./gradlew checkstyleTest

# Both (also runs SpotBugs and the test suite)
./gradlew check
```

Reports land at `build/reports/checkstyle/main.html` and `build/reports/checkstyle/test.html`. Open them in a browser to see file/line-level violations.

> Violations currently do **not** break the build (`isIgnoreFailures = true` in `build.gradle.kts`). Flip it to `false` to enforce — student PRs would then need to be clean to merge.

---

## File-level rules

These run on the raw file before parsing.

### `FileTabCharacter`
Forbids tab characters as indentation. Mixed tabs/spaces render inconsistently across editors and break diffs.

### `LineLength` (max 120)
Limits each line to 120 characters. Long lines force horizontal scrolling and hurt diff readability. `import` lines and URLs are exempt.

---

## Naming conventions

Standard Java casing — broken naming is a frequent cause of code-review confusion.

| Rule | Applies to | Required pattern |
|------|------------|------------------|
| `TypeName` | classes, interfaces, enums, records | `PascalCase` |
| `MethodName` | methods | `camelCase` |
| `ConstantName` | `static final` fields | `UPPER_SNAKE_CASE` |
| `MemberName` | non-static fields | `camelCase` |
| `ParameterName` | method parameters | `camelCase` |
| `LocalVariableName` | local variables | `camelCase` |
| `PackageName` | packages | `lower.dot.separated` |

```java
// FAIL
public class user_service { }
private static final int maxRetries = 3;

// PASS
public class UserService {
    private static final int MAX_RETRIES = 3;
}
```

---

## Imports

### `AvoidStarImport`
Forbids `import java.util.*;`-style wildcards. Wildcards hide actual dependencies and cause silent name conflicts when packages add classes in new versions.

### `UnusedImports`
Flags imports nothing in the file references. Dead imports lie about a file's dependencies.

### `RedundantImport`
Flags re-imports of classes already imported, or imports from `java.lang`.

---

## Blocks and braces

### `EmptyBlock` (text mode)
Every `{}` must contain at least a comment. A silent empty block usually means "TODO I forgot" — force the author to either fill it or document why.

```java
// FAIL
catch (IOException e) { }

// PASS
catch (IOException e) { /* file is optional, swallow */ }
```

### `NeedBraces`
Single-statement `if` / `else` / `for` / `while` must still use braces. The Apple "goto fail" bug came from braceless blocks — always brace.

```java
// FAIL
if (x > 0) doSomething();

// PASS
if (x > 0) {
    doSomething();
}
```

### `LeftCurly` / `RightCurly`
Brace placement consistency: `{` on the same line as the declaration, `}` aligned and (for control flow) on its own line.

---

## Whitespace

### `WhitespaceAround`
Operators and keywords must have spaces around them. `if(x>0)` is harder to scan than `if (x > 0)`.

### `GenericWhitespace`
No internal padding in generics. `List<String>`, not `List< String >`.

### `MethodParamPad`
No space between method name and `(`. `foo()`, not `foo ()`.

### `ParenPad`
No padding inside `()`. `(x)`, not `( x )`.

---

## Modifiers

### `ModifierOrder`
Enforces the JLS-recommended order: `public protected private abstract default static final transient volatile synchronized native strictfp`.

```java
// FAIL
static public final int MAX = 10;

// PASS
public static final int MAX = 10;
```

### `RedundantModifier`
Flags implicit modifiers. Methods on `interface` are already `public abstract` — declaring them so adds noise.

---

## Coding problems

These catch real bugs and dangerous patterns — the highest-value group for students to learn.

### `EmptyStatement`
Forbids stray `;`. A lone semicolon after `if (x > 0);` makes the body unconditional. Silent bug.

### `EqualsHashCode`
If you override `equals`, you must override `hashCode` (and vice versa). The contract is required for `HashMap` / `HashSet` to work — violating it produces lookups that "lose" entries that are clearly present.

### `OneStatementPerLine`
One statement per line. `int a = 1; int b = 2;` on one line hides intent and breaks debugger step-over.

### `MissingSwitchDefault`
Every `switch` must have a `default` branch. Forces the author to think about the "none of the above" case.

### `FallThrough`
A non-empty `case` must end with `break` / `return` / `throw`, or be explicitly commented as intentional fall-through. Implicit fall-through is one of Java's most common bug sources.

```java
// FAIL
case 1:
    doX();
case 2:
    doY();
    break;

// PASS
case 1:
    doX();
    break;
case 2: // fall through
case 3:
    doY();
    break;
```

### `StringLiteralEquality`
Forbids `string == "literal"`. `==` checks reference identity, not content. Use `.equals()`.

```java
// FAIL
if (status == "ACTIVE") { ... }

// PASS
if ("ACTIVE".equals(status)) { ... }
```

### `SimplifyBooleanExpression`
Flags constructs like `if (x == true)` or `if (!y == false)`. Just write `if (x)`.

### `SimplifyBooleanReturn`
Flags `if (cond) return true; else return false;` — just `return cond;`.

---

## Style

### `ArrayTypeStyle`
`int[] arr`, not `int arr[]`. The `int[]` form keeps the type information together.

### `UpperEll`
`long x = 100L;`, not `100l`. Lowercase `l` is visually indistinguishable from `1`.

---

## Suppressing a single violation

If a rule must be ignored at one specific spot, use Checkstyle's suppression annotation. Note: this requires adding `SuppressWarningsHolder` and `SuppressWarningsFilter` modules to the config (not currently enabled — prefer fixing the violation).

```java
@SuppressWarnings("checkstyle:LineLength")
private static final String LONG_LITERAL = "...";
```

---

## Why this ruleset and not Google's?

Google's official Checkstyle config (`google_checks.xml`) enforces around 150 rules including 2-space indentation, mandatory Javadoc on public members, and a fixed import order. It is excellent for a long-lived production codebase but counter-productive in a 3-hour code challenge:

- **Indentation conflict**: Google requires 2-space indentation; the project uses 4-space. Every file would generate dozens of warnings on day one.
- **Javadoc noise**: Google requires Javadoc on every public class/method. Students would see hundreds of `MissingJavadocMethod` warnings on controllers and records before writing any business logic — drowning out the bug-class warnings (naming, unused imports, missing braces) we actually want them to learn from.

This focused config keeps the high-signal rules and drops the noise. Promote students to Google's config in advanced courses where Javadoc and formatting matter.
