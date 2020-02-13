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
                ${librarySpec.formattedDependencies()}
            }
        """
        )
        librarySpec.sources.forEach { (name, source) ->
            withSrcFile(
                relativePath = "$DEFAULT_PACKAGE_PATH/java/$name",
                content = "package $DEFAULT_PACKAGE_NAME.java;\n\n$source"
            )
        }
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