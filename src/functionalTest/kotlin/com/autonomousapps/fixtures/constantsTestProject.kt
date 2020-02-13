package com.autonomousapps.fixtures

fun androidProjectThatUsesConstants(agpVersion: String): AndroidProject {
    return AndroidProject(
        agpVersion = agpVersion,
        appSpec = AppSpec(
            sources = mapOf("MainActivity.kt" to """
                import androidx.appcompat.app.AppCompatActivity
                import $DEFAULT_PACKAGE_NAME.android.Producer
                
                class MainActivity : AppCompatActivity() {
                    fun magic() {
                        println("Magic = " + Producer.MAGIC)
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
                type = LibraryType.KOTLIN_ANDROID,
                sources = mapOf("Producer.kt" to """
                    object Producer {
                        const val MAGIC = 42
                    }
                """.trimIndent()),
                dependencies = listOf(
                    "implementation" to KOTLIN_STDLIB_ID
                )
            )
        )
    )
}
