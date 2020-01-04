@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Abstraction for differentiating between android-app, android-lib, and java-lib projects.
 */
internal interface DependencyAnalyzer<T : ClassAnalysisTask> {
    /**
     * E.g., `flavorDebug`
     */
    val variantName: String
    /**
     * E.g., `FlavorDebug`
     */
    val variantNameCapitalized: String

    val compileConfigurationName: String
    val runtimeConfigurationName: String

    val attribute: Attribute<String>
    val attributeValue: String

    val kotlinSourceFiles: FileTree

    /**
     * This produces a report that lists all of the used classes (FQCN) in the project.
     */
    fun registerClassAnalysisTask(): TaskProvider<out T>

    /**
     * This is a no-op for com.android.application projects, since they have no meaningful ABI.
     */
    fun registerAbiAnalysisTask(
        dependencyReportTask: TaskProvider<DependencyReportTask>
    ): TaskProvider<AbiAnalysisTask>? = null
}

/**
 * Base class for analyzing an Android project (com.android.application or com.android.library only).
 */
internal abstract class AndroidAnalyzer<T : ClassAnalysisTask>(
    protected val project: Project,
    protected val variant: BaseVariant
) : DependencyAnalyzer<T> {

    final override val variantName: String = variant.name
    final override val variantNameCapitalized: String = variantName.capitalize()
    final override val compileConfigurationName = "${variantName}CompileClasspath"
    final override val runtimeConfigurationName = "${variantName}RuntimeClasspath"
    final override val attribute: Attribute<String> = AndroidArtifacts.ARTIFACT_TYPE
    final override val attributeValue = "android-classes"
    final override val kotlinSourceFiles: FileTree = getSourceDirectories()

    protected fun getKaptStubs() = getKaptStubs(project, variantName)

    private fun getSourceDirectories(): FileTree {
        val javaDirs = variant.sourceSets.flatMap {
            it.javaDirectories
        }.filter { it.exists() }

        val kotlinDirs = javaDirs
            .map { it.path }
            .map { it.removeSuffix("java") + "kotlin" }
            .map { File(it) }
            .filter { it.exists() }

        return project.files(javaDirs + kotlinDirs).asFileTree.matching {
            include("**/*.kt")
        }
    }
}

internal class AndroidAppAnalyzer(
    project: Project, variant: BaseVariant
) : AndroidAnalyzer<ClassListAnalysisTask>(project, variant) {

    override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
        // Known to exist in Kotlin 1.3.50.
        val kotlinCompileTask = project.tasks.named("compile${variantNameCapitalized}Kotlin") // KotlinCompile
        // Known to exist in AGP 3.5 and 3.6, albeit with different backing classes (AndroidJavaCompile and JavaCompile)
        val javaCompileTask = project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")

        return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
            kotlinClasses.from(kotlinCompileTask.get().outputs.files.asFileTree)
            javaClasses.from(javaCompileTask.get().outputs.files.asFileTree)
            kaptJavaStubs.from(getKaptStubs())
            layouts(variant.sourceSets.flatMap { it.resDirectories })

            output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
        }
    }
}

internal class AndroidLibAnalyzer(
    project: Project, variant: BaseVariant
) : AndroidAnalyzer<JarAnalysisTask>(project, variant) {

    // Known to exist in AGP 3.5 and 3.6
    private fun getBundleTask() =
        project.tasks.named("bundleLibCompile$variantNameCapitalized", BundleLibraryClasses::class.java)

    override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
        // Known to exist in AGP 3.5 and 3.6
        val bundleTask =
            project.tasks.named("bundleLibCompile$variantNameCapitalized", BundleLibraryClasses::class.java)

        return project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
            jar.set(bundleTask.flatMap { it.output })
            kaptJavaStubs.from(getKaptStubs())
            layouts(variant.sourceSets.flatMap { it.resDirectories })

            output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
        }
    }

    override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
        project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
            jar.set(getBundleTask().flatMap { it.output })
            dependencies.set(dependencyReportTask.flatMap { it.output })

            output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
            abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
        }
}

internal class JavaLibAnalyzer(
    private val project: Project,
    private val sourceSet: SourceSet
) : DependencyAnalyzer<JarAnalysisTask> {

    override val variantName: String = sourceSet.name
    override val variantNameCapitalized = variantName.capitalize()
    // Yes, these two are the same for this case
    override val compileConfigurationName = "compileClasspath"
    override val runtimeConfigurationName = compileConfigurationName
    override val attribute: Attribute<String> = Attribute.of("artifactType", String::class.java)
    override val attributeValue = "jar"

    override val kotlinSourceFiles: FileTree = getSourceDirectories()

    private fun getJarTask() = project.tasks.named(sourceSet.jarTaskName, Jar::class.java)

    private fun getSourceDirectories(): FileTree {
        val javaAndKotlinSource = sourceSet.allJava.sourceDirectories
        return project.files(javaAndKotlinSource).asFileTree.matching {
            include("**/*.kt")
        }
    }

    override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
        return project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
            jar.set(getJarTask().flatMap { it.archiveFile })
            kaptJavaStubs.from(getKaptStubs(project, variantName))
            output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
        }
    }

    override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
        project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
            jar.set(getJarTask().flatMap { it.archiveFile })
            dependencies.set(dependencyReportTask.flatMap { it.output })

            output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
            abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
        }
}

// Best guess as to path to kapt-generated Java stubs
internal fun getKaptStubs(project: Project, variantName: String) = project.layout.buildDirectory.asFileTree.matching {
    include("**/kapt*/**/${variantName}/**/*.java")
}
