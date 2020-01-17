package com.autonomousapps.fixtures

import java.io.File

/**
 * No Kotlin in this one.
 */
class JavaLibModule(rootProjectDir: File, librarySpec: LibrarySpec)
    : JavaGradleProject(rootProjectDir.resolve(librarySpec.name).also { it.mkdirs() }) {

    override val variant = "main"

    init {
        withBuildFile("""
            plugins {
                id('java-library')
            }
            dependencies {
                api 'org.apache.commons:commons-math3:3.6.1'
                implementation 'com.google.guava:guava:28.0-jre'
            }
        """
        )
        withSrcFile("$DEFAULT_PACKAGE_PATH/java/${librarySpec.name}/Library.java", """
            package $DEFAULT_PACKAGE_NAME.java.${librarySpec.name};
              
            class Library {
                public int magic() {
                    return 42;
                }
            }
        """
        )
    }
}

/**
 * No Android or Java, just Kotlin.
 */
class KotlinJvmLibModule(rootProjectDir: File, librarySpec: LibrarySpec)
    : KotlinGradleProject(rootProjectDir.resolve(librarySpec.name).also { it.mkdirs() }) {

    override val variant = "main"

    init {
        withBuildFile("""
            plugins {
                id('java-library')
                id('org.jetbrains.kotlin.jvm')
            }
            dependencies {
                ${librarySpec.formattedDependencies()}
            }
        """.trimIndent()
        )
        librarySpec.sources.forEach { (name, source) ->
            withSrcFile(
                relativePath = "$DEFAULT_PACKAGE_PATH/kotlin/$name",
                content = "package $DEFAULT_PACKAGE_NAME.kotlin\n\n$source"
            )
        }
    }
}