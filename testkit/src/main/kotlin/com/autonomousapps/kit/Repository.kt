package com.autonomousapps.kit

class Repository(val repo: String) {

  override fun toString(): String = repo

  companion object {
    @JvmStatic
    val DEFAULT = listOf(
      Repository("google()"),
      Repository("mavenCentral()")
    )
    val LIBS = Repository("flatDir { 'libs' }")

    @JvmStatic
    fun ofMaven(repoUrl: String): Repository {
      return Repository("maven { url = \"$repoUrl\" }")
    }
  }
}
