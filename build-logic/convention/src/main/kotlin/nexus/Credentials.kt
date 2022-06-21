package nexus

import org.gradle.api.Project
import javax.inject.Inject

open class Credentials @Inject constructor(private val project: Project) {

  fun username(): String? = secret(USERNAME)
  fun password(): String? = secret(PASSWORD)

  private fun secret(name: String): String? {
    return (project.properties[name] ?: System.getenv(name))?.toString()
  }

  companion object {
    private const val USERNAME = "sonatypeUsername"
    private const val PASSWORD = "sonatypePassword"
  }
}
