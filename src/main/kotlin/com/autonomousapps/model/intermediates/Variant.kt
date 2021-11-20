package com.autonomousapps.model.intermediates

// TODO replace with value class when Gradle supports targeting Kotlin 1.5+
//  https://kotlinlang.org/docs/inline-classes.html
//  https://blog.jetbrains.com/kotlin/2021/02/new-language-features-preview-in-kotlin-1-4-30/#inline-value-classes-stabilization
/** A simple wrapper around an Android variant / JVM source set's "name" for improved semantics. */
internal inline class Variant(val value: String) {

  companion object {
    val MAIN = Variant("main")
    fun String.into() = Variant(this)
  }
}
