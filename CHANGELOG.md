Dependency Analysis Plugin Changelog

# Version 0.42.0 (unreleased)
* [Fixed] Detects usages that only appear in a generic context.

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
* Blacklist kotlin-stdlib artifacts
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
