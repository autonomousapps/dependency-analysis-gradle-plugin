Release procedure for asm-relocated

1. Update CHANGELOG
1. Update README if needed
1. Verify shadow jar. Run `./gradlew :asm-relocated:shadowJar`. Manually inspect contents.
1. `git commit -am "chore(asm-relocated): prepare for asm-relocated release x."`
1. Publish: `./gradlew :asm-relocated:publishToMavenCentral`
1. `git tag -a asm-relocated-x -m "asm-relocated version x."`
1. `git push && git push --tags`
