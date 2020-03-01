@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.*
import com.autonomousapps.internal.antlr.v4.runtime.CharStreams
import com.autonomousapps.internal.antlr.v4.runtime.CommonTokenStream
import com.autonomousapps.internal.antlr.v4.runtime.tree.ParseTreeWalker
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.grammar.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class ConstantUsageDetectionTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = TASK_GROUP_DEP
        description = "Produces a report of constants, from other components, that have been used"
    }

    /**
     * Upstream artifacts.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val artifacts: RegularFileProperty

    /**
     * The Java source of the current project.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val javaSourceFiles: ConfigurableFileCollection

    /**
     * The Kotlin source of the current project.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val kotlinSourceFiles: ConfigurableFileCollection

    /**
     * A [`Set<Dependency>`][Dependency] of dependencies that provide constants that the current project is using.
     */
    @get:OutputFile
    abstract val constantUsageReport: RegularFileProperty

    @TaskAction
    fun action() {
        workerExecutor.noIsolation().submit(ConstantUsageDetectionWorkAction::class.java) {
            artifacts.set(this@ConstantUsageDetectionTask.artifacts)
            javaSourceFiles.setFrom(this@ConstantUsageDetectionTask.javaSourceFiles)
            kotlinSourceFiles.setFrom(this@ConstantUsageDetectionTask.kotlinSourceFiles)
            constantUsageReport.set(this@ConstantUsageDetectionTask.constantUsageReport)
        }
    }
}

interface ConstantUsageDetectionParameters : WorkParameters {
    val artifacts: RegularFileProperty
    val javaSourceFiles: ConfigurableFileCollection
    val kotlinSourceFiles: ConfigurableFileCollection
    val constantUsageReport: RegularFileProperty
}

abstract class ConstantUsageDetectionWorkAction : WorkAction<ConstantUsageDetectionParameters> {

    private val logger = getLogger<ConstantUsageDetectionTask>()

    override fun execute() {
        // Output
        val constantUsageReportFile = parameters.constantUsageReport.get().asFile
        constantUsageReportFile.delete()

        // Inputs
        val artifacts = parameters.artifacts.get().asFile.readText().fromJsonList<Artifact>()

        val usedComponents = JavaOrKotlinConstantDetector(
            logger, artifacts, parameters.javaSourceFiles, parameters.kotlinSourceFiles
        ).find()

        logger.debug("Constants usage:\n${usedComponents.toPrettyString()}")
        constantUsageReportFile.writeText(usedComponents.toJson())
    }
}

/*
 * TODO@tsr all this stuff below looks very similar to InlineMemberExtractionTask
 */

private class JavaOrKotlinConstantDetector(
    private val logger: Logger,
    private val artifacts: List<Artifact>,
    private val javaSourceFiles: FileCollection,
    private val kotlinSourceFiles: FileCollection
) {

    fun find(): Set<Dependency> {
        val constantImports: Set<ComponentWithConstantMembers> = artifacts
            .map { artifact ->
                artifact to JavaOrKotlinConstantMemberFinder(logger, ZipFile(artifact.file)).find()
            }.filterNot { (_, imports) -> imports.isEmpty() }
            .map { (artifact, imports) -> ComponentWithConstantMembers(artifact.dependency, imports) }
            .toSortedSet()

//        val javaConstants = JavaConstantUsageFinder(javaSourceFiles, constantImports).find()
//        val kotlinConstants = KotlinConstantUsageFinder(kotlinSourceFiles, constantImports).find()
        val javaConstants = JvmConstantUsageFinder(javaSourceFiles, constantImports).find()
        val kotlinConstants = JvmConstantUsageFinder(kotlinSourceFiles, constantImports).find()
        return javaConstants.plus(kotlinConstants)
    }
}

/**
 * Parses bytecode looking for constant declarations.
 */
