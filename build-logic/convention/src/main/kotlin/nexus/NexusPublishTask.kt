// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package nexus

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Not worth tracking")
abstract class NexusPublishTask : DefaultTask() {

  init {
    group = "publishing"
    description = "Closes and promotes from staging repository"
  }

  @get:Optional // will be null for users who can't publish (most of them)
  @get:Input
  abstract val username: Property<String>

  @get:Optional // will be null for users who can't publish (most of them)
  @get:Input
  abstract val password: Property<String>

  internal fun configureWith(credentials: Credentials) {
    username.set(credentials.username())
    password.set(credentials.password())
  }

  @TaskAction fun action() {
    val baseUrl = OSSRH_API_BASE_URL
    val groupId = GROUP

    val username = username.orNull ?: error("Username must not be null")
    val password = password.orNull ?: error("Password must not be null")

    Nexus(logger, username, password, groupId, baseUrl).closeAndReleaseRepository()
  }

  companion object {
    private const val OSSRH_API_BASE_URL = "https://oss.sonatype.org/service/local/"
    private const val GROUP = "com.autonomousapps"
  }
}
