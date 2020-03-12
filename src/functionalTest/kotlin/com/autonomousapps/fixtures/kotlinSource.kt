@file:JvmName("KotlinSources")

package com.autonomousapps.fixtures

val CORE_KTX_LIB = mapOf(
    "CoreKtxLibrary.kt" to """
        import android.content.Context
        import android.text.SpannableStringBuilder 
        import androidx.core.content.ContextCompat
        import androidx.core.text.bold
        import androidx.core.text.color
        import $DEFAULT_PACKAGE_NAME.lib.R

        class CoreKtxLibrary {
            fun useCoreKtx(context: Context): CharSequence {
                return SpannableStringBuilder("just some text")
                    .bold {
                        color(ContextCompat.getColor(context, R.color.colorAccent)) { append("some more text") }
                    }
                }
            }
        """.trimIndent()
)