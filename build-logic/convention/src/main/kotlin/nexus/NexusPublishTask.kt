package nexus

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@Suppress("UnstableApiUsage")
@UntrackedTask(because = "Not worth tracking")
abstract class NexusPublishTask : DefaultTask() {

  init {
    group = "publishing"
    description = "Closes and promotes from staging repository"
  }

  @get:Input
  abstract val username: Property<String>

  @get:Input
  abstract val password: Property<String>

  internal fun configureWith(credentials: Credentials) {
    username.set(credentials.username() ?: error("Username must not be null"))
    password.set(credentials.password() ?: error("Password must not be null"))
  }

  @TaskAction fun action() {
    val baseUrl = OSSRH_API_BASE_URL
    val groupId = GROUP

    Nexus(logger, username.get(), password.get(), groupId, baseUrl).closeAndReleaseRepository()
  }

  companion object {
    private const val OSSRH_API_BASE_URL = "https://oss.sonatype.org/service/local/"
    private const val GROUP = "com.autonomousapps"
  }
}
