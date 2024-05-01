// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.services

import com.autonomousapps.Flags.cacheSize
import com.autonomousapps.model.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.intermediates.ExplodingJar
import com.autonomousapps.subplugin.DEPENDENCY_ANALYSIS_PLUGIN
import com.autonomousapps.tasks.KotlinCapabilities
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class InMemoryCache : BuildService<InMemoryCache.Params> {

  interface Params : BuildServiceParameters {
    val cacheSize: Property<Long>
  }

  private val cacheSize = parameters.cacheSize.get()

  private inline fun <reified K, reified V> newCache(maxSize: Long = cacheSize): Cache<K, V> {
    val builder = Caffeine.newBuilder()
    if (maxSize >= 0) builder.maximumSize(maxSize)
    return builder.build()
  }

  private val explodingJars: Cache<String, ExplodingJar> = newCache()
  private val kotlinCapabilities: Cache<String, KotlinCapabilities> = newCache()
  private val procs: Cache<String, AnnotationProcessorDependency> = newCache()

  internal fun explodedJar(name: String): ExplodingJar? = explodingJars.asMap()[name]
  internal fun explodedJars(name: String, explodingJar: ExplodingJar) {
    explodingJars.asMap().putIfAbsent(name, explodingJar)
  }

  internal fun kotlinCapabilities(name: String): KotlinCapabilities? = kotlinCapabilities.asMap()[name]

  internal fun inlineMembers(name: String, capabilities: KotlinCapabilities) {
    kotlinCapabilities.asMap().putIfAbsent(name, capabilities)
  }

  internal fun proc(procName: String): AnnotationProcessorDependency? = procs.asMap()[procName]
  internal fun procs(procName: String, proc: AnnotationProcessorDependency) {
    procs.asMap().putIfAbsent(procName, proc)
  }

  companion object {
    private const val SHARED_SERVICES_IN_MEMORY_CACHE = "inMemoryCache"
    private const val DEFAULT_CACHE_VALUE = -1L

    // To share service across the whole build tree - https://github.com/gradle/gradle/issues/14697
    private fun Gradle.rootBuild(): Gradle = parent?.rootBuild() ?: this

    /**
     * Determines the build on which to register the service. In a composite build, the root build is used to share the
     * cache across builds if the root build used that same classloader for loading the plugin as the current build.
     * See: https://github.com/gradle/gradle/issues/17559
     */
    private fun Project.serviceHoldingBuild(): Gradle {
      val thisBuild = gradle
      val rootBuild = thisBuild.rootBuild()

      if (thisBuild == rootBuild) {
        return thisBuild
      }

      val thisPluginInstance = thisBuild.rootProject.plugins.findPlugin(DEPENDENCY_ANALYSIS_PLUGIN)
      val rootPluginInstance = rootBuild.rootProject.plugins.findPlugin(DEPENDENCY_ANALYSIS_PLUGIN)

      if (thisPluginInstance == null || rootPluginInstance == null) {
        return thisBuild
      }

      return if (thisPluginInstance::class.java == rootPluginInstance::class.java) {
        rootBuild // shared cache in the root if plugin was loaded with same classloader
      } else {
        thisBuild
      }
    }

    internal fun register(project: Project): Provider<InMemoryCache> = project
      .serviceHoldingBuild()
      .sharedServices
      .registerIfAbsent(SHARED_SERVICES_IN_MEMORY_CACHE, InMemoryCache::class.java) {
        parameters.cacheSize.set(project.cacheSize(DEFAULT_CACHE_VALUE))
      }
  }
}
