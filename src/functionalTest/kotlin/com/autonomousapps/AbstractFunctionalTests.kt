package com.autonomousapps

import com.autonomousapps.fixtures.LibrarySpec
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.fixtures.WORKSPACE
import com.autonomousapps.internal.*
import com.autonomousapps.utils.TestMatrix
import org.apache.commons.io.FileUtils
import java.io.File
import java.lang.AssertionError
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull

abstract class AbstractFunctionalTests {

    private val agpVersion = System.getProperty("com.autonomousapps.agpversion")
        ?: error("Must supply an AGP version")
    protected val testMatrix = TestMatrix(agpVersion)

    @BeforeTest fun cleanWorkspace() {
        // Same as androidProject.projectDir, but androidProject has not been instantiated yet
        FileUtils.deleteDirectory(File(WORKSPACE))
    }

    protected fun ProjectDirProvider.unusedDependenciesFor(spec: LibrarySpec): List<String> {
        return unusedDependenciesFor(spec.name)
    }

    protected fun ProjectDirProvider.unusedDependenciesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<UnusedDirectComponent>()
            .map { it.dependency.identifier }
    }

    protected fun ProjectDirProvider.completelyUnusedDependenciesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<UnusedDirectComponent>()
            .filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency.identifier }
    }

    protected fun ProjectDirProvider.abiReportFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getAbiAnalysisPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<Dependency>()
            .map { it.identifier }
    }

    protected fun ProjectDirProvider.allUsedClassesFor(spec: LibrarySpec): List<String> {
        return allUsedClassesFor(spec.name)
    }

    protected fun ProjectDirProvider.allUsedClassesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getAllUsedClassesPath(getVariantOrError(moduleName))}")
            .readLines()
    }

    private fun ProjectDirProvider.getVariantOrError(moduleName: String): String {
        return project(moduleName).variant ?: error("No variant associated with module named $moduleName")
    }

    /**
     * Asserts that every element of [this] list has a corresponding member in [actual]. Checks that _some_ element of
     * `actual` ends with _each_ element of `this`.
     *
     * Will throw an [AssertionError] if the size of `actual` is less than the size of `this`.
     */
    protected infix fun List<String>.shouldBeIn(actual: List<String>) {
        val expected = this
        if (actual.size < expected.size) {
            throw AssertionError("Actual list smaller than expected list. Was $actual")
        }

        for (element in expected) {
            assertNotNull(
                actual.find { it.endsWith(element) },
                "$actual does not contain an element like $element"
            )
        }
    }

    /**
     * A convenience function for when we have a list of only one element.
     */
    protected infix fun String.shouldBeIn(actual: List<String>) {
        listOf(this) shouldBeIn actual
    }
}