private class JavaOrKotlinConstantMemberFinder(
    private val logger: Logger,
    private val zipFile: ZipFile
) {

    /**
     * Returns either an empty list, if there are no constants, or a list of import candidates. E.g.:
     * ```
     * [
     *   "com.myapp.BuildConfig.*",
     *   "com.myapp.BuildConfig.DEBUG"
     * ]
     * ```
     * An import statement with either of those would import the `com.myapp.BuildConfig.DEBUG` constant, contributed by
     * the "com.myapp" module.
     */
    fun find(): Set<String> {
        val entries = zipFile.entries().toList()

        return entries
            .filter { it.name.endsWith(".class") }
            .flatMap { entry ->
                val classReader = zipFile.getInputStream(entry).use { ClassReader(it.readBytes()) }
                val constantVisitor = ConstantVisitor(logger)
                classReader.accept(constantVisitor, 0)

                val fqcn = constantVisitor.className
                    .replace("/", ".")
                    .replace("$", ".")
                val constantMembers = constantVisitor.classes

                if (constantMembers.isNotEmpty()) {
                    listOf(
                        // import com.myapp.BuildConfig -> BuildConfig.DEBUG
                        fqcn,
                        // import com.myapp.BuildConfig.* -> DEBUG
                        "$fqcn.*",
                        // import com.myapp.* -> /* Kotlin file with top-level const val declarations */
                        "${fqcn.substring(0, fqcn.lastIndexOf("."))}.*"
                    ) + constantMembers.map { name -> "$fqcn.$name" }
                } else {
                    emptyList()
                }
            }.toSortedSet()
    }
}

private class JavaConstantUsageFinder(
    private val javaSourceFiles: FileCollection,
    private val constantImportCandidates: Set<ComponentWithConstantMembers>
) {

    /**
     * Looks at all the Java source in the project and scans for any import that is for a known constant member.
     * Returns the set of [Dependency]s that contribute these used constant members.
     */
    fun find(): Set<Dependency> {
        return javaSourceFiles
            .flatMap { source -> parseJavaSourceFileForImports(source) }
            .mapNotNull { actualImport -> findActualConstantImports(actualImport) }
            .toSet()
    }

    private fun parseJavaSourceFileForImports(file: File): Set<String> {
        val parser = newJavaParser(file)
        val importFinder = walkTree(parser)
        return importFinder.imports()
    }

    private fun newJavaParser(file: File): JavaParser {
        val input = FileInputStream(file).use { fis -> CharStreams.fromStream(fis) }
        val lexer = JavaLexer(input)
        val tokens = CommonTokenStream(lexer)
        return JavaParser(tokens)
    }

    private fun walkTree(parser: JavaParser): JavaImportFinder {
        val tree = parser.compilationUnit()
        val walker = ParseTreeWalker()
        val importFinder = JavaImportFinder()
        walker.walk(importFinder, tree)
        return importFinder
    }

    /**
     * [actualImport] is, e.g.,
     * * `com.myapp.BuildConfig.DEBUG`
     * * `com.myapp.BuildConfig.*`
     */
    private fun findActualConstantImports(actualImport: String): Dependency? {
        return constantImportCandidates.find {
            it.imports.contains(actualImport)
        }?.dependency
    }
}

private class KotlinConstantUsageFinder(
    private val kotlinSourceFiles: FileCollection,
    private val constantImportCandidates: Set<ComponentWithConstantMembers>
) {
    /**
     * Looks at all the Java source in the project and scans for any import that is for a known constant member.
     * Returns the set of [Dependency]s that contribute these used constant members.
     */
    fun find(): Set<Dependency> {
        return kotlinSourceFiles
            .flatMap { source -> parseKotlinSourceFileForImports(source) }
            .flatMap { actualImport -> findActualConstantImports(actualImport) }
            .toSet()
    }

    private fun parseKotlinSourceFileForImports(file: File): Set<String> {
        val parser = newKotlinParser(file)
        val importFinder = walkTree(parser)
        return importFinder.imports()
    }

    private fun newKotlinParser(file: File): KotlinParser {
        val input = FileInputStream(file).use { fis -> CharStreams.fromStream(fis) }
        val lexer = KotlinLexer(input)
        val tokens = CommonTokenStream(lexer)
        return KotlinParser(tokens)
    }

    private fun walkTree(parser: KotlinParser): KotlinImportFinder {
        val tree = parser.kotlinFile()
        val walker = ParseTreeWalker()
        val importFinder = KotlinImportFinder()
        walker.walk(importFinder, tree)
        return importFinder
    }

    /**
     * [actualImport] is, e.g.,
     * * `com.myapp.BuildConfig.DEBUG`
     * * `com.myapp.BuildConfig.*`
     */
    private fun findActualConstantImports(actualImport: String): List<Dependency> {
        // TODO@tsr it's a little disturbing there can be multiple matches. An issue with this naive algorithm.
        // TODO@tsr I need to be more intelligent in source parsing. Look at actual identifiers being used and associate those with their star-imports
        return constantImportCandidates.filter {
            it.imports.contains(actualImport)
        }.map {
            it.dependency
        }
    }
}

