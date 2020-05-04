package com.autonomousapps.advice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ComponentWithTransitivesTest {

  private val externalComponent = Dependency("org.something:artifact", configurationName = "implementation")
  private val transitiveDependency1 = Dependency("org.something:core")
  private val transitiveDependency2 = Dependency("org.something-else:other")

  private val externalComponent2 = Dependency("com.something:artifact", configurationName = "implementation")

  private val projectComponent = Dependency(":core", configurationName = "implementation")

  private val facadeComponentWithTransitives = ComponentWithTransitives(
    dependency = externalComponent,
    usedTransitiveDependencies = mutableSetOf(transitiveDependency1, transitiveDependency2)
  )
  private val nonFacadeComponentWithTransitives = ComponentWithTransitives(
    dependency = externalComponent2,
    usedTransitiveDependencies = mutableSetOf(transitiveDependency1, transitiveDependency2)
  )
  private val projectComponentWithTransitives = ComponentWithTransitives(
    dependency = projectComponent,
    usedTransitiveDependencies = mutableSetOf(Dependency(":transitive"))
  )

  @Test fun isFacade() {
    assertThat(facadeComponentWithTransitives.isFacade).isTrue()
    assertThat(nonFacadeComponentWithTransitives.isFacade).isFalse()
  }

  @Test fun projectIsNotFacade() {
    assertThat(projectComponentWithTransitives.isFacade).isFalse()
  }
}
