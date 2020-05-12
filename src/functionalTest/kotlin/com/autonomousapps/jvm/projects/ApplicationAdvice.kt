package com.autonomousapps.jvm.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency

/**
 * This file exists as a companion to ApplicationProject in the Groovy sourceset. Groovy gets confused
 * by Advice.remove(), etc, as it can't distinguish it from from advice.isRemove. I should rename
 * those static methods to Advice.for...(), but should wait till several outstanding PRs have been
 * resolved.
 */
object ApplicationAdvice {
  private val COMMONS_MATH = Dependency("org.apache.commons:commons-math3", "3.6.1", "compile")
  private val COMMONS_IO = Dependency("commons-io:commons-io", "2.6", "compile")
  private val COMMONS_COLLECTIONS = Dependency("org.apache.commons:commons-collections4", "4.4", "compile")

  val expectedAdvice = listOf(
    Advice.remove(COMMONS_MATH),
    Advice.change(COMMONS_IO, "implementation"),
    Advice.change(COMMONS_COLLECTIONS, "implementation")
  )
}
