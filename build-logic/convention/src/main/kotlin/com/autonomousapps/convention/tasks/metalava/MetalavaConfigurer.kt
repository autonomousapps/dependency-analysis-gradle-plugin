package com.autonomousapps.convention.tasks.metalava

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.jvm.Jvm

/**
 * Configure Metalava for API change tracking.
 *
 * Source:
 * https://cs.android.com/android/platform/superproject/main/+/main:tools/metalava/metalava/
 *
 * Exemplars:
 * 1. https://github.com/firebase/firebase-android-sdk/blob/2bfc0a5de4c3d384238b25f9b71ef36104a72fa0/plugins/src/main/java/com/google/firebase/gradle/plugins/Metalava.kt#L4
 * 2. https://github.com/google/ksp/blob/main/buildSrc/src/main/kotlin/com/google/devtools/ksp/ApiCheck.kt
 * 3. https://github.com/liutikas/apis-how-hard-can-it-be/blob/main/build-logic/src/main/kotlin/com/example/logic/Metalava.kt
 */
internal class MetalavaConfigurer(
  private val project: Project,
  private val versionCatalog: VersionCatalog,
) {

  companion object {
    const val TASK_GROUP = "api"
  }

  private val metalava by lazy(LazyThreadSafetyMode.NONE) {
    val metalavaDependency = versionCatalog.findLibrary("metalava").get().get()
    project.configurations.detachedConfiguration(project.dependencies.create(metalavaDependency))
  }

  fun configure(): Unit = project.run {
    tasks.register("metalavaHelp", MetalavaHelpTask::class.java) { t ->
      t.metalava.setFrom(metalava)
    }

    val mainSource = extensions.getByType(SourceSetContainer::class.java).named("main")
    val classes = mainSource.map { it.compileClasspath }
    val source = mainSource
      .map { it.allJava }
      .map { it.sourceDirectories }
    val jdkHome = Jvm.current().javaHome.absolutePath

    val generateApi = tasks.register("generateApi", GenerateApiTask::class.java) { t ->
      t.metalava.setFrom(metalava)
      t.compileClasspath.setFrom(classes)
      t.jdkHome.set(jdkHome)

      t.sourceFiles.setFrom(source)
      t.output.set(layout.buildDirectory.file("reports/api/api.txt"))
    }

    val referenceApiFile = layout.projectDirectory.file("api/api.txt")

    tasks.register("updateApi", UpdateApiTask::class.java) { t ->
      t.input.set(generateApi.flatMap { it.output })
      t.output.set(referenceApiFile)
    }

    tasks.register("checkApi", CheckApiTask::class.java) { t ->
      t.projectPath.set(path)
      t.metalava.setFrom(metalava)
      t.compileClasspath.setFrom(classes)
      t.jdkHome.set(jdkHome)

      t.sourceFiles.setFrom(source)
      t.referenceApiFile.set(referenceApiFile)

      t.output.set(layout.buildDirectory.file("api/check.txt"))
    }
  }
}
