package com.autonomousapps.internal.utils

import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal fun TaskContainer.namedOrNull(name: String): TaskProvider<Task>? = try {
  named(name)
} catch (_: UnknownTaskException) {
  null
}