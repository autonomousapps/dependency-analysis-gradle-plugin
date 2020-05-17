@file:JvmName("NeedsAdviceProject")

package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency

/**
 * Will have three modules:
 * 1. app
 * 2. lib-android
 * 3. lib-jvm
 *
 * Scenario:
 * app:
 *   - add used transitive from lib-android
 *   - remove lib-android, as it is not used directly
 *   - change a library from api to implementation
 *   - remove direct library dependency that is not used and which is incorrectly "implementation" on lib-jvm
 *     (should be "api")
 *   - No dependencies should be on "api"
 * lib-android
 *   - change a library from api to implementation
 *   - change a library from implementation to api
 *   - remove an unused library
 *   - add a used transitive library
 * lib-jvm
 *   - change a library form implementation to api
 *   - change a library from api to implementation
 *   - remove an unused library
 *   - add a used transitive library
 */
fun androidProjectThatNeedsAdvice(
  agpVersion: String,
  extensionSpec: String = ""
): AndroidProject {
  return AndroidProject(
    rootSpec = RootSpec(extensionSpec = extensionSpec, agpVersion = agpVersion, librarySpecs = librarySpecs),
    appSpec = AppSpec(
      sources = mapOf("MainActivity.kt" to """ 
        import androidx.annotation.AnyThread
        import androidx.appcompat.app.AppCompatActivity
        import $DEFAULT_PACKAGE_NAME.kotlin.JvmLibrary
                        
        // AppCompatActivity from APPCOMPAT is not declared, but brought in transitively from lib-android
        class MainActivity : AppCompatActivity() {
          
          // From ANDROIDX_ANNOTATIONS, which is incorrectly declared as "api"
          @AnyThread
          fun thing() {
            JvmLibrary().thing()
          }
        }""".trimIndent()),
      dependencies = DEPENDENCIES_KOTLIN_STDLIB + listOf(
        //"implementation" to APPCOMPAT
        // should be "implementation"
        "api" to ANDROIDX_ANNOTATIONS,
        // should be removed. Needed to compile against lib-android, which doesn't declare it correctly
        "implementation" to CORE_KTX,
        // Required to compile lib-jvm, as this is part of its ABI but not declared as such
        "implementation" to COMMONS_IO
      )
    ),
    librarySpecs = librarySpecs
  )
}

private val librarySpecs = listOf(
  LibrarySpec(
    name = "lib_android",
    type = LibraryType.KOTLIN_ANDROID_LIB,
    sources = mapOf("AndroidLibrary.kt" to """ 
      import androidx.annotation.AnyThread
      import androidx.appcompat.app.AppCompatActivity
      import androidx.core.provider.FontRequest
      
      class AndroidLibrary {
        // FontRequest from CORE_KTX is part of the ABI
        fun abi() = FontRequest("foo", "foo", "foo", 0)
        
        // @AnyThread is from ANDROIDX_ANNOTATIONS, brought in transitively from APPCOMPAT
        @AnyThread
        fun implementation() {
          // AppCompatActivity from APPCOMPAT is an implementation dependency
          val klass = AppCompatActivity::class.java
        }
      }""".trimIndent()),
    dependencies = DEPENDENCIES_KOTLIN_STDLIB + listOf(
      "api" to APPCOMPAT, // should be "implementation"
      "implementation" to CORE_KTX, // should be "api"
      "implementation" to NAV_UI_KTX // should be removed
    )
  ),
  LibrarySpec(
    name = "lib_jvm",
    type = LibraryType.KOTLIN_JVM_LIB,
    sources = mapOf("JvmLibrary.kt" to """
      import org.apache.commons.collections4.bag.HashBag // Direct from commons-collections
      import org.apache.commons.lang3.StringUtils // Brought in transitively from commons-text
      import org.apache.commons.io.output.NullWriter // Direct from commons-io
      
      class JvmLibrary {
        fun thing() {
          // From commons-lang
          val empty = StringUtils.isEmpty("")
          // From commons-collections
          val bag = HashBag<String>()
        }
        
        // NullWriter is part of ABI, but if method is never called, not needed by consumer
        fun nullWriter(): NullWriter {
          return NullWriter()
        }
      }""".trimIndent()),
    dependencies = DEPENDENCIES_KOTLIN_STDLIB + listOf(
      // Transitively brings in org.apache.commons:commons-lang
      // Should be removed, and commons-lang3 added directly
      "implementation" to COMMONS_TEXT,
      // Should be "implementation"
      "api" to COMMONS_COLLECTIONS,
      // Should be "api"
      "implementation" to COMMONS_IO
    )
  )
)

