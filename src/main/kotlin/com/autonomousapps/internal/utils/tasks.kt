package com.autonomousapps.internal.utils

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal fun TaskContainer.namedOrNull(name: String): TaskProvider<Task>? = if (names.contains(name)) {
  named(name)
} else {
  null
}
