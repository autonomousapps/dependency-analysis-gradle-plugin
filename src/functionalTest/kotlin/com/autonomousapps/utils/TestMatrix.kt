package com.autonomousapps.utils

import org.gradle.util.GradleVersion

/**
 * Testing against AGP versions:
 * - 3.5.3
 * - 3.6.0-rc03
 * - 4.0.0-alpha09, whose min Gradle version is 6.1-rc-1
 */
class TestMatrix(
    val agpVersion: String,
    val gradleVersions: List<GradleVersion> = listOf(
        GradleVersion.version("5.6.4"),
        GradleVersion.version("6.0.1"),
        GradleVersion.version("6.1.1"),
        GradleVersion.version("6.2.1")
    )
) : Iterable<Pair<GradleVersion, String>> {

    private val matrix = gradleVersions.map { gradleVersion ->
        gradleVersion to agpVersion
    }.filterNot { (gradleVersion, agpVersion) ->
        agpVersion.startsWith("4.") && !gradleVersion.version.startsWith("6.1")
    }

    override fun iterator(): Iterator<Pair<GradleVersion, String>> {
        return matrix.iterator()
    }
}

/**
 * A poor man's "when" block (from Spock).
 */
fun Iterable<Pair<GradleVersion, String>>.forEachPrinting(action: (Pair<GradleVersion, String>) -> Unit) {
    for ((gradleVersion, agpVersion) in this) {
        println("Testing against Gradle ${gradleVersion.version}")
        println("Testing against AGP $agpVersion")
        action(gradleVersion to agpVersion)
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