private class JvmConstantUsageFinder(
    private val kotlinSourceFiles: FileCollection,
    private val constantImportCandidates: Set<ComponentWithConstantMembers>
) {
    /**
     * Looks at all the Java source in the project and scans for any import that is for a known constant member.
     * Returns the set of [Dependency]s that contribute these used constant members.
     */
    fun find(): Set<Dependency> {
        return kotlinSourceFiles
            .flatMap { source -> parseSourceFileForImports(source) }
            .flatMap { actualImport -> findActualConstantImports(actualImport) }
            .toSet()
    }

    private fun parseSourceFileForImports(file: File): Set<String> {
        val parser = newSimpleParser(file)
        val importFinder = walkTree(parser)
        return importFinder.imports()
    }

    private fun newSimpleParser(file: File): SimpleParser {
        val input = FileInputStream(file).use { fis -> CharStreams.fromStream(fis) }
        val lexer = SimpleLexer(input)
        val tokens = CommonTokenStream(lexer)
        return SimpleParser(tokens)
    }

    private fun walkTree(parser: SimpleParser): SimpleImportFinder {
        val tree = parser.file()
        val walker = ParseTreeWalker()
        val importFinder = SimpleImportFinder()
        walker.walk(importFinder, tree)
        return importFinder
    }

    /**
     * [actualImport] is, e.g.,
     * * `com.myapp.BuildConfig.DEBUG`
     * * `com.myapp.BuildConfig.*`
     */
    private fun findActualConstantImports(actualImport: String): List<Dependency> {
        // TODO@tsr it's a little disturbing there can be multiple matches. An issue with this naive algorithm.
        // TODO@tsr I need to be more intelligent in source parsing. Look at actual identifiers being used and associate those with their star-imports
        return constantImportCandidates.filter {
            it.imports.contains(actualImport)
        }.map {
            it.dependency
        }
    }
}

/**
 * The `Simple` grammar only cares about imports and throws out the rest of the input. Works for Java and Kotlin. Should
 * be faster than the language-specific listeners, which parse everything.
 */
private class SimpleImportFinder : SimpleBaseListener() {

    private val imports = mutableSetOf<String>()

    internal fun imports(): Set<String> = imports

    override fun enterImportDeclaration(ctx: SimpleParser.ImportDeclarationContext) {
        val qualifiedName = ctx.qualifiedName().text
        val import = if (ctx.children.any { it.text == "*" }) {
            "$qualifiedName.*"
        } else {
            qualifiedName
        }

        imports.add(import)
    }
}

private class JavaImportFinder : JavaBaseListener() {

    private val imports = mutableSetOf<String>()

    internal fun imports(): Set<String> = imports

    override fun enterImportDeclaration(ctx: JavaParser.ImportDeclarationContext) {
        val qualifiedName = ctx.qualifiedName().text
        val import = if (ctx.children.any { it.text == "*" }) {
            "$qualifiedName.*"
        } else {
            qualifiedName
        }

        imports.add(import)
    }
}

private class KotlinImportFinder : KotlinParserBaseListener() {

    private val imports = mutableSetOf<String>()

    internal fun imports(): Set<String> = imports

    override fun enterImportHeader(ctx: KotlinParser.ImportHeaderContext) {
        val qualifiedName = ctx.identifier().text
        val import = if (ctx.MULT()?.text == "*") {
            "$qualifiedName.*"
        } else {
            qualifiedName
        }

        imports.add(import)
    }
}

/*
 * TODO@tsr some thoughts on an improved algo:
 * Need a data structure that includes the following import patterns from providers:
 * 1. com.myapp.MyClass                // Import of class containing constant thing -> MyClass.CONSTANT_THING
 * 2. com.myapp.MyClass.CONSTANT_THING // Direct import of constant thing -> CONSTANT_THING
 * 3. com.myapp.MyClass.*              // Star-import of all constant things in MyClass -> CONSTANT_THING_1, CONSTANT_THING_2
 * 4. com.myapp.*                      // Kotlin top-level declarations in com.myapp package -> CONSTANT_THING
 *
 * 3 and 4 (mostly 4) are problematic because they results in non-uniquely identifiable component providers of
 * constants.
 *
 * If, on the consumer side, I see one of those import patterns, I could also look for `SimpleIdentifier`s and associate
 * those with constant things provided by the providers. My data structure would need the addition of simple identifiers
 * for each class/package.
 */
