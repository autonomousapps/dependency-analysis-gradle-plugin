package nexus

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class NexusPublishTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

  init {
    group = "publishing"
    description = "Closes and promotes from staging repository"

    @Suppress("LeakingThis")
    notCompatibleWithConfigurationCache("Cannot use Project in task action")
  }

  companion object {
    private const val OSSRH_API_BASE_URL = "https://oss.sonatype.org/service/local/"
    private const val GROUP = "com.autonomousapps"
  }

  private val credentials = objects.newInstance(Credentials::class.java, project)

  @TaskAction fun action() {
    val baseUrl = OSSRH_API_BASE_URL
    val groupId = GROUP

    val repositoryUsername = credentials.username() ?: error("Username must not be null")
    val repositoryPassword = credentials.password() ?: error("Password must not be null")

    Nexus(logger, repositoryUsername, repositoryPassword, groupId, baseUrl)
      .closeAndReleaseRepository()
  }
}
