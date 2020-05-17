package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.TransitiveDependency

class TransitiveAdviceAndroidProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("MyModule.java" to """
    package $DEFAULT_PACKAGE_NAME;
    
    import dagger.Module;
    import dagger.Provides;
    
    @Module
    public abstract class MyModule {
      @Provides public static String provideString() {
        return "magic";
      }
    }
  """.trimIndent())

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to APPCOMPAT,
      "implementation" to "com.google.dagger:dagger-android:2.24",
      "implementation" to "com.google.dagger:dagger-android-support:2.24"
    )
  )

  // Dependencies
  private val daggerCoreDep = Dependency("com.google.dagger:dagger", "2.24")
  private val daggerAndroidDep = Dependency("com.google.dagger:dagger-android", "2.24")
  private val daggerAndroidSupportDep = Dependency("com.google.dagger:dagger-android-support", "2.24")

  // Components (with transitives)
  private val daggerAndroidComponent = ComponentWithTransitives(
    dependency = daggerAndroidDep.copy(configurationName = "implementation"),
    usedTransitiveDependencies = mutableSetOf(daggerCoreDep)
  )
  private val daggerAndroidSupportComponent = ComponentWithTransitives(
    dependency = daggerAndroidSupportDep.copy(configurationName = "implementation"),
    usedTransitiveDependencies = mutableSetOf(daggerCoreDep)
  )

  // Advices
  private val addDaggerCore = Advice.ofAdd(
    transitiveDependency = TransitiveDependency(
      dependency = daggerCoreDep,
      parents = setOf(daggerAndroidDep, daggerAndroidSupportDep)
    ),
    toConfiguration = "implementation"
  )
  private val removeDaggerAndroid = Advice.ofRemove(component = daggerAndroidComponent)
  private val removeDaggerAndroidSupport = Advice.ofRemove(component = daggerAndroidSupportComponent)

  val expectedAdviceForApp = setOf(addDaggerCore, removeDaggerAndroid, removeDaggerAndroidSupport)
}
