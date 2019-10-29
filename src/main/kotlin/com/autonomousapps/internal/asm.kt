package com.autonomousapps.internal

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.TypePath
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
    private val methodPrinter = MethodAnalyzer(logger, classes)

    fun classes(): Set<String> = classes

    private fun addClass(className: String?) {
        className?.let {
            // Strip array indicators
            it.replace("[", "")
            // Only add class types (not primitives)
            if (it.startsWith("L")) {
                classes.add(it.substring(1, it.length - 1))
            }
        }
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

        // TODO: visit and look for annotations
        return null
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
            """L[\w/]+;""".toRegex().findAll(it).forEach {
                addClass(it.value)
            }
        }

        return methodPrinter
    }

    override fun visitEnd() {
        logger.debug("}")
    }
}

class MethodAnalyzer(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : MethodVisitor(ASM4) {

    private fun addClass(className: String?) {
        className?.let {
            // Strip array indicators
            it.replace("[", "")
            // Only add class types (not primitives)
            if (it.startsWith("L")) {
                classes.add(it.substring(1, it.length - 1))
            }
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        logger.debug("    $type")
        addClass(type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        logger.debug("    $owner.$name $descriptor")
        addClass(owner)
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
        addClass(owner)
        addClass(descriptor)
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

        logger.debug("    $descriptor")
        addClass(descriptor)

        return null
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("    $descriptor")
        addClass(descriptor)

        return null
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("    $descriptor")
        addClass(descriptor)

        return null
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("    $descriptor")
        addClass(descriptor)

        return null
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
        logger.debug("    $descriptor")
        addClass(descriptor)

        return null
    }
}