private val transitiveStdLib = TransitiveDependency(
  dependency = Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"),
  parents = setOf(
    Dependency(KOTLIN_STDLIB_JDK7_ID),
    Dependency(":lib_android", configurationName = "implementation"),
    Dependency(":lib_jvm", configurationName = "implementation"),
    Dependency(CORE_KTX_ID, configurationName = "implementation")
  )
)
private val transitiveStdLib2 = TransitiveDependency(
  dependency = Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"),
  parents = setOf(
    Dependency(CORE_KTX_ID, configurationName = "implementation"),
    Dependency(KOTLIN_STDLIB_JDK7_ID),
    Dependency(NAV_UI_KTX_ID)
  )
)
private val transitiveAppCompat = TransitiveDependency(
  dependency = Dependency(APPCOMPAT_ID),
  parents = setOf(Dependency(":lib_android", configurationName = "implementation"))
)
private val stdLib7Component = ComponentWithTransitives(
  Dependency(KOTLIN_STDLIB_JDK7_ID, configurationName = "implementation"),
  mutableSetOf(Dependency("org.jetbrains.kotlin:kotlin-stdlib"))
)
private val stdLib7Component2 = ComponentWithTransitives(
  Dependency(KOTLIN_STDLIB_JDK7_ID, configurationName = "implementation"),
  mutableSetOf(
    Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
    Dependency("org.jetbrains:annotations")
  )
)
private val coreKtxComponent = ComponentWithTransitives(
  dependency = Dependency(CORE_KTX_ID, configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf(
    Dependency("org.jetbrains.kotlin:kotlin-stdlib")
  )
)
private val coreKtxComponent2 = ComponentWithTransitives(
  dependency = Dependency(CORE_KTX_ID, configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf(
    Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
    Dependency("org.jetbrains:annotations"),
    Dependency("androidx.annotation:annotation"),
    Dependency("androidx.core:core")
  )
)
private val commonsIoComponent = ComponentWithTransitives(
  dependency = Dependency(COMMONS_IO_ID, configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf()
)
private val navComponent = ComponentWithTransitives(
  dependency = Dependency(NAV_UI_KTX_ID, configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf(
    Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
    Dependency("org.jetbrains:annotations"),
    Dependency("androidx.annotation:annotation"),
    Dependency("androidx.core:core")
  )
)

private val libAndroidComponent = ComponentWithTransitives(
  Dependency(":lib_android", configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf(
    Dependency("androidx.appcompat:appcompat"), Dependency("org.jetbrains.kotlin:kotlin-stdlib")
  )
)

fun expectedAppAdvice(ignore: Set<String> = emptySet()): Set<Advice> = mutableSetOf(
  Advice.ofAdd(transitiveStdLib, toConfiguration = "implementation"),
  Advice.ofAdd(transitiveAppCompat, toConfiguration = "implementation"),
  Advice.ofChange(Dependency(ANDROIDX_ANNOTATIONS_ID, configurationName = "api"), "compileOnly"),
  Advice.ofRemove(libAndroidComponent),
  Advice.ofRemove(coreKtxComponent),
  Advice.ofRemove(commonsIoComponent),
  Advice.ofRemove(stdLib7Component)
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()

private val transitiveCore = TransitiveDependency(
  Dependency(CORE_ID),
  setOf(
    Dependency("androidx.core:core-ktx"),
    Dependency("androidx.navigation:navigation-ui-ktx"),
    Dependency("androidx.appcompat:appcompat")
  )
)

fun expectedLibAndroidAdvice(ignore: Set<String> = emptySet()) = mutableSetOf(
  Advice.ofAdd(transitiveStdLib2, "implementation"),
  Advice.ofAdd(transitiveCore, "api"),
  Advice.ofChange(Dependency(APPCOMPAT_ID, configurationName = "api"), "implementation"),
  Advice.ofRemove(coreKtxComponent2),
  Advice.ofRemove(navComponent),
  Advice.ofRemove(stdLib7Component2)
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()

private val transitiveCommonsLang = TransitiveDependency(
  dependency = Dependency(COMMONS_LANG3_ID),
  parents = setOf(Dependency("org.apache.commons:commons-text"))
)
private val transitiveStdLib3 = TransitiveDependency(
  dependency = Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"),
  parents = setOf(Dependency(KOTLIN_STDLIB_JDK7_ID))
)
private val commonsTextComponent = ComponentWithTransitives(
  dependency = Dependency(COMMONS_TEXT_ID, configurationName = "implementation"),
  usedTransitiveDependencies = mutableSetOf(Dependency("org.apache.commons:commons-lang3"))
)

fun expectedLibJvmAdvice(ignore: Set<String> = emptySet()) = mutableSetOf(
  Advice.ofAdd(transitiveStdLib3, "implementation"),
  Advice.ofAdd(transitiveCommonsLang, "implementation"),
  Advice.ofChange(Dependency(COMMONS_COLLECTIONS_ID, configurationName = "api"), "implementation"),
  Advice.ofChange(Dependency(COMMONS_IO_ID, configurationName = "implementation"), "api"),
  Advice.ofRemove(commonsTextComponent),
  Advice.ofRemove(stdLib7Component2)
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()
