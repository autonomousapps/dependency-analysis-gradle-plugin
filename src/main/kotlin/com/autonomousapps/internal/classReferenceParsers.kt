package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import org.gradle.api.logging.Logger
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
    protected abstract fun parseBytecode(): Set<String>

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

    private val logger = getLogger<JarReader>()
    private val zipFile = ZipFile(jarFile)

    override fun parseBytecode(): Set<String> {
        return zipFile.entries().toList()
            .filter { it.name.endsWith(".class") }
            .flatMap { classEntry ->
                zipFile.getInputStream(classEntry).use { BytecodeParser(it.readBytes(), logger).parse() }
            }.toSet()
    }
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

    private val logger = getLogger<ClassSetReader>()

    override fun parseBytecode(): Set<String> {
        return classes.flatMap { classFile ->
            classFile.inputStream().use { BytecodeParser(it.readBytes(), logger).parse() }
        }.toSet()
    }
}

private class BytecodeParser(private val bytes: ByteArray, private val logger: Logger) {
    fun parse(): Set<String> {
        val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes)
            // Constant pool has a lot of weird bullshit in it
            .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }
            //.onEach { println("CONSTANT: $it") }

        val classEntries = ClassReader(bytes).let { classReader ->
            ClassAnalyzer(logger).apply {
                classReader.accept(this, 0)
            }
        }.classes()

        return constantPool.plus(classEntries)
            // Filter out `java` packages, but not `javax`
            .filterNot { it.startsWith("java/") }
            .map { it.replace("/", ".") }
            .toSet()
    }
}

private inline fun <R> NodeList.map(transform: (Node) -> R): List<R> {
    val destination = ArrayList<R>(length)
    for (i in 0 until length) {
        destination.add(transform(item(i)))
    }
    return destination
}
