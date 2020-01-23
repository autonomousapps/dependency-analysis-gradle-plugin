package com.autonomousapps

import com.autonomousapps.fixtures.LibrarySpec
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.fixtures.WORKSPACE
import com.autonomousapps.internal.*
import com.autonomousapps.utils.TestMatrix
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.BeforeTest

abstract class AbstractFunctionalTests {

    private val agpVersion = System.getProperty("com.autonomousapps.agpversion")
        ?: error("Must supply an AGP version")
    protected val testMatrix = TestMatrix(agpVersion)

    @BeforeTest fun cleanWorkspace() {
        // Same as androidProject.projectDir, but androidProject has not been instantiated yet
        FileUtils.deleteDirectory(File(WORKSPACE))
    }

    protected fun ProjectDirProvider.unusedDependenciesFor(spec: LibrarySpec): List<String> =
        unusedDependenciesFor(spec.name)

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

    protected fun ProjectDirProvider.allUsedClassesFor(spec: LibrarySpec): List<String> = allUsedClassesFor(spec.name)

    protected fun ProjectDirProvider.allUsedClassesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getAllUsedClassesPath(getVariantOrError(moduleName))}")
            .readLines()
    }

    protected fun ProjectDirProvider.adviceFor(spec: LibrarySpec): Set<Advice> = adviceFor(spec.name)

    protected fun ProjectDirProvider.adviceFor(moduleName: String): Set<Advice> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getAdvicePath(getVariantOrError(moduleName))}")
            .readText()
            .fromJsonList<Advice>()
            .toSortedSet()
    }

    private fun ProjectDirProvider.getVariantOrError(moduleName: String): String {
        return project(moduleName).variant ?: error("No variant associated with module named $moduleName")
    }
}
