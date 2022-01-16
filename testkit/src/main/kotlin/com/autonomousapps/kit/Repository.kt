package com.autonomousapps.kit

class Repository(private val repo: String) {

  override fun toString(): String = repo

  companion object {
    @JvmStatic
    val GOOGLE = Repository("google()")

    @JvmStatic
    val MAVEN_CENTRAL = Repository("mavenCentral()")

    @JvmStatic
    val DEFAULT = listOf(
      GOOGLE,
      MAVEN_CENTRAL
    )

    @JvmStatic
    val LIBS = Repository("flatDir { 'libs' }")

    @JvmStatic
    val MAVEN_LOCAL = Repository("mavenLocal()")

    @JvmStatic
    fun ofMaven(repoUrl: String): Repository {
      return Repository("maven { url = \"$repoUrl\" }")
    }
  }
}
