@file:Suppress("UnstableApiUsage")

package com.autonomousapps.services

import com.autonomousapps.Flags.cacheSize
import com.autonomousapps.model.InlineMemberCapability
import com.autonomousapps.model.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.intermediates.ExplodingJar
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.gradle.api.Project
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
  private val inlineMembers: Cache<String, Set<InlineMemberCapability.InlineMember>> = newCache()
  private val procs: Cache<String, AnnotationProcessorDependency> = newCache()

  internal fun explodedJar(name: String): ExplodingJar? = explodingJars.asMap()[name]
  internal fun explodedJars(name: String, explodingJar: ExplodingJar) {
    explodingJars.asMap().putIfAbsent(name, explodingJar)
  }

  internal fun inlineMember(name: String): Set<InlineMemberCapability.InlineMember>? = inlineMembers.asMap()[name]

  internal fun inlineMembers(name: String, members: Set<InlineMemberCapability.InlineMember>) {
    inlineMembers.asMap().putIfAbsent(name, members)
  }

  internal fun proc(procName: String): AnnotationProcessorDependency? = procs.asMap()[procName]
  internal fun procs(procName: String, proc: AnnotationProcessorDependency) {
    procs.asMap().putIfAbsent(procName, proc)
  }

  companion object {
    private const val SHARED_SERVICES_IN_MEMORY_CACHE = "inMemoryCache"
    private const val DEFAULT_CACHE_VALUE = -1L

    internal fun register(project: Project): Provider<InMemoryCache> = project
      .gradle
      .sharedServices
      .registerIfAbsent(SHARED_SERVICES_IN_MEMORY_CACHE, InMemoryCache::class.java) {
        parameters.cacheSize.set(project.cacheSize(DEFAULT_CACHE_VALUE))
      }
  }
}
