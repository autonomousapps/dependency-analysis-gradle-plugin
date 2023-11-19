Gradle TestKit Plugin Changelog

# Version 0.6
* Add a functionalTest publication. Not all projects have this set up.

# Version 0.4, 0.5
* Install `functionalTestRuntimeClasspath` if it exists.
* Support installing arbitrary classpaths.

# Version 0.3
* Support included builds with new extension `gradleTestKitSupport.includeProjects()`.
* Update extension to use gradle-testkit-support v0.9.

# Version 0.2
* New `gradleTestKitSupport` extension to simplify adding optional support libraries. Uses:
  * v0.8 of gradle-testkit-support, and
  * v1.3 of gradle-testkit-truth.

# Version 0.1
* First release.
