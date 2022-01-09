@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   usages {
 *     exclusions {
 *       excludeClasses(".*\\.internal\\..*")
 *     }
 *   }
 * }
 * ```
 */
open class UsagesHandler @Inject constructor(objects: ObjectFactory) {

  internal val exclusionsHandler: UsagesExclusionsHandler = objects.newInstance(UsagesExclusionsHandler::class)

  fun exclusions(action: Action<UsagesExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}

abstract class UsagesExclusionsHandler @Inject constructor(objects: ObjectFactory) {

  internal val classExclusions = objects.setProperty<String>().convention(emptySet())

  fun excludeClasses(@Language("RegExp") vararg classRegexes: String) {
    classExclusions.addAll(*classRegexes)
  }
}
