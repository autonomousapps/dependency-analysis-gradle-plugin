Gradle TestKit Support Changelog

# Version 0.18
* [New]: support passing custom environments to testkit. Add docs.

# Version 0.17
* [New]: allow adding custom content to android blocks.

# Version 0.16
* [New] Support imports in build scripts.
* [New] Expose capability overload on Dependency.
* [New] Update minSdk, targetSdk, compileSdk, and an equals after applicationId and namespace.
* [New] Add gradle property for disabling KGP default dep behavior.
* [New] Add support for isolated projects to GradleProperties.
* [New] Make it easier to pass in a custom environment to GradleBuilder.

# Version 0.15
* [New] New BuildArtifact API.
* [New] New Subjects for accessing build artifacts.
* [New] New `GradleProject` APIs for artifact access from test fixtures.
* [Fix] Remove Truth from compile and runtime classpaths.

# Version 0.14
* [New] Support `DslKind.KOTLIN`.
* [New] Allow overriding the `dslKind` when creating a project builder with `AbstractGradleProject`.

# Version 0.13
* [New] kit.Dependency supports platforms and enforcedPlatforms.
* [Fix] Escape maven url in kit.gradle.Repository.

# Version 0.12
* [Fix] Removed `plusAssign` operators from `GradleProperties` to resolve operator overload ambiguities when used with
  variables.

# Version 0.11
* [New] More things are mutable.
* [New] Simplified Android project defaults.
* [New] Support ergonomically writing version catalog files.

# Version 0.10
* Enhance `GradleProperties` and `AndroidBlock`.

# Version 0.9
* Enhance `Repositories` with `operator fun` overloads.
* Add `Repository.FUNC_TEST_INCLUDED_BUILDS` for access to list of included build repos.

# Version 0.8
* Filesystem repo is via `AbstractGradleProject.FUNC_TEST_REPO`.

# Version 0.7
* Version information for your plugin-under-test is now exposed via `AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION`.

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
