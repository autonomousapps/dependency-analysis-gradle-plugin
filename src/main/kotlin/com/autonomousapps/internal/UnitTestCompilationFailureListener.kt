package com.autonomousapps.internal

import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskState
import java.util.*

/**
 * Resulted from the investigation of
 * [https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/205]
 */
internal class UnitTestCompilationFailureListener(
  private val logger: Logger
) : TaskExecutionListener {

  companion object {
    private const val KOTLIN_COMPILE = "org.jetbrains.kotlin.gradle.tasks.KotlinCompile"
    private const val ANDROID_UNIT_TEST_SUFFIX = "UnitTestKotlin"
  }

  override fun beforeExecute(task: Task) {}

  override fun afterExecute(task: Task, state: TaskState) {
    if (shouldLog(task)) {
      if (state.failure != null) {
        val variant = task.name
          .removePrefix("compile")
          .removeSuffix("UnitTestKotlin")
          .toLowerCase(Locale.ROOT)
        val msg = """
            Compilation failed for unit tests of variant $variant.
            Consider excluding this test variant if you don't use it (since AGP 4.0.0):
            android {
              onVariants {
                if (buildType == '$variant') {
                  unitTest {
                    enabled = false
                  }
                }
              }
            }
          """.trimIndent()
        logger.quiet(msg)
      }
    }
  }

  private fun shouldLog(task: Task): Boolean =
    task.javaClass.canonicalName.startsWith(KOTLIN_COMPILE) &&
      task.name.endsWith(ANDROID_UNIT_TEST_SUFFIX)
}
