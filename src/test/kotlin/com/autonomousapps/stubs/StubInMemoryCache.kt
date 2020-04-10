@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import com.autonomousapps.services.InMemoryCache
import org.gradle.api.services.BuildServiceParameters

class StubInMemoryCache : InMemoryCache() {
  override fun getParameters(): BuildServiceParameters.None {
    error("There are no parameters")
  }

}
