// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Sources")

package com.autonomousapps.fixtures

val DEFAULT_APP_SOURCES = mapOf("MainActivity.kt" to """
    package $DEFAULT_PACKAGE_NAME
                
    import androidx.appcompat.app.AppCompatActivity
                
    class MainActivity : AppCompatActivity() {
    }
""".trimIndent())

val DEFAULT_SOURCE_KOTLIN_ANDROID = mapOf("Library.kt" to """ 
    import androidx.core.provider.FontRequest
             
    class Library {
        fun magic() = 42

        fun font() = FontRequest("foo", "foo", "foo", 0) 
    }
""".trimIndent())

val DEFAULT_SOURCE_KOTLIN_JVM = mapOf("Library.kt" to """  
    class Library {
        fun magic() = 42 
    }
""".trimIndent())

val DEFAULT_SOURCE_JAVA = mapOf("Library.java" to """  
    class Library {
        public int magic() {
            return 42;
        }
    }
""".trimIndent())
