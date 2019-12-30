package com.autonomousapps.fixtures

/**
 * A "multi-module" Java library project (has the `java-library` plugin applied). There is a root project with no
 * source, and one or more java-library subprojects.
 *
 * @param librarySpecs a list of library project names and types. Can be null. See [LibrarySpec] and
 * [LibraryType].
 */
class MultiModuleJavaLibraryProject(
    librarySpecs: List<LibrarySpec>? = null
) : ProjectDirProvider {

    private val rootProject = RootProject(librarySpecs)

    /**
     * Feed this to a [GradleRunner][org.gradle.testkit.runner.GradleRunner].
     */
    override val projectDir = rootProject.projectDir

    // A collection of library modules, keyed by their respective names.
    private val modules: Map<String, Module> = mapOf(
        *librarySpecs?.map { spec ->
            spec.name to libraryFactory(projectDir, spec)
        }?.toTypedArray() ?: emptyArray()
    )

    override fun project(moduleName: String) = modules[moduleName] ?: error("No '$moduleName' project found!")
}

private val DEFAULT_DEPENDENCIES_JVM = listOf(
    "implementation" to "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61"
)

val PARENT = LibrarySpec(
    name = "parent",
    type = LibraryType.KOTLIN_JVM,
    dependencies = DEFAULT_DEPENDENCIES_JVM + listOf("implementation" to "project(':child')"),
    sources = mapOf("Parent.kt" to """
            import com.autonomousapps.test.kotlin.inlineFunction
            
            class Parent {
                fun useInlineFunction() {
                    println(inlineFunction())
                }
            }
        """.trimIndent()
    )
)

val CHILD = LibrarySpec(
    name = "child",
    type = LibraryType.KOTLIN_JVM,
    dependencies = DEFAULT_DEPENDENCIES_JVM,
    sources = mapOf("Child.kt" to """
            inline fun inlineFunction(): Int = 42
        """.trimIndent()
    )
)
