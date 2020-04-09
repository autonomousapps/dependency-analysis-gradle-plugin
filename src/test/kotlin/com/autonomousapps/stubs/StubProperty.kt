@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import org.gradle.api.Transformer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class StubProperty<T>(
  private val thing: T
) : Property<T> {

  override fun get(): T {
    return thing
  }

  override fun finalizeValueOnRead() {
    TODO("Not yet implemented")
  }

  override fun disallowChanges() {
    TODO("Not yet implemented")
  }

  override fun getOrElse(defaultValue: T): T {
    TODO("Not yet implemented")
  }

  override fun value(value: T?): Property<T> {
    TODO("Not yet implemented")
  }

  override fun value(provider: Provider<out T>): Property<T> {
    TODO("Not yet implemented")
  }

  override fun getOrNull(): T? {
    TODO("Not yet implemented")
  }

  override fun set(value: T?) {
    TODO("Not yet implemented")
  }

  override fun set(provider: Provider<out T>) {
    TODO("Not yet implemented")
  }

  override fun isPresent(): Boolean {
    TODO("Not yet implemented")
  }

  override fun convention(value: T?): Property<T> {
    TODO("Not yet implemented")
  }

  override fun convention(valueProvider: Provider<out T>): Property<T> {
    TODO("Not yet implemented")
  }

  override fun <S : Any?> map(transformer: Transformer<out S, in T>): Provider<S> {
    TODO("Not yet implemented")
  }

  override fun finalizeValue() {
    TODO("Not yet implemented")
  }

  override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in T>): Provider<S> {
    TODO("Not yet implemented")
  }

  override fun orElse(value: T): Provider<T> {
    TODO("Not yet implemented")
  }

  override fun orElse(provider: Provider<out T>): Provider<T> {
    TODO("Not yet implemented")
  }
}
