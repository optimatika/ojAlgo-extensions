# Plan: HiGHS Native Solver Integration via FFM API

Direct native binding to HiGHS using Java 25 FFM API, bundling all platform binaries in one JAR. Implements `UpdatableSolver` for in-place bound updates (enables efficient branch-and-bound with HiGHS as the relaxation solver).

**Supported problem types:**
- **LP** — Linear Programming ✓
- **MIP** — Mixed-Integer Programming (LP + integers) ✓
- **QP** — Quadratic Programming (convex quadratic objective, linear constraints) ✓ (well-conditioned problems)
- **MIQP** — Mixed-Integer QP ✗ (HiGHS does not support integers with quadratic objective)
- **QCQP** — Quadratically Constrained QP ✗ (HiGHS does not support quadratic constraints)

**QP Performance Note:** HiGHS is primarily an LP/MIP solver. Its QP support works well for well-conditioned problems but may return UNSTABLE for numerically challenging QP instances. For difficult QP problems, consider Clarabel4j or ojAlgo's native QP solver.

Legend: [x] done, [ ] pending, [~] partial/in progress

Cross-reference: `.github/copilot-instructions.md`

## Current Status (2026-02-20)

**Implementation complete.** All 6 basic tests pass. Benchmarks show correct results for LP/MIP and well-conditioned QP.

### Files Created

| File | Description | Status |
|------|-------------|--------|
| `HiGHSBindings.java` | FFM bindings for HiGHS C API | ✓ Complete |
| `NativeLoader.java` | Platform detection, library extraction, SymbolLookup | ✓ Complete |
| `SolverHiGHS.java` | Integration, Configurator, EntityMap, UpdatableSolver | ✓ Complete |
| `SolverHiGHSTest.java` | Basic LP/MIP/QP tests | ✓ 6/6 passing |
| `pom.xml` | Updated with `--enable-native-access=ALL-UNNAMED` | ✓ Complete |

### Native Libraries Bundled (from conda-forge)

| Platform | Library | Size | Status |
|----------|---------|------|--------|
| `macos-arm64` | `libhighs.dylib` | 3.9 MB | ✓ From conda-forge |
| `macos-x64` | `libhighs.dylib` | 4.9 MB | ✓ From conda-forge |
| `linux-x64` | `libhighs.so` | 5.1 MB | ✓ From conda-forge |
| `linux-arm64` | `libhighs.so` | 4.7 MB | ✓ From conda-forge |
| `linux-ppc64le` | `libhighs.so` | 6.1 MB | ✓ From conda-forge |
| `windows-x64` | `highs.dll` | 8.3 MB | ✓ From conda-forge |

**Total JAR size (native libs only): ~33 MB**

### Update Script

Run `./scripts/update-highs-binaries.sh [VERSION]` to download all platform binaries from conda-forge.

## Next Steps

1. **[ ] Add more comprehensive tests**
   - Extend existing ojAlgo test suites (Netlib, Maros-Meszaros, etc.)
   - Test UpdatableSolver functionality (re-solve after bound changes)

2. **[ ] Verify QP support**
   - Test quadratic objectives more thoroughly
   - Ensure Hessian is passed correctly in lower-triangular CSC format

3. **[ ] Consider module-info.java**
   - Currently not using JPMS modules (consistent with other extensions)

4. **[ ] Test on other platforms**
   - Verify Linux and Windows binaries work correctly

## Lessons Learned

### HiGHS Native Library Sources

**Problem**: HiGHS GitHub releases only provide **static** libraries (`.a`, `.lib`) for most platforms. FFM requires shared libraries (`.dylib`, `.so`, `.dll`).

**Solution**: Use **conda-forge** packages which include pre-built shared libraries for all platforms:

```bash
# Download (no conda installation required)
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/osx-arm64/highs-1.13.1-h3feff0a_0.conda"

# Extract (.conda is a zip containing a .tar.zst)
unzip package.conda
zstd -d pkg-*.tar.zst -o pkg.tar
tar -xf pkg.tar

# Libraries are in:
# - lib/libhighs.1.13.dylib (macOS)
# - lib/libhighs.so.1.13.1 (Linux)
# - Library/bin/highs.dll (Windows)
```

Browse versions at: https://anaconda.org/conda-forge/highs/files

**Alternative**: Build from source with `-DBUILD_SHARED_LIBS=ON` (requires CMake and native toolchain).

### FFM Native Access Warning

Java 25 FFM requires `--enable-native-access=ALL-UNNAMED` to suppress warnings. Added to `pom.xml`:
```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>--enable-native-access=ALL-UNNAMED</argLine>
  </configuration>
</plugin>
```

