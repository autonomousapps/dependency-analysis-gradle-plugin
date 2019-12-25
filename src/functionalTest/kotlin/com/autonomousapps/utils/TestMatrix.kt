package com.autonomousapps.utils

import org.gradle.util.GradleVersion

class TestMatrix(
    gradleVersions: List<GradleVersion> = listOf(
        GradleVersion.version("5.6.4"),
        GradleVersion.version("6.0.1")
    ),
    agpVersions: List<String> = listOf(
        "3.5.3",
        "3.6.0-rc01",
        "4.0.0-alpha07"
    )
) : Iterable<Pair<GradleVersion, String>> {

    private val matrix = gradleVersions.flatMap { gradleVersion ->
        agpVersions.map { agpVersion ->
            gradleVersion to agpVersion
        }
    }

    override fun iterator(): Iterator<Pair<GradleVersion, String>> {
        return matrix.iterator()
    }
}
