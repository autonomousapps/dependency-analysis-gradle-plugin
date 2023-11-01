Gradle TestKit Support Changelog

# Version 0.7
* Version information for your plugin-under-test is now exposed via
* `AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION`.

# Version 0.6
* Changed artifact name to `com.autonomousapps:gradle-testkit-support`
* Added `GradleBuilder` as a helper around `GradleRunner`.

# Version 0.5
* `BuildScript.group` and `BuildScript.version` should be `var`.
* Simplify plugin names.

# Version 0.4
* New helper methods on `GradleProject` and `GradleProject.Builder` to simplify fixture construction.
* Simplified default JVM args set in `gradle.properties` of test fixtures.

# Version 0.3
* Enhance testkit BuildScript model.

# Version 0.2
* New `AbstractGradleProject` class as a foundation for project fixtures. 

# Version 0.1
Initial release.
