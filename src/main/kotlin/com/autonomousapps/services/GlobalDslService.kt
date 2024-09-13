package com.autonomousapps.services

import com.autonomousapps.BuildHealthPlugin
import com.autonomousapps.extension.*
import com.autonomousapps.internal.utils.mapToMutableList
import com.autonomousapps.subplugin.DEPENDENCY_ANALYSIS_PLUGIN
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * This class is used alongside [DependencyAnalysisExtension][com.autonomousapps.DependencyAnalysisExtension] and
 * [DependencyAnalysisSubExtension][com.autonomousapps.DependencyAnalysisSubExtension] to safely (in isolated projects-
 * terms) configure the entire build, globally, without any subproject touching mutable properties of any other project.
 */
abstract class GlobalDslService @Inject constructor(
  objects: ObjectFactory,
) : BuildService<BuildServiceParameters.None> {

  // Used for validity check. The plugin must be registered on the root project.
  private var registeredOnRoot = false

  // Used for error message when AGP or KGP missing from classpath.
  private var registeredOnSettings = false

  internal fun setRegisteredOnRoot() {
    registeredOnRoot = true
  }

  internal fun setRegisteredOnSettings() {
    registeredOnSettings = true
  }

  internal fun notifyAgpMissing() {
    val msg = if (registeredOnSettings) {
      """
        Android Gradle Plugin (AGP) not found on classpath. This might be a classloader issue. For the Dependency 
        Analysis Gradle Plugin (DAGP) to be able to analyze Android projects, AGP must be loaded in the same class
        loader as DAGP, or a parent. One solution is to ensure your settings script looks like this:
        
          // settings.gradle[.kts]
          buildscript {
            repositories { ... }
            dependencies {
              classpath("com.android.tools.build:gradle:<<version>>")
            }
          }
          
          plugins {
            id("${BuildHealthPlugin.ID}") version "<<version>>"
            
            // Optional
            id("org.jetbrains.kotlin.android") version "<<version>>" apply false
          }
      """.trimIndent()
    } else {
      """
        Android Gradle Plugin (AGP) not found on classpath. This might be a classloader issue. For the Dependency 
        Analysis Gradle Plugin (DAGP) to be able to analyze Android projects, AGP must be loaded in the same class
        loader as DAGP, or a parent. One solution is to ensure your root build script looks like this:
        
          // root build.gradle[.kts]
          buildscript {
            repositories { ... }
            dependencies {
              classpath("com.android.tools.build:gradle:<<version>>")
            }
          }
          
          plugins {
            id("$DEPENDENCY_ANALYSIS_PLUGIN") version "<<version>>"
            
            // Optional
            id("org.jetbrains.kotlin.android") version "<<version>>" apply false
          }
      """.trimIndent()
    }

    error(msg)
  }

  internal fun notifyKgpMissing() {
    val msg = if (registeredOnSettings) {
      """
        Kotlin Gradle Plugin (KGP) not found on classpath. This might be a classloader issue. For the Dependency 
        Analysis Gradle Plugin (DAGP) to be able to analyze Kotlin projects, KGP must be loaded in the same class
        loader as DAGP, or a parent. One solution is to ensure your settings script looks like this:
        
          // settings.gradle[.kts]
          plugins {
            id("${BuildHealthPlugin.ID}") version "<<version>>"
            id("org.jetbrains.kotlin.<jvm|android|etc>)" version "<<version>>" apply false
          }
      """.trimIndent()
    } else {
      """
        Kotlin Gradle Plugin (KGP) not found on classpath. This might be a classloader issue. For the Dependency 
        Analysis Gradle Plugin (DAGP) to be able to analyze Kotlin projects, KGP must be loaded in the same class
        loader as DAGP, or a parent. One solution is to ensure your root build script looks like this:
        
          // root build.gradle[.kts]
          plugins {
            id("$DEPENDENCY_ANALYSIS_PLUGIN") version "<<version>>"
            id("org.jetbrains.kotlin.<jvm|android|etc>)" version "<<version>>" apply false
          }
      """.trimIndent()
    }

    error(msg)
  }

  // Global handlers, one instance each for the whole build.
  internal val abiHandler: AbiHandler = objects.newInstance()
  internal val dependenciesHandler: DependenciesHandler = objects.newInstance()
  internal val usagesHandler: UsagesHandler = objects.newInstance()

  /**
   * Hydrate dependencies map with version catalog entries.
   */
  internal fun withVersionCatalogs(project: Project) {
    dependenciesHandler.withVersionCatalogs(project)
  }

  /*
   * Issues Handler, one instance per project.
   */

  private val undefined = objects.newInstance<Issue>()
  private val defaultBehavior = objects.property<Behavior>().convention(Warn())

  private val all = objects.newInstance(ProjectIssueHandler::class.java, "__all")
  private val projects = objects.domainObjectContainer(ProjectIssueHandler::class.java)

  internal fun all(action: Action<ProjectIssueHandler>) {
    action.execute(all)
  }

  internal fun project(projectPath: String, action: Action<ProjectIssueHandler>) {
    projects.maybeCreate(projectPath).apply {
      action.execute(this)
    }
  }

  internal fun shouldAnalyzeSourceSet(sourceSetName: String, projectPath: String): Boolean {
    val a = sourceSetName !in all.ignoreSourceSets.get()
    val b = sourceSetName !in projects.findByName(projectPath)?.ignoreSourceSets?.get().orEmpty()

    return a && b
  }

  internal fun anyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.anyIssue }
  }

  internal fun unusedDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.unusedDependenciesIssue }
  }

  internal fun usedTransitiveDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.usedTransitiveDependenciesIssue }
  }

  internal fun incorrectConfigurationIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.incorrectConfigurationIssue }
  }

  internal fun compileOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.compileOnlyIssue }
  }

  internal fun runtimeOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.runtimeOnlyIssue }
  }

  internal fun unusedAnnotationProcessorsIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.unusedAnnotationProcessorsIssue }
  }

  internal fun redundantPluginsIssueFor(projectPath: String): Provider<Behavior> {
    return overlay(all.redundantPluginsIssue, projects.findByName(projectPath)?.redundantPluginsIssue)
  }

  internal fun moduleStructureIssueFor(projectPath: String): Provider<Behavior> {
    return overlay(all.moduleStructureIssue, projects.findByName(projectPath)?.moduleStructureIssue)
  }

  private fun issuesFor(projectPath: String, mapper: (ProjectIssueHandler) -> Issue): List<Provider<Behavior>> {
    val projectHandler = projects.findByName(projectPath)

    val allIssuesBySourceSet = all.issuesBySourceSet(mapper)
    val projectIssuesBySourceSet = projectHandler.issuesBySourceSet(mapper)
    val globalProjectMatches = mutableListOf<Pair<Issue, Issue?>>()

    // Iterate through the global/all list first
    val iter = allIssuesBySourceSet.iterator()
    while (iter.hasNext()) {
      // Find matching (by sourceSetName) elements in both lists
      val a = iter.next()
      val b = projectIssuesBySourceSet.find { p -> p.sourceSet.get() == a.sourceSet.get() }

      // Drain both lists
      iter.remove()
      if (b != null) projectIssuesBySourceSet.remove(b)

      // Add to result list (it's ok if `b` is null)
      globalProjectMatches.add(a to b)
    }

    // Now iterate through the remaining elements of the proj list
    projectIssuesBySourceSet.forEach { b ->
      val a = allIssuesBySourceSet.find { a -> a.sourceSet.get() == b.sourceSet.get() }

      // In contrast to the above, it is NOT ok if `a` is null, so we use `undefined` instead.
      if (a != null) {
        globalProjectMatches.add(a to b)
      } else {
        globalProjectMatches.add(undefined to b)
      }
    }

    val primaryBehavior = overlay(mapper(all), projectHandler?.let(mapper))
    val result = mutableListOf(primaryBehavior)
    globalProjectMatches.mapTo(result) { (global, project) ->
      overlay(global, project, primaryBehavior)
    }

    return result
  }

  private fun ProjectIssueHandler?.issuesBySourceSet(mapper: (ProjectIssueHandler) -> Issue): MutableList<Issue> {
    return this?.sourceSets.mapToMutableList { s ->
      s.issueOf(mapper)
    }
  }

  /** Project severity wins over global severity. Excludes are unioned. */
  private fun overlay(global: Issue, project: Issue?, coerceTo: Provider<Behavior>? = null): Provider<Behavior> {
    val c = coerceTo ?: defaultBehavior

    // If there's no project-specific handler, just return the global handler
    return if (project == null) {
      c.flatMap { coerce ->
        global.behavior().map { g ->
          if (g is Undefined) {
            when (coerce) {
              is Fail -> Fail(filter = g.filter, sourceSetName = g.sourceSetName)
              is Warn, is Undefined -> Warn(filter = g.filter, sourceSetName = g.sourceSetName)
              is Ignore -> Ignore(sourceSetName = g.sourceSetName)
            }
          } else {
            g
          }
        }
      }
    } else {
      global.behavior().flatMap { g ->
        val allFilter = g.filter
        project.behavior().map { p ->
          val projFilter = p.filter
          val union = allFilter + projFilter

          when (p) {
            is Fail -> Fail(filter = union, sourceSetName = p.sourceSetName)
            is Warn -> Warn(filter = union, sourceSetName = p.sourceSetName)
            is Ignore -> Ignore(sourceSetName = p.sourceSetName)
            is Undefined -> {
              when (g) {
                is Fail -> Fail(filter = union, sourceSetName = p.sourceSetName)
                is Warn -> Warn(filter = union, sourceSetName = p.sourceSetName)
                is Undefined -> Warn(filter = union, sourceSetName = p.sourceSetName)
                is Ignore -> Ignore(sourceSetName = p.sourceSetName)
              }
            }
          }
        }
      }
    }
  }

  internal companion object {
    fun of(project: Project): Provider<GlobalDslService> = of(project.gradle)

    fun of(gradle: Gradle): Provider<GlobalDslService> {
      return gradle
        .sharedServices
        .registerIfAbsent("dagpDslService", GlobalDslService::class.java) {}
    }
  }
}
