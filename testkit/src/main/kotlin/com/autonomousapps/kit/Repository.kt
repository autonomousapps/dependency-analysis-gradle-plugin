package com.autonomousapps.kit

class Repository(val repo: String) {

  override fun toString(): String = repo

  companion object {
    val DEFAULT = listOf(
      Repository("google()"),
      Repository("jcenter()"),
      Repository("maven { url = \"https://dl.bintray.com/kotlin/kotlin-eap\" }")
    )
  }
}
