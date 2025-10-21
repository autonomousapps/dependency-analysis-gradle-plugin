Graph-Support Changelog

# Version 0.9.0
* [Feat]: add `Graphs.reversed()`.

# Version 0.8.3
* [Fix] Remove auto-applied kotlin-stdlib for Gradle 8.11 support.

# Version 0.8.2
* [Fix] Forcibly downgrade kotlin-stdlib to 2.0.21 for Gradle 8.11 support.

# Version 0.8.1
* [Fix] Downgrade Kotlin to 2.0.21 for Gradle 8.11 support.

# Version 0.8
* [Fix] set apiVersion and languageVersion to 2.0 for Gradle 8.11 support.

# Version 0.7
* [Feat]: Build with Gradle 9.0.0, change API to non-nullable.

# Version 0.6
* [Fix]: `DominanceTree` doesn't loop forever.

# Version 0.5
* [New] `Graphs` has new overloads of `reachableNodes()` that accepts a predicate for node-matching.

# Version 0.4
* [New] `Graphs` now has `Graph<N>.roots()` and `Graph<N>.shortestPaths(source)` methods.
* [New] `ShortestPaths` now has a `distanceTo(other)` method.
* [Chore] Bump Guava to 33.3.1-jre.

# Version 0.3
* [New] Output dominator tree results in JSON format including size and total size of deps. 

# Version 0.2
Second release.

# Version 0.1
Initial release.