### Arena Choices

- **Solver lifetime**: `Arena.ofAuto()` — GC-managed, no explicit close needed, supports re-solve
- **Problem data arrays**: Short-lived `Arena.ofConfined()` within `build()` — HiGHS copies data internally

### Test Expectations

- ojAlgo presolve may detect infeasibility before reaching HiGHS (e.g., `x.lower(2).upper(1)`)
- Use constraint-based infeasibility for tests that should reach the solver

### CSC Array Lengths

HiGHS expects `a_start` and `q_start` arrays of length `num_col` (not `num_col + 1` as in standard CSC). The end of the last column is determined by the `num_nz` / `q_num_nz` parameters.

### Hessian Construction

HiGHS expects the **lower triangular** part of the symmetric Hessian Q for `minimize ½ x' Q x + c' x`.

ojAlgo's `Expression.getQuadraticKeySet()` returns stored entries which may be:
- Only `(i,j)` for term `c * x_i * x_j`
- Only `(j,i)` for the same term
- Both `(i,j)` and `(j,i)` with values that sum to the coefficient

Following the pattern from `AlternatingDirectionSolver.setInP()`:
1. Map each entry to the canonical lower triangular position `(max(r,c), min(r,c))`
2. Double diagonal entries (they appear once in the key set but need doubling for the `½ x' Q x` formulation)
3. Off-diagonal entries from both `(i,j)` and `(j,i)` accumulate via `merge(..., Double::sum)`

## Steps

### 1. [x] Create `HiGHSBindings.java`

FFM bindings via `Linker.nativeLinker()` for HiGHS C functions:

- `Highs_create` → `MemorySegment` (pointer to HiGHS instance)
- `Highs_destroy` → void
- `Highs_passLp` → int (pass LP in CSC format)
- `Highs_passMip` → int (pass MIP in CSC format)
- `Highs_passHessian` → int (pass QP Hessian in half-CSC format)
- `Highs_run` → int (solve)
- `Highs_getSolution` → int (extract primal/dual solution)
- `Highs_getModelStatus` → int
- `Highs_setDoubleOptionValue` → int
- `Highs_setIntOptionValue` → int
- `Highs_setStringOptionValue` → int
- `Highs_changeColBounds` → int (for `UpdatableSolver.updateRange()`)

Use `Arena.ofAuto()` for solver lifetime memory.

**Note:** Bindings are hand-written based on `highs_c_api.h`. The jextract tool could auto-generate these, but it has limited platform availability (not available for macOS). The manual approach works well and provides full control over which functions to expose.

### 2. [x] Create `NativeLoader.java`

- Detect OS/arch at runtime (`os.name`, `os.arch`)
- Extract matching binary from `resources/native/{os-arch}/libhighs.*` to temp directory
- Return `SymbolLookup.libraryLookup(path, Arena.ofAuto())` for use by bindings
- Support platforms: `macos-arm64`, `macos-x64`, `linux-x64`, `linux-arm64`, `windows-x64`

### 3. [x] Implement `SolverHiGHS.java`

Instance fields:
- `MemorySegment myHighs` — pointer to HiGHS instance
- `int myNumCol` — number of columns (variables)
- `int myNumRow` — number of rows (constraints)
- `HiGHSEntityMap myEntityMap` — variable index mapping (for `UpdatableSolver`)

`Integration.build()`:
- Extract free variables and fixed variables from model
- Build CSC constraint matrix (values, row indices, column pointers)
- Build cost vector, variable bounds, constraint bounds
- Call `Highs_passLp` or `Highs_passMip` based on integrality
- If quadratic objective, call `Highs_passHessian` with lower-triangular Hessian
- Build and store `EntityMap` for variable index mapping
- Return `SolverHiGHS` instance

`Integration.isCapable()`:
- Return `true` (HiGHS handles LP, MIP, and convex QP)

`Integration.toModelState()` / `toSolverState()`:
- Use `expandFreeToFull` / `reduceFullToFree` helper methods

`solve()`:
- Call `Highs_run`
- Extract status via `Highs_getModelStatus`
- Translate to `Optimisation.State`
- Extract solution via `Highs_getSolution`
- Return `Optimisation.Result`

`dispose()`:
- Call `Highs_destroy`

`UpdatableSolver` methods:
- `getEntityMap()`: Return stored `EntityMap`
- `fixVariable(index, value)`: Call `Highs_changeColBounds(index, value, value)`
- `updateRange(index, lower, upper)`: Call `Highs_changeColBounds(index, lower, upper)`

