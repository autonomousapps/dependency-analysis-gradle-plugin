package com.autonomousapps.kit

class Repository(val repo: String) {

  override fun toString(): String = repo

  companion object {
    @JvmStatic
    val DEFAULT = listOf(
      Repository("google()"),
      Repository("jcenter()"),
      Repository("maven { url = \"https://dl.bintray.com/kotlin/kotlin-eap\" }")
    )
    val LIBS = Repository("flatDir { 'libs' }")

    @JvmStatic
    fun ofMaven(repoUrl: String): Repository {
      return Repository("maven { url = \"$repoUrl\" }")
    }
  }
}
