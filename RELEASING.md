Release procedure for dependency-analysis-android-gradle-plugin

1. Update the changelog
1. Bump version number to next stable version (use semantic versioning: x.y.z)
1. Update README if needed
1. git commit -am "Prepare for release x.y.z." 
1. Release on ...
1. git tag -a x.y.z -m "Version x.y.z"
1. Update build.kts to next snapshot version
1. git commit -am "Prepare next development version."
1. git push && git push --tags

