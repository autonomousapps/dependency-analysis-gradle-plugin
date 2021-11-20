@file:Suppress("UnstableApiUsage")

package com.autonomousapps.services

import com.autonomousapps.FLAG_MAX_CACHE_SIZE
import com.autonomousapps.internal.AnalyzedJar
import com.autonomousapps.internal.AnnotationProcessor
import com.autonomousapps.model.InlineMemberCapability
import com.autonomousapps.model.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.intermediates.ExplodingJar
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.gradle.api.GradleException
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentSkipListMap

abstract class InMemoryCache : BuildService<BuildServiceParameters.None> {
  private val jars: MutableMap<String, Int> = ConcurrentSkipListMap()
  private val classes: MutableMap<String, Int> = ConcurrentSkipListMap()

  internal fun updateJars(jarName: String) {
    jars.merge(jarName, 1) { oldValue, increment -> oldValue + increment }
  }

  internal fun updateClasses(className: String) {
    classes.merge(className, 1) { oldValue, increment -> oldValue + increment }
  }

  internal fun classes(): Map<String, Int> = classes

  /*
   * Caches.
   */

  private inline fun <reified K, reified V> newCache(maxSize: Long = maxSize()): Cache<K, V> {
    val builder = Caffeine.newBuilder()
    if (maxSize >= 0) builder.maximumSize(maxSize)
    return builder.build()
  }

  // TODO document and/or provide DSL
  private fun maxSize(): Long {
    val sysProp = System.getProperty(FLAG_MAX_CACHE_SIZE)
    return try {
      sysProp?.toLong() ?: -1
    } catch (e: Exception) {
      throw GradleException("$sysProp is not a valid cache size. Provide a long value", e)
    }
  }

  private val analyzedJars: Cache<String, AnalyzedJar> = newCache()
  private val explodingJars: Cache<String, ExplodingJar> = newCache()
  private val constantMembers: Cache<String, Set<String>> = newCache()
  private val inlineMembers: Cache<String, List<String>> = newCache()
  private val inlineMembers2: Cache<String, Set<InlineMemberCapability.InlineMember>> = newCache()
  private val procs: Cache<String, AnnotationProcessor> = newCache()
  private val procs2: Cache<String, AnnotationProcessorDependency> = newCache()

  // TODO remove
  internal fun analyzedJar(name: String): AnalyzedJar? = analyzedJars.asMap()[name]
  internal fun analyzedJars(name: String, analyzedJar: AnalyzedJar) {
    analyzedJars.asMap().putIfAbsent(name, analyzedJar)
  }

  internal fun explodedJar(name: String): ExplodingJar? = explodingJars.asMap()[name]
  internal fun explodedJars(name: String, explodingJar: ExplodingJar) {
    explodingJars.asMap().putIfAbsent(name, explodingJar)
  }

  fun constantMember(identifier: String): Set<String>? = constantMembers.asMap()[identifier]

  internal fun constantMembers(identifier: String, constants: Set<String>) {
    constantMembers.asMap().putIfAbsent(identifier, constants)
  }

  internal fun inlineMember(name: String): List<String>? = inlineMembers.asMap()[name]

  internal fun inlineMembers(name: String, members: List<String>) {
    inlineMembers.asMap().putIfAbsent(name, members)
  }

  internal fun inlineMember2(name: String): Set<InlineMemberCapability.InlineMember>? = inlineMembers2.asMap()[name]

  internal fun inlineMembers2(name: String, members: Set<InlineMemberCapability.InlineMember>) {
    inlineMembers2.asMap().putIfAbsent(name, members)
  }

  fun proc(procName: String): AnnotationProcessor? = procs.asMap()[procName]
  fun procs(procName: String, proc: AnnotationProcessor) {
    procs.asMap().putIfAbsent(procName, proc)
  }

  internal fun proc2(procName: String): AnnotationProcessorDependency? = procs2.asMap()[procName]
  internal fun procs2(procName: String, proc: AnnotationProcessorDependency) {
    procs2.asMap().putIfAbsent(procName, proc)
  }

  companion object {
    private const val SHARED_SERVICES_IN_MEMORY_CACHE = "inMemoryCache"

    internal fun register(gradle: Gradle): Provider<InMemoryCache> {
      return gradle.sharedServices.registerIfAbsent(SHARED_SERVICES_IN_MEMORY_CACHE, InMemoryCache::class.java) {}
    }
  }
}
