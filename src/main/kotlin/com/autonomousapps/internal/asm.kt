package com.autonomousapps.internal

import com.autonomousapps.internal.asm.*
import com.autonomousapps.internal.asm.Opcodes.ASM7
import org.slf4j.Logger

private var logDebug = true

/**
 * This will collect the class name, only.
 */
class ClassNameCollector(private val logger: Logger) : ClassVisitor(ASM7) {

    var className: String? = null

    private fun log(msg: String) {
        logger.debug(msg)
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        log("ClassNameCollector#visit: $name extends $superName {")
        className = name
    }
}

/**
 * This will collect the class name and the name of all classes used by this class and the methods of this class.
 */
class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM7) {

    private val classes = mutableSetOf<String>()
    private val methodAnalyzer = MethodAnalyzer(logger, classes)
    private val fieldAnalyzer = FieldAnalyzer(logger, classes)
    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    internal lateinit var className: String

    fun classes(): Set<String> = classes

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    private fun log(msg: String) {
        if (logDebug) {
            logger.debug(msg)
        } else {
            logger.warn(msg)
        }
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        log("ClassAnalyzer#visit: $name extends $superName {")
        className = name
        addClass("L$superName;")
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        log("ClassAnalyzer#visitField: $descriptor $name")
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
        log("ClassAnalyzer#visitMethod: $name $descriptor")

        descriptor?.let {
            METHOD_DESCRIPTOR_REGEX.findAll(it).forEach { result ->
                addClass(result.value)
            }
        }

        return methodAnalyzer
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        log("ClassAnalyzer#visitAnnotation: descriptor=$descriptor visible=$visible")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        log("ClassAnalyzer#visitTypeAnnotation: typeRef=$typeRef typePath=$typePath descriptor=$descriptor visible=$visible")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitEnd() {
        log("}")
        logDebug = true
    }
}

class MethodAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : MethodVisitor(ASM7) {

    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    private fun log(msg: String) {
        if (logDebug) {
            logger.debug(msg)
        } else {
            logger.warn(msg)
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        log("- MethodAnalyzer#visitTypeInsn: $type")
        // Type can look like `java/lang/Enum` or `[Lcom/package/Thing;`, which is fucking weird
        addClass(if (type?.startsWith("[") == true) type else "L$type;")
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        log("- MethodAnalyzer#visitFieldInsn: $owner.$name $descriptor")
        addClass("L$owner;")
        addClass(descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        log("- MethodAnalyzer#visitMethodInsn: $owner.$name $descriptor")
        // Owner can look like `java/lang/Enum` or `[Lcom/package/Thing;`, which is fucking weird
        addClass(if (owner?.startsWith("[") == true) owner else "L$owner;")
        descriptor?.let {
            METHOD_DESCRIPTOR_REGEX.findAll(it).forEach { result ->
                addClass(result.value)
            }
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        log("- MethodAnalyzer#visitInvokeDynamicInsn: $name $descriptor")
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
        log("- MethodAnalyzer#visitLocalVariable: $name $descriptor")
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
        log("- MethodAnalyzer#visitLocalVariableAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        log("- MethodAnalyzer#visitAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        log("- MethodAnalyzer#visitInsnAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        log("- MethodAnalyzer#visitParameterAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        log("- MethodAnalyzer#visitTryCatchBlock: $type")
        addClass("L$type;")
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        log("- MethodAnalyzer#visitTryCatchAnnotation: $descriptor")
        addClass(descriptor)
        return annotationAnalyzer
    }
}

private class AnnotationAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>,
    private val level: Int = 0
) : AnnotationVisitor(ASM7) {

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    private fun log(msg: String) {
        if (logDebug) {
            logger.debug(msg)
        } else {
            logger.warn(msg)
        }
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

        log("${indent()}- AnnotationAnalyzer#visit: name=$name, value=(${value?.javaClass?.simpleName}, ${getValue(value)})")
        if (value is String) {
            addClass(value)
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        log("${indent()}- AnnotationAnalyzer#visitEnum: name=$name, descriptor=$descriptor, value=$value")
        addClass(descriptor)
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
        log("${indent()}- AnnotationAnalyzer#visitAnnotation: name=$name, descriptor=$descriptor")
        addClass(descriptor)
        return AnnotationAnalyzer(logger, classes, level + 1)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        log("${indent()}- AnnotationAnalyzer#visitArray: name=$name")
        return AnnotationAnalyzer(logger, classes, level + 1)
    }
}

private class FieldAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : FieldVisitor(ASM7) {

    private val annotationAnalyzer = AnnotationAnalyzer(logger, classes)

    private fun log(msg: String) {
        if (logDebug) {
            logger.debug(msg)
        } else {
            logger.warn(msg)
        }
    }

    private fun addClass(className: String?) {
        classes.addClass(className)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        addClass(descriptor)
        return annotationAnalyzer
    }
}

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
