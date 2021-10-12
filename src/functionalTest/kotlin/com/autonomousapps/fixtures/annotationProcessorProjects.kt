package com.autonomousapps.fixtures

import com.autonomousapps.advice.*

class DaggerProjectUsedByAnnotationProcessorForMethod(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("Thing.java" to """
    package $DEFAULT_PACKAGE_NAME;

    import javax.inject.Inject;
    
    public class Thing {
      @Inject Thing() {}
    }
  """.trimIndent())

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
    Advice.ofRemove(daggerAndroidComponent),
    Advice.ofAdd(transitiveInject, toConfiguration = "implementation")
  )
}

class DaggerProjectUsedByAnnotationProcessorForClass(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("MyModule.java" to """
    package $DEFAULT_PACKAGE_NAME;

    import dagger.Module;
    import dagger.Provides;
    
    @Module public abstract class MyModule {
      @Provides String provideString() {
        return "magic";
      }
    }
  """.trimIndent())

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

  private val sources = mapOf("Thing.java" to """
    package $DEFAULT_PACKAGE_NAME;
    
    public class Thing {
    }
  """.trimIndent())

  val appSpec = AppSpec(
    type = AppType.JAVA_ANDROID_APP,
    sources = sources,
    dependencies = listOf(
      "implementation" to APPCOMPAT,
      "annotationProcessor" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofRemove(Dependency("com.google.dagger:dagger-compiler", "2.24", "annotationProcessor"))
  )
}

class DaggerProjectUsedByKaptForMethod(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("Thing.kt" to """
    package $DEFAULT_PACKAGE_NAME

    import javax.inject.Inject
    
    class Thing {
      @Inject lateinit var string: String
    }
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT,
      "implementation" to "com.google.dagger:dagger:2.24",
      "kapt" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofAdd(transitiveInject2, toConfiguration = "implementation")
  )
}

class DaggerProjectUsedByKaptForClass(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("MyModule.kt" to """
    package $DEFAULT_PACKAGE_NAME

    import dagger.Module
    import dagger.Provides
    
    @Module object MyModule {
      @Provides @JvmStatic fun provideString(): String {
        return "magic"  
      }
    }
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT,
      "implementation" to "com.google.dagger:dagger:2.24",
      "kapt" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = emptySet<Advice>()
}

class DaggerProjectUnusedByKapt(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("Thing.kt" to """
    package $DEFAULT_PACKAGE_NAME;
    
    class Thing
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT,
      "kapt" to "com.google.dagger:dagger-compiler:2.24"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofRemove(Dependency("com.google.dagger:dagger-compiler", "2.24", "kapt"))
  )
}

class AutoValueProjectUsedByKapt(agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = rootSpec,
    appSpec = appSpec
  )

  val rootSpec = RootSpec(agpVersion = agpVersion)

  private val sources = mapOf("Animal.kt" to """
    package $DEFAULT_PACKAGE_NAME

    import com.google.auto.value.AutoValue
    
    @AutoValue
    abstract class Animal {
      companion object {
        @JvmStatic fun create(name: String, numberOfLegs: Int): Animal = AutoValue_Animal(name, numberOfLegs)      
      }
      abstract fun name(): String
      abstract fun numberOfLegs(): Int
    }
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT,
      "compileOnly" to "com.google.auto.value:auto-value-annotations:1.7",
      "kapt" to "com.google.auto.value:auto-value:1.7"
    )
  )

  val expectedAdviceForApp = emptySet<Advice>()
  val expectedAdviceForRoot = emptySet<PluginAdvice>()
}

class KaptIsRedundantProject(agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = rootSpec,
    appSpec = appSpec
  )

  val rootSpec = RootSpec(agpVersion = agpVersion)

  private val sources = mapOf("Thing.kt" to """
    package $DEFAULT_PACKAGE_NAME

    class Thing {
      fun magic() = 42
    }
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT
    )
  )

  val expectedAdvice = setOf(PluginAdvice.redundantKapt())
}

class KaptIsRedundantWithUnusedProcsProject(agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = rootSpec,
    appSpec = appSpec
  )

  val rootSpec = RootSpec(agpVersion = agpVersion)

  private val sources = mapOf("Thing.kt" to """
    package $DEFAULT_PACKAGE_NAME

    class Thing {
      fun magic() = 42
    }
  """.trimIndent())

  val appSpec = AppSpec(
    plugins = setOf("kotlin-kapt"),
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.5.21",
      "implementation" to APPCOMPAT,
      "kapt" to "com.google.auto.value:auto-value:1.7"
    )
  )

  val expectedAdvice = setOf(PluginAdvice.redundantKapt())
}

private val transitiveDagger = TransitiveDependency(
  Dependency("com.google.dagger:dagger", "2.24"),
  setOf(Dependency("com.google.dagger:dagger-android", "2.24"))
)
private val transitiveInject = TransitiveDependency(
  Dependency("javax.inject:javax.inject", "1"),
  setOf(Dependency("com.google.dagger:dagger-android", "2.24"))
)
private val transitiveInject2 = TransitiveDependency(
  Dependency("javax.inject:javax.inject", "1"),
  setOf(Dependency("com.google.dagger:dagger", "2.24"))
)
private val daggerAndroidComponent = ComponentWithTransitives(
  Dependency("com.google.dagger:dagger-android", "2.24", "implementation"),
  mutableSetOf(Dependency("com.google.dagger:dagger", "2.24"), Dependency("javax.inject:javax.inject", "1"))
)