### 4. [x] Implement `Configurator`

Map `Optimisation.Options` to HiGHS settings:

| ojAlgo Option | HiGHS Option |
|---------------|--------------|
| `time_abort` | `time_limit` |
| `iterations_abort` | `simplex_iteration_limit` / `ipm_iteration_limit` |
| `feasibility` tolerances | `primal_feasibility_tolerance`, `dual_feasibility_tolerance` |
| `logger_appender` + `logger_detailed` | `output_flag` |

### 5. [x] Implement `translate()` helper

Map HiGHS `HighsModelStatus` codes to ojAlgo `Optimisation.State`:

| HiGHS Status | ojAlgo State |
|--------------|--------------|
| `kOptimal` | `OPTIMAL` |
| `kInfeasible` | `INFEASIBLE` |
| `kUnbounded` | `UNBOUNDED` |
| `kUnboundedOrInfeasible` | `INVALID` |
| `kObjectiveBound`, `kObjectiveTarget` | `APPROXIMATE` |
| `kTimeLimit`, `kIterationLimit` | `APPROXIMATE` |
| `kNotset`, `kLoadError`, `kModelError`, `kPresolveError`, `kSolveError`, `kPostsolveError` | `FAILED` |
| `kModelEmpty` | `UNEXPLORED` |

### 6. [~] Bundle platform binaries

Create directory structure in `src/main/resources/native/`:

```
native/
├── macos-arm64/
│   └── libhighs.dylib  ✓ (built from source, v1.13.1)
├── macos-x64/
│   └── libhighs.dylib  (use GitHub Actions)
├── linux-x64/
│   └── libhighs.so     (use GitHub Actions)
├── linux-arm64/
│   └── libhighs.so     (use GitHub Actions)
└── windows-x64/
    └── highs.dll       ✓ (from HiGHS release v1.13.1)
```

**Cross-platform build solution**: Use `.github/workflows/build-highs-native.yml` to build shared libraries on GitHub Actions runners for all platforms. The workflow:
- Builds HiGHS from source with `-DBUILD_SHARED_LIBS=ON` on native runners
- Downloads Windows DLL from HiGHS releases (only platform with pre-built shared lib)
- Uploads all libraries as artifacts for download

**Note**: HiGHS releases only provide static libraries for non-Windows platforms.

### 7. [x] Create test suite

Create tests in `src/test/java/org/ojalgo/optimisation/solver/highs/`:

- Extend existing ojAlgo LP/MIP/QP test cases
- Configure `ExpressionsBasedModel.addIntegration(SolverHiGHS.INTEGRATION)` in `@BeforeEach`
- Clear integrations in `@AfterEach`
- Verify solutions match expected results using `TestUtils`

## Further Considerations

### EntityMap Implementation

Implemented as inner class `HiGHSEntityMap`:
- Stores free variable count and equality/inequality constraint counts
- `countModelVariables()`: Return free variable count
- `countSlackVariables()`: Return 0 (HiGHS handles constraints internally)
- `indexOf(solverIndex)`: Map solver index to model variable index
- `isNegated(solverIndex)`: Return false (no negation used)
- `getSlack(ids)`: Return null (no explicit slack variables exposed)
- `getConstraint(idc)`: Returns `EntryPair<ModelEntity<?>, ConstraintType>`

### Cutting Planes

`generateCutCandidates()` returns empty collection by default. HiGHS generates cuts internally during MIP solve; exposing them to ojAlgo would require additional C API support not currently available.

### Arena Lifecycle

- Solver pointer: `Arena.ofAuto()` — allows GC-based cleanup, supports re-solve
- Problem data arrays: Short-lived `Arena.ofConfined()` within `build()` — HiGHS copies data internally, so arrays can be freed immediately after `Highs_passLp` returns

### QP Support

HiGHS supports convex QP via `Highs_passHessian()` after passing LP base:
- Extract quadratic objective terms from model
- Build lower-triangular Hessian in half-CSC format
- Call `Highs_passHessian` after `Highs_passLp`

### Arena Lifecycle

- Solver pointer: `Arena.ofAuto()` — allows GC-based cleanup, supports re-solve
- Problem data arrays: Short-lived `Arena.ofConfined()` within `build()` — HiGHS copies data internally, so arrays can be freed immediately after `Highs_passLp` returns

### QP Support

HiGHS supports convex QP via `Highs_passHessian()` after passing LP base:
- Extract quadratic objective terms from model
- Build lower-triangular Hessian in half-CSC format
- Call `Highs_passHessian` after `Highs_passLp`
