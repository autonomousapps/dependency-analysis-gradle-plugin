Gradle TestKit Support Changelog

# Unreleased
* [fix] `JarSubject.resource()` no longer fails with `NoSuchFileException` for entries in nested paths (e.g. `META-INF/services/...`).

# Version 1.7.0
* Compiled against Kotlin 2.2 and Gradle 9.4.1.

# Version 1.6.1
* [fix] Re-publish with non-broken metadata. 

# Version 1.6
* [new] New Subjects for accessing build artifacts.

# Version 1.5
* Improve truth tasks and failure output.

# Version 1.4
* Enhance APIs of `BuildTaskListSubject` and `BuildTaskSubject`.

# Version 1.3
* Changed artifact name to `com.autonomousapps:gradle-testkit-truth`
