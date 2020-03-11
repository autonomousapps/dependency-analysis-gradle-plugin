package com.autonomousapps.fixtures

fun androidProjectThatUsesConstants(agpVersion: String): AndroidProject {
  return AndroidProject(
      agpVersion = agpVersion,
      appSpec = AppSpec(
          sources = mapOf("MainActivity.kt" to """
                import androidx.appcompat.app.AppCompatActivity
                import $DEFAULT_PACKAGE_NAME.android.Producer
                import $DEFAULT_PACKAGE_NAME.android.BuildConfig.DEBUG
                import $DEFAULT_PACKAGE_NAME.android.*
                
                class MainActivity : AppCompatActivity() {
                    fun magic() {
                        if (DEBUG) {
                            println("Magic = " + Producer.MAGIC)
                            println(ONE)
                            println(TWO)
                            println(THREE)
                        }
                    }
                }
            """.trimIndent()),
          dependencies = listOf(
              "implementation" to KOTLIN_STDLIB_ID,
              "implementation" to APPCOMPAT
          )
      ),
      librarySpecs = listOf(
          LibrarySpec(
              name = "lib",
              type = LibraryType.KOTLIN_ANDROID_LIB,
              sources = mapOf("Producer.kt" to """
                    object Producer {
                        const val MAGIC = 42
                    }
                """.trimIndent()),
              dependencies = listOf(
                  "implementation" to KOTLIN_STDLIB_ID
              )
          ),
          LibrarySpec(
              name = "lib2",
              type = LibraryType.KOTLIN_ANDROID_LIB,
              sources = mapOf("BuildConfig.kt" to """
                    object BuildConfig {
                        const val DEBUG = true
                    }
                """.trimIndent()),
              dependencies = listOf(
                  "implementation" to KOTLIN_STDLIB_ID
              )
          ),
          LibrarySpec(
              name = "libstar",
              type = LibraryType.KOTLIN_ANDROID_LIB,
              sources = mapOf("star.kt" to """
                    const val ONE = 1
                    const val TWO = 2
                    const val THREE = 3
                """.trimIndent()),
              dependencies = listOf(
                  "implementation" to KOTLIN_STDLIB_ID
              )
          )
      )
  )
}
