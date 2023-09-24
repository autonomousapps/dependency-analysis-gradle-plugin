package com.autonomousapps.internal.utils

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

internal fun <T> ListProperty<T>.addAll(elements: Iterable<Provider<out T>>) {
  elements.forEach { add(it) }
}
