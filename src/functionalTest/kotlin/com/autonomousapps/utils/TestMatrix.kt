package com.autonomousapps.utils

import org.gradle.util.GradleVersion

/**
 * Testing against AGP versions:
 * - 3.5.3
 * - 3.6.1
 * - 4.0.0-beta01, whose min Gradle version is 6.1
 * - 4.1.0-alpha02, whose min Gradle version is 6.2.1
 */
class TestMatrix(
    val agpVersion: String,
    val gradleVersions: List<GradleVersion> = listOf(
        GradleVersion.version("5.6.4"),
        GradleVersion.version("6.0.1"),
        GradleVersion.version("6.1.1"),
        GradleVersion.version("6.2.2")
    )
) : Iterable<Pair<GradleVersion, String>> {

  private val matrix = gradleVersions.map { gradleVersion ->
    gradleVersion to agpVersion
  }.filterNot { (gradleVersion, agpVersion) ->
    agpVersion.startsWith("4.") && !gradleVersion.version.startsWith("6.1") ||
        agpVersion.startsWith("4.1") && !gradleVersion.version.startsWith("6.2")
  }

  override fun iterator(): Iterator<Pair<GradleVersion, String>> {
    return matrix.iterator()
  }
}

/**
 * A poor man's "when" block (from Spock).
 */
fun List<GradleVersion>.forEachPrinting(action: (GradleVersion) -> Unit) {
  for (element in this) {
    println("Testing against Gradle ${element.version}")
    action(element)
  }
}
