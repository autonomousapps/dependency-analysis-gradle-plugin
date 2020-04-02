@file:JvmName("NeedsAdviceProject")

package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.Dependency

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

fun expectedAppAdvice(ignore: Set<String> = emptySet()) = mutableSetOf(
  Advice.add(Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"), "implementation"),
  Advice.add(Dependency(APPCOMPAT_ID), "implementation"),
  Advice.change(Dependency(ANDROIDX_ANNOTATIONS_ID, configurationName = "api"), "implementation"),
  Advice.remove(Dependency(":lib_android", configurationName = "implementation")),
  Advice.remove(Dependency(CORE_KTX_ID, configurationName = "implementation")),
  Advice.remove(Dependency(COMMONS_IO_ID, configurationName = "implementation")),
  Advice.remove(Dependency(KOTLIN_STDLIB_JDK7_ID, configurationName = "implementation"))
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()

fun expectedLibAndroidAdvice(ignore: Set<String> = emptySet()) = mutableSetOf(
  Advice.add(Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"), "implementation"),
  Advice.add(Dependency(CORE_ID), "api"),
  Advice.change(Dependency(APPCOMPAT_ID, configurationName = "api"), "implementation"),
  Advice.remove(Dependency(CORE_KTX_ID, configurationName = "implementation")),
  Advice.remove(Dependency(NAV_UI_KTX_ID)),
  Advice.remove(Dependency(KOTLIN_STDLIB_JDK7_ID, configurationName = "implementation"))
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()

fun expectedLibJvmAdvice(ignore: Set<String> = emptySet()) = mutableSetOf(
  Advice.add(Dependency(KOTLIN_STDLIB_ID, configurationName = "implementation"), "implementation"),
  Advice.add(Dependency(COMMONS_LANG3_ID), "implementation"),
  Advice.change(Dependency(COMMONS_COLLECTIONS_ID, configurationName = "api"), "implementation"),
  Advice.change(Dependency(COMMONS_IO_ID, configurationName = "implementation"), "api"),
  Advice.remove(Dependency(COMMONS_TEXT_ID, configurationName = "implementation")),
  Advice.remove(Dependency(KOTLIN_STDLIB_JDK7_ID, configurationName = "implementation"))
).filterNot {
  ignore.contains(it.dependency.identifier)
}.toSortedSet()
