package com.autonomousapps.transform

import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.intermediates.Bucket
import com.autonomousapps.model.intermediates.Location
import com.autonomousapps.model.intermediates.Usage
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class StandardTransformTest {

  @Nested inner class SingleVariant {

    @Test fun `no advice for correct declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.API
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.IMPL.value
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = oldConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `should be implementation`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.API.value
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = oldConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `no advice for correct variant declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = "debugImplementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.NONE
      val usages = usage(bucket, "debug").intoSet()
      val fromConfiguration = "api"
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofRemove(coordinates, fromConfiguration))
    }

    @Test fun `should add dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "implementation"))
    }
  }

  @Nested inner class MultiVariant {

    @Test fun `no advice for correct declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared compileOnly usage`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.COMPILE_ONLY
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared runtimeOnly usage`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.RUNTIME_ONLY
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.API, "release"))
      val fromConfiguration = "implementation"
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates, fromConfiguration, "api"
        )
      )
    }

    @Test fun `should be api on release variant`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates, "implementation", "debugImplementation"
        ),
        Advice.ofChange(
          coordinates, "implementation", "releaseApi"
        )
      )
    }

    @Test fun `should not remove unused and undeclared dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val fromConfiguration = "api"
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofRemove(coordinates, fromConfiguration))
    }

    @Test fun `should remove unused dependency on release variant`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.NONE, "release"))
      val fromConfiguration = "implementation"
      val locations = Location(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      // change from impl -> debugImpl (implicit "remove from release variant")
      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, fromConfiguration, "debugImplementation")
      )
    }

    @Test fun `should add dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "implementation"))
    }

    @Test fun `should add dependency to debug as impl and release as api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(coordinates, "debugImplementation"),
        Advice.ofAdd(coordinates, "releaseApi")
      )
    }

    @Test fun `should add dependency to debug as impl and not at all for release`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.NONE, "release"))
      val locations = emptySet<Location>()

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "debugImplementation"))
    }
  }

  @Nested inner class Flavors {
    // TODO
  }

  @Nested inner class MultiLocation {

    @Test fun `should consolidate on implementation declaration`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val locations = setOf(
        Location(id, "debugImplementation"),
        Location(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "implementation"),
        Advice.ofChange(coordinates, "releaseApi", "implementation")
      )
    }

    @Test fun `should consolidate on implementation declaration, with pathological redundant declaration`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val locations = setOf(
        Location(id, "implementation"),
        Location(id, "releaseImplementation")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "releaseImplementation")
      )
    }

    @Test fun `should remove release declaration and change debug to api`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.NONE, "release"))
      val locations = setOf(
        Location(id, "debugImplementation"),
        Location(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "debugApi"),
        Advice.ofRemove(coordinates, "releaseApi")
      )
    }

    @Test fun `should remove both declarations`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val locations = setOf(
        Location(id, "debugImplementation"),
        Location(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "debugImplementation"),
        Advice.ofRemove(coordinates, "releaseApi")
      )
    }

    @Test fun `should change both declarations`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.IMPL, "release"))
      val locations = setOf(
        Location(id, "debugImplementation"),
        Location(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "debugApi"),
        Advice.ofChange(coordinates, "releaseApi", "releaseImplementation")
      )
    }

    @Test fun `should change debug to debugImpl and release to releaseApi`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val locations = setOf(
        Location(id, "implementation"),
        Location(id, "releaseImplementation")
      )

      val actual = StandardTransform(coordinates, locations).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation"),
        Advice.ofChange(coordinates, "releaseImplementation", "releaseApi")
      )
    }
  }

  private fun usage(
    bucket: Bucket,
    variant: String = "debug",
    buildType: String? = null,
    flavor: String? = null
  ) = Usage(
    buildType = buildType,
    flavor = flavor,
    variant = variant,
    bucket = bucket,
    reasons = emptySet()
  )
}
