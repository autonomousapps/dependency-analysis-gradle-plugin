# kotlin-editor-relocated

This shadow module exists only to shade the [KotlinEditor](https://github.com/cashapp/kotlin-editor) project, and only
because we don't want to permit a non-shaded version of `antlr` to end up in the DAGP runtime. That is a heavily-used 
tool, and classpath conflicts are common and annoying. This is a heavy workaround, but it does work. We do it as a 
separate project because it is likely to be a stable artifact, and that will keep the final size of the DAGP jar small,
which is important in a world of ephemeral CI constantly re-downloading the internet.
