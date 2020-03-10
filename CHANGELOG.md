Dependency Analysis Plugin Changelog

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
