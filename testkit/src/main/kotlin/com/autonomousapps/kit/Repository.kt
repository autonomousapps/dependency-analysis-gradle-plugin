package com.autonomousapps.kit

class Repository(private val repo: String) {

  override fun toString(): String = repo

  companion object {
    @JvmField val GOOGLE = Repository("google()")
    @JvmField val MAVEN_CENTRAL = Repository("mavenCentral()")
    @JvmField val SNAPSHOTS = ofMaven("https://oss.sonatype.org/content/repositories/snapshots/")
    @JvmField val LIBS = Repository("flatDir { 'libs' }")
    @JvmField val MAVEN_LOCAL = Repository("mavenLocal()")

    @JvmField
    val DEFAULT = listOf(
      GOOGLE,
      MAVEN_CENTRAL,
    )

    @JvmStatic
    fun ofMaven(repoUrl: String): Repository {
      return Repository("maven { url = \"$repoUrl\" }")
    }
  }
}
