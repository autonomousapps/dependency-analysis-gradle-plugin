package com.autonomousapps.fixtures

import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates

class TransitiveAdviceAndroidProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "MyModule.java" to """
    package $DEFAULT_PACKAGE_NAME;
    
    import dagger.Module;
    import dagger.Provides;
    
    @Module
    public abstract class MyModule {
      @Provides public static String provideString() {
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
      "implementation" to "com.google.dagger:dagger-android:2.24",
      "implementation" to "com.google.dagger:dagger-android-support:2.24"
    )
  )

  // Advice
  private val addDaggerCore = Advice.ofAdd(
    ModuleCoordinates("com.google.dagger:dagger", "2.24"), "implementation"
  )
  private val addAppCompatV7 = Advice.ofAdd(
    ModuleCoordinates("com.android.support:appcompat-v7", "25.0.0"), "implementation"
  )
  private val removeDaggerAndroid = Advice.ofRemove(
    ModuleCoordinates("com.google.dagger:dagger-android", "2.24"), "implementation"
  )
  private val removeDaggerAndroidSupport = Advice.ofRemove(
    ModuleCoordinates("com.google.dagger:dagger-android-support", "2.24"), "implementation"
  )
  val expectedAdviceForApp = setOf(addDaggerCore, addAppCompatV7, removeDaggerAndroid, removeDaggerAndroidSupport)
}
