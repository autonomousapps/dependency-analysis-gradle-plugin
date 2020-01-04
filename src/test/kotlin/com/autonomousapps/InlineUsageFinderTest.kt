package com.autonomousapps

import com.autonomousapps.internal.ComponentWithInlineMembers
import com.autonomousapps.internal.Dependency
import com.autonomousapps.stubs.Dependencies
import com.autonomousapps.stubs.StubFileCollection
import com.autonomousapps.tasks.InlineUsageFinder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertTrue

class InlineUsageFinderTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test fun `can find used inline members with specific import`() {
        // Given some source
        val f = tempFolder.newFile().apply {
            writeText(kotlinSourceWithSpecificImport)
        }
        val collection = StubFileCollection(f)

        // When we search that source for inline member usages
        val actual = InlineUsageFinder(collection, setOf(inlineImportsKotlinStdLibJdk7, inlineImportsProject)).find()

        // Then
        val expected = setOf(Dependencies.kotlinStdlibJdk7)
        assertTrue("Was $actual, expected $expected") {
            actual == expected
        }
    }

    @Test fun `can find used inline members with star specific`() {
        // Given some source
        val f = tempFolder.newFile().apply {
            writeText(kotlinSourceWithStarImport)
        }
        val collection = StubFileCollection(f)

        // When we search that source for inline member usages
        val actual = InlineUsageFinder(collection, setOf(inlineImportsKotlinStdLibJdk7, inlineImportsProject)).find()

        // Then
        val expected = setOf(Dependencies.kotlinStdlibJdk7)
        assertTrue("Was $actual, expected $expected") {
            actual == expected
        }
    }

    @Test fun `finds nothing when there's nothing to find`() {
        // Given some source
        val f = tempFolder.newFile().apply {
            writeText(kotlinSourceWithNoImports)
        }
        val collection = StubFileCollection(f)

        // When we search that source for inline member usages
        val actual = InlineUsageFinder(collection, setOf(inlineImportsKotlinStdLibJdk7, inlineImportsProject)).find()

        // Then
        val expected = emptySet<Dependency>()
        assertTrue("Was $actual, expected $expected") {
            actual == expected
        }
    }

    @Test fun `finds the right things even with confusing source`() {
        // Given some source
        val f = tempFolder.newFile().apply {
            writeText(kotlinSourceWithSpecificImport)
        }
        val g = tempFolder.newFile().apply {
            writeText(kotlinSourceWithStarImport)
        }
        val h = tempFolder.newFile().apply {
            writeText(kotlinSourceWithImportsAndConfusingComments)
        }
        val collection = StubFileCollection(f, g, h)

        // When we search that source for inline member usages
        val actual = InlineUsageFinder(collection, setOf(inlineImportsKotlinStdLibJdk7, inlineImportsProject)).find()

        // Then
        val expected = setOf(Dependency(":project"), Dependencies.kotlinStdlibJdk7)
        assertTrue("Was      $actual\nexpected $expected") {
            actual == expected
        }
    }
}

val kotlinSourceWithSpecificImport = """
    import kotlin.jdk7.use
    import java.io.File
    
    class SomeClass {
        fun hello() {
            File("magic").bufferedWriter().use { writer -> publicApi.dump(writer) }
        }
    }
""".trimIndent()

val kotlinSourceWithStarImport = """
    import kotlin.jdk7.*
    import java.io.File
    
    class SomeClass {
        fun hello() {
            File("magic").bufferedWriter().use { writer -> publicApi.dump(writer) }
        }
    }
""".trimIndent()

val kotlinSourceWithNoImports = """
    class SomeClass {
        fun hello() {
            println("hi!")
        }
    }
""".trimIndent()

val kotlinSourceWithImportsAndConfusingComments = """
    import kotlin.jdk7.use
    import com.project.*
    import java.io.File
    
    /* This multi-line comments has text designed to confuse the algorithm. What will happen?!
    import some stuff
    */
    class SomeClass {
        fun hello() {
            File("magic").bufferedWriter().use { writer -> publicApi.dump(writer) }
        }
    }
""".trimIndent()

val inlineImportsKotlinStdLibJdk7 = ComponentWithInlineMembers(
    dependency = Dependencies.kotlinStdlibJdk7,
    imports = setOf(
        "kotlin.jdk7.*",
        "kotlin.jdk7.use"
    )
)

val inlineImportsProject = ComponentWithInlineMembers(
    dependency = Dependency(":project"),
    imports = setOf(
        "com.project.*",
        "com.project.magic"
    )
)
