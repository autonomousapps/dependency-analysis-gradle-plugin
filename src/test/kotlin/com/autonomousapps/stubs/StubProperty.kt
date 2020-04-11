@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import org.gradle.api.Transformer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.naming.OperationNotSupportedException

class StubProperty<T>(
  private val thing: T
) : Property<T> {

  override fun get(): T {
    return thing
  }

  override fun finalizeValueOnRead() {
    throw OperationNotSupportedException("stub")
  }

  override fun disallowChanges() {
    throw OperationNotSupportedException("stub")
  }

  override fun getOrElse(defaultValue: T): T {
    throw OperationNotSupportedException("stub")
  }

  override fun value(value: T?): Property<T> {
    throw OperationNotSupportedException("stub")
  }

  override fun value(provider: Provider<out T>): Property<T> {
    throw OperationNotSupportedException("stub")
  }

  override fun getOrNull(): T? {
    throw OperationNotSupportedException("stub")
  }

  override fun set(value: T?) {
    throw OperationNotSupportedException("stub")
  }

  override fun set(provider: Provider<out T>) {
    throw OperationNotSupportedException("stub")
  }

  override fun isPresent(): Boolean {
    throw OperationNotSupportedException("stub")
  }

  override fun convention(value: T?): Property<T> {
    throw OperationNotSupportedException("stub")
  }

  override fun convention(valueProvider: Provider<out T>): Property<T> {
    throw OperationNotSupportedException("stub")
  }

  override fun <S : Any?> map(transformer: Transformer<out S, in T>): Provider<S> {
    throw OperationNotSupportedException("stub")
  }

  override fun finalizeValue() {
    throw OperationNotSupportedException("stub")
  }

  override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in T>): Provider<S> {
    throw OperationNotSupportedException("stub")
  }

  override fun orElse(value: T): Provider<T> {
    throw OperationNotSupportedException("stub")
  }

  override fun orElse(provider: Provider<out T>): Provider<T> {
    throw OperationNotSupportedException("stub")
  }
}
