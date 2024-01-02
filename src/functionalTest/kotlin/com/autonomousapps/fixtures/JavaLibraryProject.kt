// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("JvmFixtures")

package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.dependencies.Plugins

/**
 * A "multi-module" Java library project (has the `java-library` plugin applied). There is a root project with no
 * source, and one or more java-library subprojects.
 *
 * @param librarySpecs a list of library project names and types. Can be null. See [LibrarySpec] and
 * [LibraryType].
 */
class MultiModuleJavaLibraryProject(
  rootSpec: RootSpec = RootSpec(),
  librarySpecs: List<LibrarySpec>? = null,
) : ProjectDirProvider {

  private val rootProject = RootProject(rootSpec)

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
  "implementation" to "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Plugins.KOTLIN_VERSION}"
)

//region constant tests
val CONSUMER_CONSTANT_JAVA = LibrarySpec(
  name = "consumer",
  type = LibraryType.JAVA_JVM_LIB,
  dependencies = listOf("implementation" to "project(':producer')"),
  sources = mapOf(
    "Consumer.java" to """ 
    import $DEFAULT_PACKAGE_NAME.java.Producer;
    
    public class Consumer {
      public void magic() {
        System.out.println("Magic = " + Producer.MAGIC);
      }
    }""".trimIndent()
  )
)

val PRODUCER_CONSTANT_JAVA = LibrarySpec(
  name = "producer",
  type = LibraryType.JAVA_JVM_LIB,
  dependencies = emptyList(),
  sources = mapOf(
    "Producer.java" to """
    public class Producer {
      public static final int MAGIC = 42;
    }""".trimIndent()
  )
)

val CONSUMER_CONSTANT_KOTLIN = LibrarySpec(
  name = "consumer",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = listOf(
    "implementation" to "project(':producer')",
    "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}"
  ),
  sources = mapOf(
    "Consumer.kt" to """ 
    import $DEFAULT_PACKAGE_NAME.kotlin.Producer
    
    class Consumer {
      fun magic() {
        println("Magic = " + Producer.MAGIC);
      }
    }""".trimIndent()
  )
)

val PRODUCER_CONSTANT_KOTLIN = LibrarySpec(
  name = "producer",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = listOf("implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}"),
  sources = mapOf(
    "Producer.kt" to """
    object Producer {
      const val MAGIC = 42;
    }""".trimIndent()
  )
)
//endregion constant tests

//region inline test
val INLINE_PARENT = LibrarySpec(
  name = "parent",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = DEFAULT_DEPENDENCIES_JVM + listOf("implementation" to "project(':child')"),
  sources = mapOf(
    "Parent.kt" to """
    import $DEFAULT_PACKAGE_NAME.kotlin.inlineFunction
    
    class Parent {
      fun useInlineFunction() {
        println(inlineFunction())
      }
    }""".trimIndent()
  )
)

val INLINE_CHILD = LibrarySpec(
  name = "child",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = DEFAULT_DEPENDENCIES_JVM,
  sources = mapOf("Child.kt" to "inline fun inlineFunction(): Int = 42")
)
//endregion inline test

//region abi test
val ABI_SUPER_LIB = LibrarySpec(
  name = "super-lib",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = DEFAULT_DEPENDENCIES_JVM,
  sources = mapOf("SuperClass.kt" to "open class SuperClass")
)

val ABI_CHILD_LIB = LibrarySpec(
  name = "child-lib",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = DEFAULT_DEPENDENCIES_JVM + listOf("api" to "project(':super-lib')"),
  sources = mapOf(
    "ChildClass.kt" to """
    import $DEFAULT_PACKAGE_NAME.kotlin.SuperClass
    
    class ChildClass : SuperClass()""".trimIndent()
  )
)

val ABI_CONSUMER_LIB = LibrarySpec(
  name = "consumer-lib",
  type = LibraryType.KOTLIN_JVM_LIB,
  dependencies = DEFAULT_DEPENDENCIES_JVM + listOf("implementation" to "project(':child-lib')"),
  sources = mapOf(
    "ConsumerClass.kt" to """
    import $DEFAULT_PACKAGE_NAME.kotlin.ChildClass
    
    class ConsumerClass {
      init {
        val child = ChildClass()
      }
    }""".trimIndent()
  )
)
//endregion abi test

//region error test
val JAVA_ERROR = LibrarySpec(
  name = "error",
  type = LibraryType.JAVA_JVM_LIB,
  applyPlugin = true,
  dependencies = emptyList(),
  sources = mapOf(
    "Error.java" to """ 
    import $DEFAULT_PACKAGE_NAME.java.Error;
    
    public class Error {
      public void magic() {
        System.out.println("Magic = " + Producer.MAGIC);
      }
    }""".trimIndent()
  )
)
//endregion error test
