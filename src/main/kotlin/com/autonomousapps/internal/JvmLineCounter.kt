package com.autonomousapps.internal

import java.io.File

internal object JvmLineCounter {

    private val multiline = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
    private val singleline = Regex("""^\s*//.*$""", RegexOption.MULTILINE)

    fun countLines(file: File): Int {
        var text = file.readText()

        // Strip out all single-line comments
        text = singleline.replace(text, "")

        // String out all multi-line comments
        text = multiline.replace(text) { matchResult ->
            if (matchResult.value.contains("\n")) {
                "\n"
            } else {
                ""
            }
        }

        val lines = text.split("\n").filter { it.isNotBlank() }

//        println("File ${file.name}")
//        lines.forEachIndexed { i, s ->
//            println("${i + 1}: $s")
//        }

        return lines.size
    }
}