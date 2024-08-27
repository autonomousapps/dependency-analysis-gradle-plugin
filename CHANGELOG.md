Dependency Analysis Plugin Changelog

# Version 2.0.0
* [Feat]: add 'com.autonomousapps.build-health' settings plugin.
* [Feat]: remove support for auto-apply flag.
* [Feat]: remove restriction that DAGP must be applied to root project.
* [Feat]: helpful error message when KGP or AGP are missing from build classpath.
* [Feat]: check that buildHealth did something.
* [Feat]: remove deprecated ignoreKtx property.
* [Feat]: move PluginAdvice to the model package.
* [Feat]: rename FindInlineMembersTask to FindKotlinMagicTask.
* [Feat]: don't warn when AGP is using a newer patch version
* [Fix]: gracefully handle 'empty' Android res file.
* [Chore]: update to Gradle 8.9
* [Chore]: update to AGP 8.5.2

# Version 1.33.0
* [Feat] Bundle kotlin-test to avoid false-positives.
* [Fix] Merge dependency usages by identifier, not gav.
* [Fix] Publish graph-support v0.3, fixing broken metadata.
* [Fix] Improve performance of usesResByRes.
* [Fix] Sort output of findDeclarations.
* [Fix] Sort output of graphView tasks.
* [Fix] Handle constant pool tag 17 (CONSTANT_DYNAMIC).
* [Chore] Update latest stable AGP version to 8.5.1.

# Version 1.32.0
* [New]: Output dominator tree results in JSON format including size and total size of deps.
* [New]: Allow to force app behavior for pure Java projects.
* [New]: `generateProjectGraph` task.
* [New]: `reason` works for multi-capabilities.
* [New]: Print build file path in `projectHealth` console report.
* [Fix]: Enhance logging (more) when `ConstantPoolParser` throws exception.
* [Fix]: Do not dotty for path matching and remove prefix and suffix from binary class name.
* [Fix]: Fix Windows file separator incompatibility.
* [Fix]: Don't suggest adding `testImplementation` dependency on self.
* [Fix]: DAGP variants have a `Category` of 'dependency-analysis'.
* [Fix]: Sort an input map for better reproducibility.

# Version 1.31.0
* [Fix] support Isolated Projects.
* [Fix] only use new configurations factories from Gradle 8.5.
* [Fix] Do not suggest to move dependencies between feature variants
* [Fix] Reason explanation id ambiguity
* [Fix] Use a hash in file name for dependencies with capabilities
* [Fix] make file relativizing work properly on Windows
* [Fix] try/catch to workaround AGP issue.
* [Fix] enhance logging when ConstantPoolParser throws exception.
* [Chore] no group for 'internal' tasks.

# Version 1.30.0
* [Fix] Don't pass in android res (incl layouts) to XmlSourceExploderTask.
* [Fix] Use AGP-blessed API for getting compiled class files instead of bundleTask.
* [Fix] Use AGP's `variant.artifacts` instead of `tasks.named` for accessing class files.
* [Fix] Move Android project configuration outside of afterEvaluate.
* [Fix] Bump gradle-script-grammar to v0.3 (improving `fixDependencies` results).
* [Fix] Reason was failing to give correct results for project dependencies.
* [Fix] Handle disjoint classpaths. (main source and test source might have different versions on the same dependency.)
* [Chore] Use `java.util.Objects` instead of Guava for hashing.

# Version 1.29.0
* [New] Migrate to new, non-deprecated AGP APIs. Min AGP version now 8.0.
* [Fix] Don't suggest unnecessary dependency relating to Android res and new IDs.
* [Fix] Longstanding copy-paste bug in AndroidScore calculation.
* [Fix] Don't leak Kotlin stdlib from shaded dependencies.

# Version 1.28.0
* [New] Fully compatible with the configuration cache.
* [Fix] Detect `typealias` usage.
* [Chore] Fix deprecations relating to kotlinx-metadata 0.8.0.
* [Chore] Build with Gradle 8.5.

# Version 1.27.0
* [New] Users can exclude source sets from ABI analysis.
* [New] Dominator tree calculation for runtime classpath.
* [New] Move `ignoreKtx()` to DependenciesHandler (structure). Add deprecation.
* [Fixed] Filter `Provider`s from `ConfigurableFileCollection` dependencies.
* [Fixed] Catch exception when analyzing Kotlin files for inline members.
* [Fixed] ReasonTask should use actual ID, not first one it finds that matches `String#startsWith`.
* [Fixed] Make XML source `Comparable` and use sorted sets.
* [Fixed] Track usage of `testInstrumentationRunner` in Android projects.
* [Chore] Check for presence of unused flags and warn.
* [Chore] Don't sign publications when testing. Don't disable configuration cache.
* [Chore] Deal with many old TODOs.

# Version 1.26.0
* [New] Support KMP artifacts better ("-android" and "-jvm").
* [New] Support version catalog references in `BundleHandler` and `DependenciesHandler`.
* [Fixed] Look for InlineMembers in class files, not just jar files.
* [Chore] Update kotlinx-metadata-jvm to 0.7 from 0.6. Remove deprecated usages involved in Kotlin ABI analysis.
* [Chore] Rename all-declared-dependencies to exploded-jars.json.
* [Chore] Promote some tasks to the non-internal group.

# Version 1.25.0
* [New] Add exclude method for version catalog dependencies.
* [Fixed] Deprecate `dependencyAnalysis.dependencies` for structure.
* [Fixed] Filter out comments when scanning for annotation processors.
* [Fixed] Clarify annotation processor-related error messages.
* Use asm-relocated 9.6.0.0 to support Java 22.
* Revert "Support for correctly resolving KMP artifacts."
* Lower min supported version of Gradle to 7.4 from 7.5.

