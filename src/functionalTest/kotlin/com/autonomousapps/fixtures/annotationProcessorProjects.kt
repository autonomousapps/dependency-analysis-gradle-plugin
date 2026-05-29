// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates

class DaggerProjectUsedByAnnotationProcessorForMethod(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "Thing.java" to """
    package $DEFAULT_PACKAGE_NAME;

    import javax.inject.Inject;
    
    public class Thing {
      @Inject Thing() {}
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    type = AppType.JAVA_ANDROID_APP,
    sources = sources,
    dependencies = listOf(
      "implementation" to APPCOMPAT,
      "implementation" to "com.google.dagger:dagger-android:2.24",
      "annotationProcessor" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofAdd(transitiveDagger, toConfiguration = "implementation"),
    Advice.ofRemove(daggerAndroidComponent, fromConfiguration = "implementation"),
    Advice.ofAdd(transitiveInject, toConfiguration = "implementation")
  )
}

class DaggerProjectUsedByAnnotationProcessorForClass(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "MyModule.java" to """
    package $DEFAULT_PACKAGE_NAME;

    import dagger.Module;
    import dagger.Provides;
    
    @Module public abstract class MyModule {
      @Provides String provideString() {
        return "magic";
      }
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    type = AppType.JAVA_ANDROID_APP,
    sources = sources,
    dependencies = listOf(
      "implementation" to APPCOMPAT,
      "implementation" to "com.google.dagger:dagger:2.24",
      "annotationProcessor" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = emptySet<Advice>()
}

class DaggerProjectUnusedByAnnotationProcessor(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "Thing.java" to """
    package $DEFAULT_PACKAGE_NAME;
    
    public class Thing {
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    type = AppType.JAVA_ANDROID_APP,
    sources = sources,
    dependencies = listOf(
      "implementation" to APPCOMPAT,
      "annotationProcessor" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofRemove(
      ModuleCoordinates(
        "com.google.dagger:dagger-compiler", "2.24", GradleVariantIdentification.EMPTY
      ),
      fromConfiguration = "annotationProcessor"
    )
  )
}

private val transitiveDagger = ModuleCoordinates("com.google.dagger:dagger", "2.24", GradleVariantIdentification.EMPTY)
private val transitiveInject = ModuleCoordinates("javax.inject:javax.inject", "1", GradleVariantIdentification.EMPTY)
private val daggerAndroidComponent =
  ModuleCoordinates("com.google.dagger:dagger-android", "2.24", GradleVariantIdentification.EMPTY)
