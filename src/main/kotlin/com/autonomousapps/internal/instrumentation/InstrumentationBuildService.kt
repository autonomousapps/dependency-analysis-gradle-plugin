@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.instrumentation

import com.autonomousapps.internal.AnalyzedJar
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

abstract class InstrumentationBuildService : BuildService<BuildServiceParameters.None> {
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

  // AnalyzedJar cache
  internal val analyzedJars: MutableMap<String, AnalyzedJar> = ConcurrentHashMap()

  // Constant members cache
  internal val constantMembers: MutableMap<String, Set<String>> = ConcurrentHashMap()

  // Inline members cache
  internal val inlineMembers: MutableMap<String, List<String>> = ConcurrentHashMap()
}
