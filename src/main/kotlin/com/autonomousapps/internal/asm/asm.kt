package com.autonomousapps.internal.asm

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
class ClassPrinter(private val logger: Logger) : ClassVisitor(ASM4) {

    private val _classes = mutableSetOf<String>()
    private val methodPrinter = MethodPrinter(logger, _classes)

    val classes: Set<String> = _classes

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        logger.debug("$name extends $superName {")
        superName?.let {
            logger.debug("adding ClassPrinter#visit $it")
            _classes.add(it)
        }
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        logger.debug("    $descriptor $name")
        descriptor?.let {
            if (it.startsWith("L")) {
                logger.debug("adding ClassPrinter#visitField ${it.substring(1, it.length - 1)}")
                _classes.add(it.substring(1, it.length - 1))
            }
        }
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
        descriptor?.removePrefix("(")?.replace(")", "")
            ?.split(";")
            ?.filter { it.startsWith("L") }
            ?.map { it.substring(1, it.length) }
            ?.forEach {
                logger.debug("adding ClassPrinter#visitMethod $it")
                _classes.add(it)
            }
        return methodPrinter
    }

    override fun visitEnd() {
        logger.debug("}")
    }
}

class MethodPrinter(
    private val logger: Logger,
    private val classes: MutableSet<String>
) : MethodVisitor(ASM4) {

    override fun visitParameter(name: String?, access: Int) {
        logger.debug("    (param) $name")
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        logger.debug("    (type) $type")
        type?.let {
            if (it.startsWith("[L")) {
                logger.debug("adding MethodPrinter#visitTypeInsn ${it.substring(2, it.length - 1)}")
                classes.add(it.substring(2, it.length - 1))
            }
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        logger.debug("    (field) $owner.$name $descriptor")
        owner?.let { classes.add(it) }
        descriptor?.let {
            if (it.startsWith("L")) {
                // TODO handle arrays
                logger.debug("adding MethodPrinter#visitFieldInsn ${it.substring(1, it.length - 1)}")
                classes.add(it.substring(1, it.length - 1))
            }
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        logger.debug("    (method) $owner.$name $descriptor")
        owner?.let { classes.add(it) }
        // e.g. (Ljava/lang/Object;Ljava/lang/String;)V
        descriptor?.removePrefix("(")?.replace(")", "")
            ?.split(";")
            ?.filter { it.startsWith("L") }
            ?.map { it.substring(1, it.length) }
            ?.forEach {
                logger.debug("adding MethodPrinter#visitMethodInsn $it")
                classes.add(it)
            }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        logger.debug("    (invokedynamic) $name $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitInvokeDynamicInsn $it")
            classes.add(it)
        }
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        logger.debug("    (localvariable) $name $descriptor")
        descriptor?.let {
            if (it.startsWith("L")) {
                logger.debug("adding MethodPrinter#visitLocalVariable ${it.substring(1, it.length - 1)}")
                classes.add(it.substring(1, it.length - 1))
            }
        }
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
        logger.debug("    (localvariable anno) $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitLocalVariableAnnotation $it")
            classes.add(it)
        }
        return null
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("    (method anno) $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitAnnotation ${it.substring(1, it.length - 1)}")
            classes.add(it.substring(1, it.length - 1))
        }
        return null
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("    (ins anno) $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitInsnAnnotation $it")
            classes.add(it)
        }
        return null
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        logger.debug("    (param anno) $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitParameterAnnotation ${it.substring(1, it.length - 1)}")
            classes.add(it.substring(1, it.length - 1))
        }
        return null
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        logger.debug("    (try/catch) $type")
        type?.let {
            logger.debug("adding MethodPrinter#visitTryCatchBlock $it")
            classes.add(it)
        }
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        logger.debug("    (try/catch anno) $descriptor")
        descriptor?.let {
            logger.debug("adding MethodPrinter#visitTryCatchAnnotation $it")
            classes.add(it)
        }
        return null
    }
}
