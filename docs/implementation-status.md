# DAGP Reliability Fixes — Implementation Status & Plan

**Date:** 2026-07-29  
**Plugin repo:** `~/repo/dependency-analysis-gradle-plugin`  
**Branches:** `main`, `LCP-61434-fix-false-positives`

---

## Current State

### Branch: `main`
- Cross-project transitive exposure filter (`FilterTransitiveExposureTask`) — **IMPLEMENTED**
- Handles both `api` AND `implementation` declared project deps
- Wired into `RootPlugin.kt` between `FilterAdviceTask` and `GenerateBuildHealthTask`
- Uses `AggregateTypeUsageReport` + `PublicTypes` for cross-project analysis

### Branch: `LCP-61434-fix-false-positives` (8 commits ahead of main, 1 behind)
- `TransitiveExposureFilter` — older version, only handles `api` deps (superseded by main)
- `RuntimeUsageFilter` — Spring/Liquibase runtime detection (**NOT on main**)
- `FindRuntimeDepsTask` — scans sources for @Bean, @ComponentScan, @Import (**NOT on main**)
- Unit tests for all three filters

---

## Fix Status Table

| # | Fix | Covers | Status | Location | Action Needed |
|---|-----|--------|--------|----------|---------------|
| 1 | Cross-project transitive check | 91% of FPs | ✅ Implemented on `main` | `FilterTransitiveExposureTask.kt` | **Test against ae** |
| 2 | Generated source wiring | ~308 suppressions | ❌ Not in plugin | ae build files | **Add `dependsOn` in ae for antlr/openapi tasks** |
| 3 | Exclusions config (runtime deps) | 3-5% | ✅ Built into DAGP DSL | ae's `build.gradle` config | **Configure in ae using `onUnusedDependencies { exclude(...) }`** |
| 4 | Runtime deps detection (Spring/DI) | Unknown | ✅ On branch only | `FindRuntimeDepsTask.kt`, `RuntimeUsageFilter.kt` | **Merge branch → main** |
| 5 | Annotation retention linking | 1-2% | ⚠️ Partially implemented | `asm.kt` detects retention | **Likely not needed — only 1 FP** |

---

## Action Plan

### Step 1: Merge runtime detection from branch to main

The branch has `FindRuntimeDepsTask` + `RuntimeUsageFilter` that detect:
- `@Bean` return types (Spring DI)
- `@ComponentScan` packages
- `@Import` class references
- Liquibase migration class references
- Spring XML `<bean class="...">` references

These need to be merged into `main` and wired into the pipeline alongside `FilterTransitiveExposureTask`.

**Files to cherry-pick/merge:**
```
src/main/kotlin/com/autonomousapps/tasks/FindRuntimeDepsTask.kt
src/main/kotlin/com/autonomousapps/internal/advice/RuntimeUsageFilter.kt
src/main/kotlin/com/autonomousapps/model/internal/intermediates/RuntimeDepsReport.kt
src/main/kotlin/com/autonomousapps/subplugin/ProjectPlugin.kt  (registration of FindRuntimeDepsTask)
src/main/kotlin/com/autonomousapps/internal/artifacts/DagpArtifacts.kt  (RUNTIME_DEPS kind)
src/test/kotlin/com/autonomousapps/internal/advice/RuntimeUsageFilterTest.kt
src/test/kotlin/com/autonomousapps/tasks/FindRuntimeDepsTaskTest.kt
```

**Wiring needed in `RootPlugin.kt`:**
- Add `runtimeDepsResolver` 
- Wire into `FilterTransitiveExposureTask` (or add a separate `FilterRuntimeDepsTask`)

### Step 2: Test the cross-project transitive filter against ae

Run `buildHealth` on the ae worktree and verify:
- The 80 known transitive FPs are now suppressed
- The 550 filter suppressions from our Python script are handled by the plugin
- No new false negatives (deps that should be flagged but aren't)

### Step 3: Configure exclusions in ae (build.gradle)

Already supported by DAGP DSL:
```groovy
dependencyAnalysis {
  issues {
    all {
      onUnusedDependencies {
        exclude(
          'com.appian.komodo:kafka-util',
          'io.micrometer:micrometer-registry-prometheus',
          'com.appian.sailintellij:shared',
          'org.gwtproject:gwt-dev',
          'org.glassfish.jersey.containers:jersey-container-jetty-servlet',
        )
      }
    }
  }
}
```

### Step 4: Fix generated source wiring in ae

For the 3 excluded projects (tempo, lcp-api-server-generated):
```groovy
// In appian-libraries/tempo/tempo.gradle
tasks.named('explodeCodeSourceMain') {
  dependsOn 'generateGrammarSource'
}

// In appian-libraries/lcp-api/lcp-api-server-generated-*/build.gradle
tasks.named('explodeCodeSourceMain') {
  dependsOn 'openApiGenerate'
}
```

---

## Remaining TODOs

1. [ ] Cherry-pick runtime detection from branch to main
2. [ ] Wire RuntimeUsageFilter into the pipeline on main
3. [ ] Run tests: `./gradlew test` on main after merge
4. [ ] Publish to mavenLocal and test against ae
5. [ ] Verify suppression count matches expectations
6. [ ] Configure exclusions in ae's build.gradle
7. [ ] Add dependsOn for generated sources in ae
8. [ ] Re-run buildHealth and compare FP counts
