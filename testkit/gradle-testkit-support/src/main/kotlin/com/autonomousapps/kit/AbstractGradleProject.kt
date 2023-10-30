package com.autonomousapps.kit

import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories

/**
 * Provides some common functionality for Gradle functional tests.
 */
public abstract class AbstractGradleProject @JvmOverloads constructor(
  buildPath: String = "build/functionalTest",
) {

  protected open fun newGradleProjectBuilder(): GradleProject.Builder {
    return GradleProject.Builder(rootDir.toFile(), GradleProject.DslKind.GROOVY)
  }

  /**
   * The root directory of a Gradle build. The default value is
   * ```
   * <PWD>/build/functionalTest/<ConcreteClassSimpleName>-<UUID>[-Gradle worker ID (if present)]
   * ```
   */
  public val rootDir: Path = File("$buildPath/${newSlug()}").toPath().createDirectories()

  private fun newSlug(): String {
    var worker = System.getProperty("org.gradle.test.worker", "")
    if (worker.isNotEmpty()) {
      worker = "-$worker"
    }
    return "${javaClass.simpleName}-${UUID.randomUUID().toString().take(8)}$worker"
  }
}
