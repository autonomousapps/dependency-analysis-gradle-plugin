Graph-Support Changelog

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
