Release procedure for antlr

1. Update CHANGELOG
1. Update README if needed
1. `git commit -am "chore(antlr): prepare for antlr release x."`
1. Verify shadow jar. Run `./gradlew :antlr:shadowJar`. Manually inspect contents.
1. Publish: `./gradlew :antlr:publishToMavenCentral`
1. `git tag -a antlr-x -m "antlr version x."`
1. `git push && git push --tags`
