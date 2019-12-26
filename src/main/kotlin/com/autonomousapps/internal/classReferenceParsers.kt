package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

internal sealed class ProjectClassReferenceParser(
    private val layouts: Set<File>,
    private val kaptJavaSource: Set<File>
) {

    /**
     * Source is either a jar or set of class files.
     */
    protected abstract fun parseBytecode(): List<String>

    private fun parseLayouts(): List<String> {
        return layouts.map { layoutFile ->
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(layoutFile)
            document.documentElement.normalize()
            document.getElementsByTagName("*")
        }.flatMap { nodeList ->
            nodeList.map { it.nodeName }.filter { it.contains(".") }
        }
    }

    private fun parseKaptJavaSource(): List<String> {
        return kaptJavaSource
            .flatMap { it.readLines() }
            // This is grabbing things that aren't class names. E.g., urls, method calls. Maybe it doesn't matter, though.
            // If they can't be associated with a module, then they're ignored later in the analysis. Some FQCN references
            // are only available via import statements; others via FQCN in the body. Should be improved, but it's unclear
            // how best.
            .flatMap { JAVA_FQCN_REGEX.findAll(it).toList() }
            .map { it.value }
            .map { it.removeSuffix(".class") }
    }

    // TODO some jars only have metadata. What to do about them?
    // 1. e.g. kotlin-stdlib-common-1.3.50.jar
    // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
    internal fun analyze(): Set<String> = (parseBytecode() + parseLayouts() + parseKaptJavaSource()).toSortedSet()
}

/**
 * Given a jar and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a set of FQCN
 * references present in these inputs, as strings. These inputs are part of a single logical whole, viz., the Gradle
 * project being analyzed.
 */
internal class JarReader(
    jarFile: File,
    layouts: Set<File>,
    kaptJavaSource: Set<File>
) : ProjectClassReferenceParser(layouts = layouts, kaptJavaSource = kaptJavaSource) {

    private val logger = Logging.getLogger(JarReader::class.java)
    private val zipFile = ZipFile(jarFile)

    override fun parseBytecode() = zipFile.entries().toList()
        .filterNot { it.isDirectory }
        .filter { it.name.endsWith(".class") }
        .map { classEntry -> zipFile.getInputStream(classEntry).use { ClassReader(it.readBytes()) } }
        .collectClassNames(logger)
}

/**
 * Given a set of .class files and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a
 * set of FQCN references present in these inputs, as strings. These inputs are part of a single logical whole, viz.,
 * the Gradle project being analyzed.
 */
internal class ClassSetReader(
    private val classes: Set<File>,
    layouts: Set<File>,
    kaptJavaSource: Set<File>
) : ProjectClassReferenceParser(layouts = layouts, kaptJavaSource = kaptJavaSource) {

    private val logger = Logging.getLogger(ClassSetReader::class.java)

    override fun parseBytecode() = classes
        .map { classFile -> classFile.inputStream().use { ClassReader(it) } }
        .collectClassNames(logger)
}

private fun Iterable<ClassReader>.collectClassNames(logger: Logger): List<String> =
    map {
        val classNameCollector = ClassAnalyzer(logger)
        it.accept(classNameCollector, 0)
        classNameCollector
    }
        .flatMap { it.classes() }
        // Filter out `java` packages, but not `javax`
        .filterNot { it.startsWith("java/") }
        .map { it.replace("/", ".") }

private inline fun <R> NodeList.map(transform: (Node) -> R): List<R> {
    val destination = ArrayList<R>(length)
    for (i in 0 until length) {
        destination.add(transform(item(i)))
    }
    return destination
}