# Version 1.24.0
* [New] Support for correctly resolving KMP artifacts.
* [Fixed] Use full GAV when comparing module coordinates.
* Update 'known good AGP versions' logging.
* `resolveExternalDependencies` is configuration-cache compatible.
* Resolve Kotlin deprecations.

# Version 1.23.0, 1.23.1
* [New] DSL now permits custom behavior per sourceSet.
* [Fixed] Fix issues with sourceSet-based analysis. (Custom sourceSets, the test sourceSet.)
* [Fixed] There is now parity between Kotlin-JVM library analysis and java-library analysis.
* Use Shadow 8.1.1.

# Version 1.22.0
* [Fixed] Bundles account for IncludedBuildCoordinates, which are used more now.
* [Fixed] Workaround Gradle bug when analyzing `files(...)` dependencies in a Kotlin DSL build script.
* [Fixed] Workaround Gradle bug (?) relating to analyzing mixed source sets of Java and Kotlin.
* Use BuildIdentifier.getBuildPath() starting with Gradle 8.2.
* AGP 8.1.0 is the latest supported version.
* Kotlin 1.9.10.
* Fix issue in plugin build script for publishing to Gradle Plugin Portal.

Thanks [Jendrik Johannes](https://github.com/jjohannes))
Thanks [Josh Friend](https://github.com/joshfriend))

# Version 1.21.0
* [Fixed] Include Android res IDs in analysis.
* [Fixed] Handle ResolvedDependencyResult.resolvedVariant == null.
* Reduce memory usage by using a single InMemoryCache when possible.
* Remove pretty-printed files, reducing IO and disk usage.
* Consistently use IncludedBuildCoordinates to refer to other projects (#916).
* Change text to clarify missing entries (#910).
* Use new grammar for source analysis.
* Build and test against Gradle 8.2.1.
* Build and test against Kotlin 1.9.0.
* Test against latest AGP versions, and compile against AGP 7.4.2.

# Version 1.20.0
* [New] Support testFixtures in standard JVM projects.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [New] Can ignore analysis for specified source sets.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] Incorrect advice when using a dependency with a capability.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] False positive for testImplementation dependency with classifier.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* 

# Version 1.19.0
* [New] Analyze dependencies of all source sets in standard JVM projects.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] Dependencies that provide Android Activities are now considered to be providing runtime capabilities.
* [Fixed] Variant-specific declarations now more accurately map to variant-specific usages in the advice.
* [Fixed] Allow exclusions to be added more than once.
  (Thanks [Daan Schipper](https://github.com/daanschipper))
* Kotlinx Metadata 0.6.0.
  (Thanks [Gabriel Ittner](https://github.com/gabrielittner))

# Version 1.18.0
* [New] Add flag for ignoring android variants.
  (Thanks [Sergey Boishtyan](https://github.com/sboishtyan))
* [New] Use buffered reading when reading files in `SynthesizeDependenciesTask`.
  (Thanks [Daan Schipper](https://github.com/daanschipper))

# Version 1.17.0
* [Fixed] Fix NPE in `asmUtil.exportedPackages()`.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] `Coordinates.compareTo()` fix.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] Use buffered writer when writing large json objects to file.
  (Thanks [Daan Schipper](https://github.com/daanschipper))

# Version 1.16.0
* [New] Support ABI filtering on project level.
  (Thanks [Martijn Vegter](https://github.com/mvegter).)
* [New] You can now specify an allow-list (using regex) of which projects to enable analysis on. By default, the regex 
  is `".*"` (that is, all projects). An example of a regex that would exclude paths matching a prefix is 
  `"^((?!:prefix)).*$"`. Example usage:
  ```properties
  # gradle.properties
  # match all project paths except those with the prefix ':prefix' (using a negative lookahead)
  dependency.analysis.project.includes='^((?!:prefix)).*$'
  
  # only match project paths that have the prefix 'prefix'
  dependency.analysis.project.includes='^:prefix.*$'
  ```

# Version 1.15.0
* [Fixed] Duplicated declaration does not lead to wrong analysis result (#807)
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] Use information from 'module-info' to determine if a package is public.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] `project(...)` dependencies between subprojects of an included build.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] The requested version is irrelevant for included builds.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* Improve performance in SynthesizeDependenciesTask.
* Eliminate some redundant work in FindInlineMembersTask.
* Eliminate some redundant work relating to JarExploder.
* Use asm 9.4.

Thanks as well to [Adam Ahmed](https://github.com/oheyadam) for modernizing the GHA workflows!

# Version 1.14.0, 1.14.1
* [New] Support analysis of androidTest source sets.
* [Fixed] Improve sorting of duplicate versions.
* [Fixed] Only check AGP version on root project.
* Add debug logs for issue 780.
* Extracted new graph-support library.
* Bug fixes and performance improvements.

# Version 1.13.0, 1.13.1
* [New] `./gradlew printDuplicateDependencies` will generate a report and print to console all external dependencies in
  the build for which various subprojects resolve varying versions. This is an indication that your build might benefit
  from a platform.
* [New] `fixDependencies` task now supports the `--upgrade` flag, which triggers it to only "upgrade" dependencies (from
  `implementation` to `api`, and to add used transitives that aren't yet declared). Use it like:
  `./gradlew :module:fixDependencies --upgrade`.
* [Fixed] Consider ModuleAdvice to be empty if none of it is actionable.
* Various improvements to Groovy Gradle build script grammar.

# Version 1.12.0
* [Fixed] Analysis fails when an Android manifest doesn't have a package declaration.

# Version 1.11.0, 1.11.1, 1.11.2, 1.11.3
* [New] Very experimental `fixDependencies` task will automatically apply dependency advice to your build scripts,
  modifying those scripts in-place. Issue reports are welcome.
* [New] `reason` can be used to learn more about module-structure-related advice. For example
  `./gradlew <module>:reason --module android` will explain the rationale for advice to change (or not) a module from an
  Android project to a JVM project.
* [Fixed] Don't run `AndroidScoreTask` on Android apps.
* [Fixed] Capture even more Android res usages. Don't associate attribute names as map keys, as there are a lot of 
  duplicates.
* [Fixed] Detects `android:theme` usage in AndroidManifest.xml.
* Reverted use of KSP for JSON de/serialization with Moshi. Reflection is better.

# Version 1.10.0
* [New] `buildHealth` and `projectHealth` now include a new category of advice, "module structure advice." This  
  currently only includes advice on whether a project could be a JVM project instead of an Android project.

# Version 1.9.0
* [New] `reason` output contains much more information.
* [New] `reason` should know about the runtime classpath.
* [Fixed] Setting project-specific excludes will not by itself override the severity for issues of that type for that
  project. (Bug introduced in 1.7.0.)
* [Fixed] Fix cacheability issue in GraphViewTask, and therefore ComputeAdviceTask.

# Version 1.8.0
NOTE: This version _is_ available on the Gradle Plugin Portal.
* [Fixed] Consider 'invisible annotations' for purposes of ABI exclusions.
* [Fixed] Detect generic types from field signatures.
* [Fixed] Use KMP bundles even when neither member is declared.

# Version 1.7.0
NOTE: This version is not available on the Gradle Plugin Portal. It is only on Maven Central.
* [New] Severity set on a project takes precedence over global severity setting.
* [Fixed] `reason` "shortest path" did not show all the relevant classpaths.
* [Fixed] Removed undesirable dependencies from classpath.

# Version 1.6.0
* [New] Experimental support for printing the dominance tree of the compile classpath. Try it: 
  `./gradlew <module>:printDominanceTree`
* [Fixed] Support Components with multiple Jar files.
* [Fixed] Fails when AndroidManifest file does not contain package.
  ([#700](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/700))
* Publish shaded antlr and asm-relocated libs and consume those instead.

# Version 1.5.0
* [Fixed] Detect nested constant usage.
* [Fixed] Detect custom application defined in a different module.
* [Fixed] Detect more res-to-res usages.
* [Fixed] Use the same (unique) key when writing and reading to/from inMemoryCache.

# Version 1.4.0
* [New] Analysis now supports Groovy (experimental).
* [New] Analysis now supports Scala (experimental).

# Version 1.3.0
* [New] Treat KMP modules as implicit bundles.
* [Fixed] Plugin will not fail in the presence of `--configuration-cache`, but it still is not compatible.

# Version 1.2.1
* [New] Improve console output for root project.
* [New] Add a trailing space for path linkifying.
  (Thanks [Zac Sweers](https://twitter.com/ZacSweers).)
* [Fixed] Dependency bundles ignore change-advice to ensure consistent builds.
  ([#672](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/672))
* [Fixed] NoSuchMethodError: kotlinx.metadata.jvm.KotlinClassHeader.<init>.
  ([#673](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/673),
  thanks [Zac Sweers](https://twitter.com/ZacSweers).)

# Version 1.2.0
* [New] Support (optional) dependency version in the exclusion identifier.
  ([#640](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/640),
  thanks [Martijn Vegter](https://github.com/mvegter).)
* [New] Give advice to change to `runtimeOnly` where appropriate.
  ([#646](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/646),
  thanks [Jendrik Johannes](https://github.com/jjohannes).)
* [New] Ignore dependencies for and to unsupported sourceSets/variants.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes).)
* [New] Put 'compileOnlyApi' and 'providedCompile' into the COMPILE_ONLY bucket.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes).)
* [New] Support a primary entry point for bundles.
  ([PR #666](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/666))
* [Fixed] Filter out 'module-info.class' files in all places.
  (Thanks [Jendrik Johannes](https://github.com/jjohannes).) 
* [Fixed] Detect android assets.
  ([Issue 657](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657))
* [Fixed] Detect more Android styleable usages.
  ([Issue 664](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/664))

Special thanks to [Zac Sweers](https://twitter.com/ZacSweers) for consistently filing good issues.

# Version 1.1.0
* [New] `buildHealth` and `projectHealth` reports respect DSL language for easier copying and pasting.
* [Fixed] Capture generic types in class signature, for ABI-detection purposes.

# Version 1.0.0
* Promote to GA.
* Build with Gradle 7.4.2.
* Test against AGP 7.1.3 and 7.3.0-alpha08.

# Version 1.0.0-rc06
* [New] **Reason** now indicates if advice was filtered out due to a bundle or exclude rule.
* [Fixed] Replace `artifacts.clear()` with `isVisible=false`.
  ([#625](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/625))
  (Thanks [Jendrik Johannes](https://github.com/jjohannes))
* [Fixed] Filter out `androidTest`-related declarations more thoroughly.
* Don't register tasks if they can't be used.

# Version 1.0.0-rc05
* [New] onAny should also control onRedundantPlugins behavior.
* [New] Reason output colorized.
* [New] Reason output includes path to dependency.
* [Fixed] Filter out androidTest declarations.
* [Fixed] Improve Reason for `compileOnly` dependencies.
* [Fixed] Fix faulty set difference logic in redundant plugin excludes.

# Version 1.0.0-rc04
* [New] 'reason' task re-introduced. Usage example: `./gradlew lib:reason --id com.foo:bar:1.0`
* [New] Allow common configuration through build logic and override on project level.
  (Thanks [Martijn Vegter](https://github.com/mvegter))
* [Fixed] Plugins can now be excluded via `dependencyAnalysis.issues.<all|project(...)>.onRedundantPlugins.exclude`
* [Fixed] Fix `AbiHandler.excludeAnnotations()`.
* [Fixed] Fix build cache issue with GraphViewTask.
* `dependency.analysis.test.analysis` and `dependency.analysis.autoapply` can now be specified as Gradle properties.
* Test against AGP 7.1.2 and 7.2.0-beta04.

# Version 1.0.0-rc03
* [Fixed] Inaccurate analysis of test source sets in Android libraries.
* [Fixed] Explicit support for dependencies resolved from an included build.
* [Fixed] Improve support relating to annotation processors.
* [Fixed] Inline usages regression in 1.0.0-rcXX.
  (Thanks [Vladimir Mironov](https://github.com/nsk-mironov))
* [Fixed] Make FindAndroidLinters cacheable.
  (Thanks [Subhrajyoti Sen](https://github.com/SubhrajyotiSen))
* You can restore old behavior and print the buildHealth report with `-Pdependency.analysis.print.build.health=true`.

# Version 1.0.0-rc02
* Old model has been entirely removed. `-Ddependency.analysis.old.model=true` has no effect.
* [Fixed] Sort the output of `ProjectHealthConsoleReportBuilder` on the printed suggestion.
  ([#568](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/568))
  (Thanks [Martijn Vegter](https://github.com/mvegter))

# Version 1.0.0-rc01
* New model is now the default. To use the old model, specify `-Ddependency.analysis.old.model=true`.
* [Fixed] Don't report kotlin as unused when it is in fact used.

# Version 0.80.0
* [Fixed] Resolve LinkageError when more than one annotation processor references the same class.
  ([#556](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/556))
* [Fixed] Add databinding-compiler to the list of excluded databiding related dependencies.
  ([#543](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/543))
  Thanks to [Vladimir Mironov](https://github.com/nsk-mironov).
* [Fixed] Resolve Gradle 8 deprecation warning.
* [Fixed] Don't suggest removing test dependencies when test analysis is disabled.
* [New] Introduce UsagesHandler that allows to exclude particular classes from a list of used classes.
  ([#545](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/545))
  Thanks to [Vladimir Mironov](https://github.com/nsk-mironov).
* Build with Gradle 7.3.3.
* Test against AGP 7.1.0-beta05 and 7.2.0-alpha06.
* Improved DOT file formatting.
* Working on a new model. It can be used by passing the flag `-Dv=2`.

# Version 0.79.0
* [Fixed] Don't warn on duplicate dependency declaration.
  ([#507](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/507))
* [Fixed] Track interfaces when doing bytecode analysis.
  ([#500](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/500))
* [Fixed] Declared `compileOnly` file dependencies should not be marked as transitive
* [Fixed] Enum only JARs should not be reported as `compileOnly`.
* [Fixed] Filters out non-xml files from layouts collection.
  ([#490](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/490))
* Don't try to analyze a file that doesn't exist.
  ([#483](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/483))
* Analyze Dagger's annotation processor.
  ([#479](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/479))
* Building with Gradle 7 and Kotlin 1.5.21 now. Minimum supported version of AGP now 4.2.
* Bump to asm 9.2.
* Replace '- ' prefix with '  ' in advice output.
* Detect more res-by-res usages.
* Make artifact views lenient.
* `LocateDependenciesTask` can be up to date.
* Sort the output of `AdvicePrinter`
* The element type of `ConfigurableFileCollection` can be `GString`, use `toString` instead of casting.

Special thanks to [Martijn Vegter](https://github.com/mvegter) for several fixes in this release!

# Version 0.78.0
* Rename advice$variant task to generateAdvice$variant.
  ([#476](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/476))
* [Fixed] Detect Android res attr usage. Min version of AGP 4.2 required.
  ([#420](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/420))
* [Fixed] It's ok when a node isn't in the graph (sometimes).
  ([#463](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/463))
* [Fixed] Don't suggest declaring elements of the Gradle distribution.
  ([#464](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/464))

# Version 0.77.0
* [Fixed] Consider declared exceptions to be part of ABI, for dependency purposes.
  ([#395](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/395))
* [Fixed] Fix regression with constants detection from kt files (Kotlin 1.4 -> 1.5 breakage).
  ([#408](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/408))
* [Fixed] Don't crash when file is missing due to Android unit test being disabled for some variant.
  ([#452](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/452))
* [Fixed] Don't crash with `-Ddependency.analysis.autoapply=false` and `strictMode(false)`.
  ([#453](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/453))
* [Fixed] Handle complex Processor.init() functions when looking for annotation processor types.
  ([#454](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/454))
* Test against Gradle 7.2.
* Test against AGP 7.0.1 and 7.1.0-alpha10.

Thanks once again to [Gabriel Ittner](https://github.com/gabrielittner) for consistently filing issues with reproducers!

# Version 0.76.0
* [New] kotlin-kapt may now be excluded from the redundant-plugins check.
* [New] Auto-apply functionality has been removed from extension. To disable, you must now use a
  system property. Either `-Ddependency.analysis.autoapply=false` or add
  `systemProp.dependency.analysis.autoapply=false` to `gradle.properties`. Default remains true.
* [Improved] Dependency bundles now support multiple hops between the dependencies in the bundle.
  They also support bundles of test dependencies.
* Test against AGP 4.2.2, 7.0.0, and 7.1.0-alpha07.

# Version 0.75.0
* [New] Do not report java-library plugin as redundant, even in the absence of Java source.
* [Fixed] Fix Android manifest parsing in case of queries node.
  Thanks to [eugene-krivobokov](https://github.com/eugene-krivobokov) for this fix.
* [Fixed] Java bytecode analysis will filter to analyze only jar files.
  Thanks to [Astro03](https://github.com/Astro03) for this fix.
* [Fixed] Transitive test dependencies suggested to be put on wrong configuration.
  ([#424](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/424))
* [Fixed] Don't fail in configuration when an Android test variant is disabled.
  ([#423](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/423))
* [Fixed] Work around lint jar sometimes not existing in AGP 7.0.0.
* Test against Gradle 7.1.
* Test against AGP 4.2.1 and 7.0.0-beta04.

Additional thanks to [Gabriel Ittner](https://github.com/gabrielittner) for consistently filing good issues!

# Version 0.74.0
* [Fixed] Correctly associate compiled binaries with source files and the source sets they live in.
  ([#393](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/393))
* [Fixed] Builds with non-strict mode should not fail on strict-mode-only issues.
  ([#397](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/397))
* [Fixed] Tasks with `@Option` arguments should be annotated with `@Input`.
  Thanks to [eugene-krivobokov](https://github.com/eugene-krivobokov) for this fix.
  ([PR #413](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/413))
* [Fixed] Unused `testImplementation` dependencies should be detected.
  ([#366](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/366))
* Use the BIN distribution of Gradle 6.9.
  Thanks to [Vladimir Sitnikov](https://github.com/vlsi) for this enhancement.

# Version 0.73.0
* [Fixed] Detect usage of annotations on type parameters
  ([#372](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/372))
* Remove deprecated functions.

# Version 0.72.0
* [New] Non-strict mode which helps to keep compile graphs smaller than with the default strict
  mode.
  ```
  // root build.gradle[.kts]
  dependencyAnalysis {
    strictMode(false) // default is true
  }
  ```
  
* [New] Compute some basic build health metrics, based on size and shape of the compile-classpath graphs.
* Remove all the JCenter references missed the first time around.

# Version 0.71.0
* [New] Make it easier to make changes incrementally by introducing the concept of "ripples." A
  ripple can occur if you downgrade a dependency (remove it or update it from `api` to
  `implementation`), and this removes that dependency from the compilation classpath of a dependent
  project that was using that dependency without having declared it. Use like:

```
./gradlew ripples --id :some-project
```
* Bumped Kotlin to 1.4.21.
* Now building with and testing against Gradle 6.8.2.
* Updated to latest AGPs at time of writing (4.1.2, 4.2.0-beta04, 7.0.0-alpha05).
* Removed all references to jcenter. 

# Version 0.70.0
* [Fixed] Assume dependencies on some variant (e.g. `debugImplementation` vs `implementation`) are 
  correctly on that variant.
  ([#340](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/340))
* [Fixed] Don't duplicate section headers in `advice-console.txt`.
  ([#318](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/318))
* [Fixed] Use absolute-path sensitivity for artifact file paths.
  ([#350](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/350))
* [Fixed] File dependencies now trim the prefix path in order to provide a unique identification.
* [Fixed] Exclusively use the compile classpath for resolving dependencies, filtering out 
  constraints.

# Version 0.69.0
* [Fixed] Don't use an enforced platform for the Kotlin BOM. This will permit use of the Kotlin
compiler daemon for projects using Kotlin 1.4+.
 ([#333](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/333))
* Now building with and testing against Gradle 6.7.1.
* Update supported version of AGP 4.2.0 to beta01.

Thanks to Stephane Nicolas @stephanenicolas for helping keep AGP up to date, and for the many 
discussions.

# Version 0.68.0
* [Fixed] Improved analysis of annotations for ABI reporting.
* [Fixed] Be null-safe.

# Version 0.67.0
* [Fixed] Annotations on public classes, public methods, and parameters of public methods are part 
of the ABI.
 ([#296](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/296))

# Version 0.66.0
* [New] Visualize project dependencies and reverse-dependencies (dependents) with 
```
./gradlew :projectGraphReport
```
or see the dependents of a specific project with
```
./gradlew :projectGraphReport --id :my-project
```
The resultant dot files can be converted to svg by installing graphviz and executing
```
dot -Tsvg path/to/file.gv -o output.svg
```
* [New] Configure artifacts-clearing behavior with a new system property, 
`dependency.analysis.clear.artifacts=<true|false>`. This can be added to the command line invocation
 or added to `gradle.properties`. Default is `true`.
 ([#321](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/321))


# Version 0.65.0
* [Fixed] Remove task execution listener. It wasn't doing anything useful and was incompatible with 
the configuration cache.
* [Fixed] Optimize GraphBuilder algorithm to dramatically improve performance.

# Version 0.64.0
* [New] Run `./gradlew :ripples` to see expected downstream impacts from upstream changes.
* [New] Colorize console output (only used experimentally in `ripples` right now).
* [Fixed] Detect imports in Kotlin source even when @file:<Anything> is used.
* [Fixed] Filter out Java Platform modules when creating transitive-use relations.
* [Fixed] LocateDependenciesTask should not resolve dependencies.

# Version 0.63.0
* [New] Detect Android lint-only dependencies and do not suggest removing them.
([#303](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/303))

# Version 0.62.0
* [New] Will now ignore not-well-known configurations.
([#300](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/300))
* [New] Firebase is now part of a dependency bundle by default.
([#305](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/305))
* [New] ABI dump output is now more accessible to end-users.
* [New] `Reason` now includes more information about dependencies, specifically Android manifest
  components and security providers.
* [Fixed] Safe-cast to `ProjectDependency` to prevent mysterious NPE.
([#295](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295))
* [Fixed] Dependency graph doesn't show flat jars.
* Improved console output. 
* Improved exception message when build is configured to fail.
* Now building with Gradle 6.7.

Thanks to Vladimir Sitnikov [@vlsi](https://github.com/vlsi) for discussing several issues and
reviewing PRs for this release.

# Version 0.61.0
* [New] New `:proj:projectHealth` task added to each subproject/module in a build. This is a new 
primary end-user target and will respect the user-defined `severity` behavior, similar to 
`:buildHealth`. Furthermore, the `:failOrWarn` task has been removed, and now `:buildHealth` will
simply fail itself. 
([Issue 273](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/273))
* [Fixed] Flat jar file dependencies mis-reported in several interesting ways.
* Updated to latest AGPs at time of writing (4.1.0-rc03 and 4.2.0-alpha12) 

# Version 0.60.0
* [Fixed] Does not union global and local rules as expected.
([Issue 273](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/273))
* [Fixed] In-memory cache now has a configurable eviction policy.
([Issue 274](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/274))
* [Fixed] Sometimes a dependency can appear multiple times in the advice.

# Version 0.59.0
* [New] Kotlin stdlib now a dependency bundle by default, improving ergonomics for Kotlin users.
* [New] Improved on _reason_ experimental feature. Try it out:
```
./gradlew proj:reason --id com.squareup.okio:okio
```
* Do less work during configuration.

# Version 0.58.0
* [New] New experimental feature to provide insight into the reason for the plugin's advice. For 
example:
```
./gradlew proj:reasonDebug --id com.squareup.okio:okio
``` 
* [Fixed] Fails to recognize when dependencies have been declared on multiple configurations.
([Issue 257](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/257))
* [Fixed] Eagerly realizing tasks.
* Updated to latest AGPs at time of writing (4.1.0-rc02 and 4.2.0-alpha09) 
* Updated test matrix to run against Gradle 6.6.1.

# Version 0.57.0
* [Fixed] LocateDependenciesTask remains up to date even after changing dependency configurations
([Issue 255](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/255)).
* [Fixed] Works better with Kotlin Multiplatform Projects 
([Issue 228](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/228)).
(Thank you to Martin Bonnin @martinbonnin for contributing this!)
* [Fixed] AARs with only native libs wrongly declared unused
([Issue 252](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/252)).
* [Fixed] Don't try to analyze `module-info.class` files.
([Issue 239](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/239))

# Version 0.56.0
* [Fixed] Unhook plugin from `assemble` task, to which it was yoked against its will. The upshot is
that running `assemble` will no longer trigger the plugin's tasks to run. Using the 
**Build Project** button in Android Studio will also no longer trigger the plugin's tasks.
* [Fixed] Don't do too much work during configuration for the `LocateDependenciesTask`.
* No longer disable plugin configuration when in the context of Android Studio. This was causing
more problems than it solved (and may not even have solved anything). 
* Improved warning message about using a version of AGP outside of the known-good range.

# Version 0.55.0
* Don't call `await()` on `WorkerExecutor`s, which should improve runtime performance, and was
unnecessary anyway. 

# Version 0.54.0
* [Fixed] Crashes in the presence of file dependencies (flat jars).
* Improved efficiency in bytecode analysis, which should reduce memory pressure and CPU usage.

# Version 0.53.1
* [Fixed] Inadvertently built 0.53.0 with Java 9, which leads to compatibility issues with projects
that use Java 8.

# Version 0.53.0
* [New] Improved support for Spring Boot and Kotlin Multiplatform Projects. This plugin no longer
relies on the `jar` task, which is disabled by these plugins. Instead, it uses compiled class files
as its input.
* [New] The plugin will not suggest `api` dependencies for Spring Boot projects.
* Now built with Gradle 6.5.1.
* Now compiling against AGP 4.0.1 and testing against AGPs 3.5.4, 3.6.4, and 4.0.1.

# Version 0.52.0
* [New] Provide suggested fix on Android unit test compilation failure. Partially resolves 
[Issue 205](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/205).
* [Fixed] Android Studio reports configuration failure due to changes in 0.50.0.
* [Fixed] Don't suggest changing an implementation dependency to testImplementation when it is 
actually used in the main source.
* Now compiling against AGP 4.0.0.

# Version 0.51.0
* [Fixed] Don't crash when unit test variants have been disabled.
(Thank you to Stephane Nicolas @stephanenicolas for contributing this!)

# Version 0.50.0
* [New] Plugin disables itself if invoked from the IDE, to workaround an issue with Android Studio's
`Make project`.
* [New] You can disable most console output with a new system property, 
`dependency.analysis.silent=true`. This can be added to the command line invocation or added to 
`gradle.properties`.

# Version 0.49.0
* [Fixed] Detect star imports as supported annotation types.
* [Fixed] Detect if jar task has been disabled and provide more useful error message if so. 

# Version 0.48.0
* [New] Analyze test source in order to provide advice relating to test dependencies.
* Updated to latest AGPs at time of writing (4.1.0-beta01 and 4.2.0-alpha01) 
* Deprecated 'master' branch in favor of new 'main' branch.

# Version 0.47.0
* [New] The concept of dependency "facades" has been formalized as "logical dependencies", aka
"bundles". `dependencyAnalysis.setFacadeGroups()` is now deprecated and is a no-op. Users should 
instead use `dependencyAnalysis.dependencies {}` to specify their bundles. Finally, with this 
change, the Kotlin stdlib family is no longer considered a bundle by default.
(Thanks to Zac Sweers @ZacSweers for talking with me about how to name this new feature)
* Simplified and reduced the amount of logging.
* Building with Gradle 6.5. Added Gradle 6.5 to the test matrix.

# Version 0.46.0
* [New] New `registerPostProcessingTask()` method on the extension to register a custom task to 
execute with the `ComprehensiveAdvice` produced by the project as an input.
* [New] Can now filter/configure compileOnly advice, analogous to other kinds of advice.
* [New] Improved DSL for configuring plugin behavior. See the
[wiki](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/wiki/Customizing-plugin-behavior)
for more information.
(Thank you to Stephane Nicolas @stephanenicolas for contributing this!)
* Updated to latest AGPs at time of writing (4.0.0!).

# Version 0.45.0
* [New] All advice, both dependency- and plugin-related, is now aggregated at the project level for
easier consumption. The extension methods `adviceOutputFor(variant: String)` has been changed to
`adviceOutput()`. It no longer returns a nullable `RegularFileProperty`. This output is now always
non-null, but may nevertheless _contain_ a null value, so you should use `getOrElse()`.
Finally, the type of the output has changed from `Set<Advice>` to `Set<ComprehensiveAdvice>`.
* [Fixed] `StringIndexOutOfBoundsException` when parsing empty source directory.
* Several root-level aggregate tasks were removed as no longer useful.

# Version 0.44.0
* [New] Improved support for Android variants in dependency analysis. Plugin now analyzes all 
source sets / variants for each execution of `buildHealth`.
* [Fixed] Build cache issue with `ArtifactsAnalysisTask`. (Thanks to @Paladinium for filing the 
issue and providing a reproducer!)
* [Deprecated] The `setVariants()` method is now deprecated; it no longer does anything, and will be
removed in v1.0.0.
* No longer attempts to parse Java stubs generated by kapt. No tests break, so ¯\\_(ツ)\_/¯
* Updated to latest AGPs at time of writing (4.0.0-rc01).
* Staging repo closing and release process is now semi-automated (with thanks to 
Niklas Baudy @vanniktech, https://github.com/vanniktech/gradle-maven-publish-plugin)

# Version 0.43.0
* [New] New `setDependencyRenamingMap()` method on the DSL allows customizing some output with 
semantic dependency names. See the javadoc for more information. More output will be mapped in a
future release. (Thank you to Stephane Nicolas @stephanenicolas for contributing this!)
* Building plugin with Gradle 6.4.1, and testing against same.
* Comment on PRs with build scan URL when there is a failure.

# Version 0.42.0
* [New] Support the `application` plugin.
* [New] New `abi` exclusions DSL. See the wiki for details. (Thanks to Zac Sweers @ZacSweers for the
contribution!)
* [New] Detects usages that only appear in a generic context, and javadoc too (as a side effect).
* [New] Does not suggest removing dependencies that supply security providers (such as Conscrypt).
* Now publishing snapshots on every push to master.
* Building plugin with Gradle 6.4, and testing against same.
* Updated to latest AGP, 4.1.0-alpha09.

# Version 0.41.0
* [New] Supports the concept of "facade" dependencies. See the new wiki for details.
* Post-processing support has been improved. See the new wiki for details.
* Most of what was in the over-long readme has been moved to the wiki.
* `OutputPaths` is back to internal. We have a new way to make outputs available.
* Updated to latest AGPs at time of writing (4.0.0-beta05, 4.1.0-alpha08).

# Version 0.40.0
* [New] Console report now stored on disk for easier human consumption.
(Thank you to Stephane Nicolas @stephanenicolas for contributing this!)
* [New] `OutputPaths` is now non-internal at `com.autonomousapps.OutputPaths`. This makes it 
possible to consume plugin outputs without having to reference its tasks.
* [New] `Advice` json for "remove" and "add" advice now indicates used-transitive dependencies (for
the former) and parents or upstream dependencies (for the latter).
* [Fixed] Reports Java service loaders as unused. Due to their runtime nature, they will now be 
filtered from the unused dependencies advice.
* Updated to latest AGPs at time of writing (3.6.3, 4.0.0-beta04, 4.1.0-alpha06).
* Added a CONTRIBUTING guide (Thank you to Stephane Nicolas @stephanenicolas for assisting with 
this)

# Version 0.39.0
* [New] Reports when kapt has been applied but there are no annotation processors present.
* [New] Reports when either the `java-library` or `org.jetbrains.kotlin.jvm` have been redundantly
applied. Laid the groundwork for reporting other plugin issues.
* [Fixed] Reports unused dependencies that are on a flavor-based configuration as being on a "null"
configuration.
* Re-organized the model code. In particular, moved `Advice` and `Dependency` to a non-internal
package. These should now be considered part of the plugin's public API.
* Added an "intermediates" directory to the output, so it's more clear to end-users which files
they should care about.

# Version 0.38.0
* [Fixed] Plugin now correctly detects usage of annotation processors that support annotations with
`SOURCE` retention policies.
* Updated asm dependency to 8.0.1 (shaded).

# Version 0.37.0
Please note that this version is only available via Maven Central, as there were issues publishing 
to the Gradle Plugin Portal.
* [New] Support kotlin-jvm projects.
* [Fixed] False positive on unused annotation processors.
* [Fixed] Poor performance in `MisusedDependencyDetector`. (special thanks to Stephane Nicolas 
@stephanenicolas for discussing this issue with me)

# Version 0.36.0
* [New] Reports unused annotation processors.
* [New] Tries to be more memory-efficient by interning strings.
* [New] Tries to be more memory-efficient by using in-memory cache to avoid duplicate (simultaneous) work.
* [New] No longer fails when used outside of known-good AGP range. Instead emits warning.
* [New] New extension method `chatty()` lets users set console logging preference.
* [Fixed] Don't use default Locale when capitalizing strings. 

# Version 0.35.0
* [Fixed] Plugin gives redundant advice (suggests moving compileOnly dep to compileOnly). This impacted both
console output and advice.json.
* Plugin no longer eagerly fails if a version of AGP is used outside of the known-good range.
Instead it emits a warning.

# Version 0.34.0
* [Fixed] Check for presence of ContentProvider or Service in contributed Android manifests.
Such Android libraries are now considered used, even if they don't appear in the compiled classes of a project.
(nb: this ensures LeakCanary is not considered unused.)
* [Fixed] Reports appcompat as unused when its resources are (but not by Java/Kotlin source).
* [Fixed] Deprecation warning with AGP 4+ for `android.viewBinding` and `android.dataBinding`.
Fixed issue with `AgpVersion` comparisons.

# Version 0.33.0
* Added extension option for ignoring "-ktx" dependencies. See README for more information.
* [Fixed] Version badge showed a very old version. (Thanks Pavlos-Petros Tournaris!) 
* [Fixed] Emit helpful error message if plugin has not been applied to root project.
* [Fixed] Provide advice for source-containing root projects.
* [Fixed] Subtle bug that led to unused dependencies showing up in change-advice.

# Version 0.32.0
* Plugin now available on Maven Central in addition to Gradle Plugin Portal.
* Plugin supports a new configuration option to not auto-apply to all subprojects.

# Version 0.31.0
* [Fixed] Now supports AGP 4.1.0-alpha04.

# Version 0.30.0
* [Fixed] [Issue 82](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/82). Build fails with StringIndexOutOfBoundsException.

# Version 0.28.0
* [Feature] The plugin will warn the user and throw an explicit error if it is applied to a project outisde the range of accepted AGP versions (3.5.3-4.1.0-alpha08 at time of writing).
* [Fixed] Deprecation warnings on projects built with AGP 4+, due to accessing 3.x-era `android.dataBinding` and `android.viewBinding`.

# Version 0.27.0
* There is no version 0.27.0, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.26.1
* [Feature] The plugin now detects if databinding or viewbinding have been enabled and, of so, does not suggest changing dependencies automatically added by AGP.

# Version 0.26.0
* There is no version 0.26.0, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.25.0
* [Fixed] Support for AGP 4.1.0-alpha02.

# Version 0.24.1
* [Fixed] Should not assume every class has a superclass.

# Version 0.24.0
* [Fixed] Crashes when configuring project with Java-only Android application module.

# Version 0.23.0
* [Feature] Added support for recognizing potential `compileOnly` dependencies, such as source/class annotation libraries.
* Simplified source code parsing so it's only done once.
* [Fixed] Forgot to close an input stream on a collection of files.

# Version 0.22.0
* [Fixed] Runtime issue during configuration for some projects.
* Now testing against Gradle 6.2.2.
* Bumped Kotlin to 1.3.70.

# Version 0.21.1
* [Fixed] Runtime issue with custom variant and java-library subproject.

# Version 0.21.0
* [Fixed] ConstantDetector task fails at runtime.

# Version 0.20.3
* [Fixed] Plugin crashes with AGP 4.0.0-beta01.
* Functional tests updated to run against latest AGP 4.0.0 beta and Gradle 6.2.1.

# Version 0.20.2
* [Feature] Lifts limitation on detecting constant usage.

# Version 0.20.1
* There is no version 0.20.1, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.20.0
* There is no version 0.20.0, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.19.2
* [Feature] Extension offers more configuration options for detected issues.

# Version 0.19.1
* There is no version 0.19.1, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.19.0
* There is no version 0.19.0, as there were issues with publishing to the Gradle Plugin Portal.

# Version 0.18.0
* [Feature] Extension now offers basic configurable failure options.
* Bumped to Gradle 6.1.1, including functional tests matrix.

# Version 0.17.1
* Simplified console output.

# Version 0.17.0
* [Feature] Thorough advice now provided for how to update build scripts based on plugin findings.
This is printed in narrative form as well as written to disk in a machine-readable format.
* Trimmed down README with a new focus on the advice-related tasks.

# Version 0.16.0 
* [Fixed] Plugin doesn't detect namespaced Android resource usage.
(nb: still does not detect non-namespaced res usage.)
* [Fixed] Plugin now works with AGP 4.0.0-alpha09.

# Version 0.15.0
* [Fixed] Plugin now works with AGP 4.0.0-alpha08.

# Version 0.14.0
* [Feature] Reports now indicate on which configuration a dependency was declared, if it were.
* [Fixed] Kotlin stdlib is never a candidate for unused direct dependency.
* Significantly more tests, alongside some refactoring.

# Version 0.13.1
* [Fixed] Kotlin stdlib never appears in list of used transitive dependencies.

# Version 0.13.0
* Added support for detecting use of Kotlin inline members.

# Version 0.12.1 
* Reverted modularization of models & utils.

# Version 0.12.0
* ([12](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/12)) Generate outputs into buildDir/reports directory
* Normalize annotation of input AbiAnalysisTask
* ([11](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/11)) add version in transitive dependencies
* More robustness in Functional tests
* Extract models and utils to separate Gradle project ("module")

# Version 0.11.1
* Add resolved versions to all reports
* Clean up of Runner class
* Matrix of Functional tests for different AGP and Gradle versions
* Target Kotlin at Java 8
* Add test for ABI dependency analysis
* Use Java 8
* Add unit tests to some components
* Improved logging
* Refactor of jar/ClassList analysis tasks
* Refactor Gradle Runner

# Version 0.11.0

# Version 0.10.0
* Plugin should now be applied only to root project.

# Version 0.9.0
* Add extension for Aggregate BuildHealth Task
* Improved bytecode analysis
* Switched from aggregating tasks to aggregating configurations.
* ([6](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/6)) Changed file extension on some outputs to .json.
* ([7](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/7)) Use reified types
* Added abstract base class for android-project analysis
* Introduction of perpetual semantic versioning.

# Version 0.8
* Improve analysis of pure Java projects
* Apache 2.0 license
* Improved misused-dependencies report.
* Fixed an issue that impacted IDE sync.
* ABI analysis merged
* AGP 3.5.3

# Version 0.7.1

# Version 0.7

# Version 0.6.1

# Version 0.6

# Version 0.5
* Reports in HTML
* Improved cacheability of tasks
* AGP & KGP versions bump 3.5.2 & 1.3.61

# Version 0.4
* Gradle 5 and shaded ASM dependency
* Change handling of kotlin-stdlib artifacts
* ASM 7
* kapt support
* [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) projects support
* Better byte code analysis

# Version 0.3

# Version 0.2
* Analysis of XML layouts
* Gradle 6
* Function testing introduced (but doesn't work yet)
* Simplified ClassListAnalysisTask configuration.

# Version 0.1
October 23rd, 2019. Project starts.
