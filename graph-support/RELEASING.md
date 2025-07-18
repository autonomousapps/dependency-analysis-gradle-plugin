Release procedure for graph-support

1. Update CHANGELOG
1. Update README if needed
1. Update the version in `build.gradle.kts`.
1. `git commit -am "chore(graph-support): prepare for graph-support release x.y."`
1. Publish: `./gradlew :graph-support:publishToMavenCentral`
   (this will automatically run the tests, and won't publish if any fail)
1. `git tag -a graph-support-x.y -m "Graph Support version x.y."`
1. `git push && git push --tags`
