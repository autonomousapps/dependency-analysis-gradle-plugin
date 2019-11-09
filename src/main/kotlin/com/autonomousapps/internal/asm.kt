package com.autonomousapps.internal

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM4
import org.slf4j.Logger

/**
 * This will collect the class name, only.
 */
class ClassNameCollector(private val logger: Logger) : ClassVisitor(ASM4) {

    var className: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        logger.debug("$name extends $superName {")
        className = name
    }
}

/**
 * This will collect the class name and the name of all classes used by this class and the methods of this class.
 */
class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM4) {

    private val classes = mutableSetOf<String>()
    private val methodAnalyzer = MethodAnalyzer(logger, classes)
    private val fieldAnalyzer = FieldAnalyzer(logger, classes)
    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    fun classes(): Set<String> = classes

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        logger.debug("$name extends $superName {")
        addClass(superName)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        logger.debug("    $descriptor $name")
        addClass(descriptor)
        return fieldAnalyzer
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        logger.debug("    $name $descriptor")

        descriptor?.let {
            METHOD_DESCRIPTOR_REGEX.findAll(it).forEach {
                addClass(it.value)
            }
        }

        return methodAnalyzer
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("ClassAnalyzer#visitAnnotation: descriptor=$descriptor visible=$visible")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("ClassAnalyzer#visitTypeAnnotation: typeRef=$typeRef typePath=$typePath descriptor=$descriptor visible=$visible")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitEnd() {
        logger.debug("}")
    }
}

class MethodAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : MethodVisitor(ASM4) {

    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        logger.debug("    $type")
        addClass(type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        logger.debug("    $owner.$name $descriptor")
        addClass(descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        logger.debug("    $owner.$name $descriptor")
        descriptor?.let {
            METHOD_DESCRIPTOR_REGEX.findAll(it).forEach {
                addClass(it.value)
            }
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        logger.debug("    $name $descriptor")
        addClass(descriptor)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        logger.debug("    $name $descriptor")
        addClass(descriptor)
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        start: Array<out Label>?,
        end: Array<out Label>?,
        index: IntArray?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("MethodAnalyzer#visitLocalVariableAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("MethodAnalyzer#visitAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("MethodAnalyzer#visitInsnAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("MethodAnalyzer#visitParameterAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        logger.debug("    $type")
        addClass(type)
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("MethodAnalyzer#visitTryCatchAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }
}

private class AnnotationAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>,
    private val level: Int = 0
) : AnnotationVisitor(ASM4) {

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    private fun indent() = "  ".repeat(level)

    override fun visit(name: String?, value: Any?) {
        fun getValue(value: Any?): String? {
            return if (value is String && value.contains("\n")) {
                ""
            } else {
                value.toString()
            }
        }

        logger.debug("${indent()}- AnnotationAnalyzer#visit: name=$name, value=(${value?.javaClass?.simpleName}, ${getValue(value)})")
        if (value is String) {
            addClass(value)
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        logger.debug("${indent()}- AnnotationAnalyzer#visitEnum: name=$name, descriptor=$descriptor, value=$value")
        addClass(descriptor)
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
        logger.debug("${indent()}- AnnotationAnalyzer#visitAnnotation: name=$name, descriptor=$descriptor")
        addClass(descriptor)
        return AnnotationAnalyzer(logger, classes, level + 1)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        logger.debug("${indent()}- AnnotationAnalyzer#visitArray: name=$name")
        return AnnotationAnalyzer(logger, classes, level + 1)
    }
}

private class FieldAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : FieldVisitor(ASM4) {

    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        addClass(descriptor)
        return annotationAnalyzer
    }
}

private val METHOD_DESCRIPTOR_REGEX = """L[\w/]+;""".toRegex()

private fun MutableSet<String>.addClass(className: String?) {
    className?.let {
        // Strip array indicators
        it.replace("[", "")
        // Only add class types (not primitives)
        if (it.startsWith("L")) {
            add(it.substring(1, it.length - 1))
        }
    }
}
