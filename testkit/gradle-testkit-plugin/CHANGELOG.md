Gradle TestKit Plugin Changelog

# Version 0.14
* [Feat]: support functional tests of Gradle 'libraries' as well as plugins.

# Version 0.13
* [New] Update default support lib version to 0.18.

# Version 0.12
* [chore]: use non-deprecated method when available.

# Version 0.11
* [fix] Update default support lib version to 0.17.

# Version 0.10
* [fix] don't disallow changes on the `disablePublication()` property.
* [fix] Update default support and truth lib versions.

# Version 0.9
* [chore] Update default support and truth lib versions.

# Version 0.8
* [chore] Update default support and truth lib versions.

# Version 0.7
* [New] Add `disablePublication()` method to DSL.

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
