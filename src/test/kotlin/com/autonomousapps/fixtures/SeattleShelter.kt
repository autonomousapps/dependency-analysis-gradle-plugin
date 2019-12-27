package com.autonomousapps.fixtures

import com.autonomousapps.utils.fileFromResource
import com.autonomousapps.utils.pathFromResource
import java.io.File
import java.nio.file.Path

/**
 * Testing against an open source Android project,
 * [seattle-shelter][https://gitlab.com/autonomousapps/seattle-shelter-android]. VCS revision
 * [726b501a][https://gitlab.com/autonomousapps/seattle-shelter-android/tree/726b501a1df34eddea9a0879b8cbdc0813c4cebc].
 * Relevant files have been copied directly into test/resources for ease of test development.
 *
 * Treating it as a golden value.
 */
class SeattleShelter {

    private val root = "shelter"

    val core = AndroidLibraryModule("$root/core")
    val db = AndroidLibraryModule("$root/db")

    class AndroidLibraryModule(private val root: String) {

        fun jarFile(): File = fileFromResource("$root/classes.jar")
        fun layoutsPath(): Path = pathFromResource("$root/layouts")
        fun kaptStubsPath(): Path = pathFromResource("$root/kapt-stubs")

        fun classReferencesInJar() =
            fileFromResource("$root/classes-jar-expected.txt").readLines()

        fun classReferencesInLayouts() =
            fileFromResource("$root/classes-layouts-expected.txt").readLines()

        fun classReferencesInKaptStubs() =
            fileFromResource("$root/kapt-stubs-expected.txt").readLines()
    }
}
