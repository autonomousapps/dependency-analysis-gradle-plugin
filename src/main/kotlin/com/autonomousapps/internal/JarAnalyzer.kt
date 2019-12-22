package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Given a jar and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a set of FQCN
 * references present in these inputs, as strings. These inputs are part of a single logical whole, viz., the Gradle
 * project being analyzed.
 *
 * TODO reconsider name. This actually analyzes a gradle project such as an android library or java library (but NOT an android app)
 */
internal class JarAnalyzer(
    private val jarFile: File,
    private val layouts: Set<File>,
    private val kaptJavaSource: Set<File>
) {

    private val logger = LoggerFactory.getLogger(JarAnalyzer::class.java)

    // TODO some jars only have metadata. What to do about them?
    // 1. e.g. kotlin-stdlib-common-1.3.50.jar
    // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
    internal fun analyze(): Set<String> {
        // Analyze class usage in jar file
        val z = ZipFile(jarFile)
        val classNames = z.entries().toList()
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { classEntry -> z.getInputStream(classEntry).use { ClassReader(it.readBytes()) } }
            .collectClassNames(logger)

        // Analyze class usage in layout files
        layouts.map { layoutFile ->
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(layoutFile)
            document.documentElement.normalize()
            document.getElementsByTagName("*")
        }.flatMap { nodeList ->
            nodeList.map { it.nodeName }.filter { it.contains(".") }
        }.fold(classNames) { set, item -> set.apply { add(item) } }

        // Analyze class usage in Kapt Java source (only location for some annotations)
        collectFromSource(kaptJavaSource, classNames)

        return classNames
    }
}

// TODO I really want to collapse this into the class above, somehow
internal class ClassListAnalyzer(
    private val classes: Set<File>,
    private val layouts: Set<File>,
    private val kaptJavaSource: Set<File>
) {

    private val logger = LoggerFactory.getLogger(ClassListAnalyzer::class.java)

    // TODO some jars only have metadata. What to do about them?
    // 1. e.g. kotlin-stdlib-common-1.3.50.jar
    // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
    internal fun analyze(): Set<String> {
        // Analyze class usage in collection of class files
        val classNames = classes
            .map { classFile -> classFile.inputStream().use { ClassReader(it) } }
            .collectClassNames(logger)

        // Analyze class usage in layout files
        layouts.map { layoutFile ->
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(layoutFile)
            document.documentElement.normalize()
            document.getElementsByTagName("*")
        }.flatMap { nodeList ->
            nodeList.map { it.nodeName }.filter { it.contains(".") }
        }.fold(classNames) { set, item -> set.apply { add(item) } }

        // Analyze class usage in Kapt Java source (only location for some annotations)
        collectFromSource(kaptJavaSource, classNames)

        return classNames
    }
}

private fun collectFromSource(kaptJavaSource: Set<File>, classNames: MutableSet<String>) {
    kaptJavaSource
        .flatMap { it.readLines() }
        // This is grabbing things that aren't class names. E.g., urls, method calls. Maybe it doesn't matter, though.
        // If they can't be associated with a module, then they're ignored later in the analysis. Some FQCN references
        // are only available via import statements; others via FQCN in the body. Should be improved, but it's unclear
        // how best.
        .flatMap { JAVA_FQCN_REGEX.findAll(it).toList() }
        .map { it.value }
        .map { it.removeSuffix(".class") }
        .fold(classNames) { set, item -> set.apply { add(item) } }
}

private fun Iterable<ClassReader>.collectClassNames(logger: Logger): MutableSet<String> =
    map {
        val classNameCollector = ClassAnalyzer(logger)
        it.accept(classNameCollector, 0)
        classNameCollector
    }
        .flatMap { it.classes() }
        // Filter out `java` packages, but not `javax`
        .filterNot { it.startsWith("java/") }
        .map { it.replace("/", ".") }
        .toSortedSet()

private inline fun <R> NodeList.map(transform: (Node) -> R): List<R> {
    val destination = ArrayList<R>(length)
    for (i in 0 until length) {
        destination.add(transform(item(i)))
    }
    return destination
}
