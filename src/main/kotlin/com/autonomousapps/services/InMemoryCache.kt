@file:Suppress("UnstableApiUsage")

package com.autonomousapps.services

import com.autonomousapps.internal.AnalyzedJar
import com.autonomousapps.internal.AnnotationProcessor
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
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

  internal fun jars(): Map<String, Int> = jars
  internal fun classes(): Map<String, Int> = classes

  internal val largestJarCount by lazy { jars.maxBy { it.value } }
  internal val largestClassesCount by lazy { classes.maxBy { it.value } }

  // Caches
  internal val analyzedJars: MutableMap<String, AnalyzedJar> = ConcurrentHashMap()
  internal val constantMembers: MutableMap<String, Set<String>> = ConcurrentHashMap()
  internal val inlineMembers: MutableMap<String, List<String>> = ConcurrentHashMap()
  internal val procs: MutableMap<String, AnnotationProcessor> = ConcurrentHashMap()
}
