package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonSet

class CompileOnlyTestProject(
  private val agpVersion: String
) {
  val appSpec = AppSpec(
    sources = mapOf("MainActivity.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      import androidx.appcompat.app.AppCompatActivity
      import androidx.annotation.ColorRes
      import $DEFAULT_PACKAGE_NAME.R
      import $DEFAULT_PACKAGE_NAME.android.KotlinLibrary
      import $DEFAULT_PACKAGE_NAME.java.JavaLibrary
      
      class MainActivity : AppCompatActivity() {
        @ColorRes
        val colorRes: Int = R.color.colorPrimaryDark
        
        fun evenMoreNothing() {
          KotlinLibrary().doNothing()
          JavaLibrary().createMagic()
        }
      }""".trimIndent()),
    dependencies = listOf(
      "implementation" to KOTLIN_STDLIB_ID,
      "implementation" to APPCOMPAT,
      "implementation" to ANDROIDX_ANNOTATIONS // could be compileOnly
    )
  )

  val androidKotlinLib = LibrarySpec(
    name = "lib",
    type = LibraryType.KOTLIN_ANDROID_LIB,
    sources = mapOf("KotlinLibrary.kt" to """ 
      import com.google.auto.value.AutoValue
      import org.jetbrains.annotations.NotNull
      
      @AutoValue
      class KotlinLibrary {
        @NotNull
        fun doNothing() {
        }
      }""".trimIndent()),
    dependencies = listOf(
      "implementation" to "com.google.auto.value:auto-value-annotations:1.6",
      "implementation" to KOTLIN_STDLIB_ID // provides `org.jetbrains:annotations`, a compileOnly candidate, transitively
    )
  )
  val javaJvmLib = LibrarySpec(
    name = "lib1",
    type = LibraryType.JAVA_JVM_LIB,
    sources = mapOf("JavaLibrary.java" to """ 
      import com.google.auto.value.AutoValue;
      
      @AutoValue
      public class JavaLibrary {
        public String createMagic() {
          return "magic";
        }
      }""".trimIndent()),
    dependencies = listOf(
      "implementation" to "com.google.auto.value:auto-value-annotations:1.6" // could be compileOnly
    )
  )

  private val librarySpecs = listOf(androidKotlinLib, javaJvmLib)

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion, librarySpecs = librarySpecs),
    appSpec = appSpec,
    librarySpecs = librarySpecs
  )

  val expectedAdviceForApp =
    """[{"dependency":{"identifier":"androidx.annotation:annotation","resolvedVersion":"1.1.0"},"fromConfiguration":"implementation","toConfiguration":"compileOnly"}]"""
      .fromJsonSet<Advice>()

  val expectedAdviceForAndroidKotlinLib =
    """[{"dependency":{"identifier":"com.google.auto.value:auto-value-annotations","resolvedVersion":"1.6"},"fromConfiguration":"implementation","toConfiguration":"compileOnly"}]"""
      .fromJsonSet<Advice>()

  val expectedAdviceForJavaJvmLib =
    """[{"dependency":{"identifier":"com.google.auto.value:auto-value-annotations","resolvedVersion":"1.6"},"fromConfiguration":"implementation","toConfiguration":"compileOnly"}]"""
      .fromJsonSet<Advice>()
}
