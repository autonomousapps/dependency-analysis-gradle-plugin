package com.autonomousapps.internal

import com.autonomousapps.internal.asm.*
import com.autonomousapps.internal.asm.Opcodes.ASM8
import com.autonomousapps.internal.utils.DESC_REGEX
import com.autonomousapps.internal.utils.METHOD_DESCRIPTOR_REGEX
import com.autonomousapps.internal.utils.efficient
import kotlinx.metadata.jvm.KotlinClassHeader
import org.gradle.api.logging.Logger
import java.util.concurrent.atomic.AtomicReference

private var logDebug = true

/**
 * This class will detect usage of annotations that are supported by annotation processors used by the project.
 */
internal class ProcClassVisitor(
  private val logger: Logger,
  private val annotationProcessors: Set<AnnotationProcessor>
) : ClassVisitor(ASM8) {

  private var className: String? = null
  private val usedProcs = mutableListOf<AnnotationProcessor>()

  internal fun usedProcs(): List<AnnotationProcessor> = usedProcs

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
    name: String?,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    className = name
    log("ProcClassVisitor#visit: $name extends $superName")
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    log("- ProcClassVisitor#visitAnnotation ($className): descriptor=$descriptor")

    val found = findUsedProcs(descriptor)
    return if (!found) null else ProcAnnotationVisitor()
  }

  override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
    log("- ProcClassVisitor#visitMethod ($className): descriptor=$descriptor")
    return ProcMethodVisitor()
  }

  override fun visitField(access: Int, name: String?, descriptor: String?, signature: String?, value: Any?): FieldVisitor? {
    log("- ProcClassVisitor#visitField ($className): descriptor=$descriptor")
    return ProcFieldVisitor()
  }

  override fun visitEnd() {
    log("- ProcClassVisitor#visitEnd ($className)")
  }

  private fun findUsedProcs(descriptor: String?): Boolean {
    if (descriptor == null) {
      return false
    }

    val theAnno = DESC_REGEX.find(descriptor)?.let { it.groupValues[1] }
      ?.replace("/", ".")
      ?: return false

    for (entry in annotationProcessors) {
      if (entry.supportedAnnotationTypes.contains(theAnno)) {
        // Found a match! This classes uses annotation processor `proc`
        usedProcs.add(entry)
      }
    }
    return true
  }

  private inner class ProcAnnotationVisitor : AnnotationVisitor(ASM8) {
    override fun visit(name: String?, value: Any?) {
      log("ProcAnnotationVisitor#visit: $name value=$value")
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
      log("- ProcAnnotationVisitor#visitAnnotation: $name descriptor=$descriptor")

      val found = findUsedProcs(descriptor)
      return if (!found) null else ProcAnnotationVisitor()
    }
  }

  private inner class ProcMethodVisitor : MethodVisitor(ASM8) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
      log("ProcMethodVisitor#visitAnnotation: descriptor=$descriptor")

      val found = findUsedProcs(descriptor)
      return if (!found) null else ProcAnnotationVisitor()
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
      log("ProcMethodVisitor#visitParameterAnnotation: descriptor=$descriptor")

      val found = findUsedProcs(descriptor)
      return if (!found) null else ProcAnnotationVisitor()
    }
  }

  private inner class ProcFieldVisitor : FieldVisitor(ASM8) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
      log("ProcFieldVisitor#visitAnnotation: descriptor=$descriptor")

      val found = findUsedProcs(descriptor)
      return if (!found) null else ProcAnnotationVisitor()
    }
  }
}

/**
 * This will collect the class name and information about annotations.
 */
internal class ClassNameAndAnnotationsVisitor(private val logger: Logger) : ClassVisitor(ASM8) {

  private lateinit var className: String private set
  private lateinit var access: Access
  private var superClassName: String? = null
  private val retentionPolicyHolder = AtomicReference("")
  private var isAnnotation = false
  private val methods = mutableSetOf<Method>()
  private val innerClasses = mutableSetOf<String>()

  private var methodCount = 0
  private var fieldCount = 0

  // From old ConstantVisitor
  private val constantClasses = mutableSetOf<String>()

  internal fun getAnalyzedClass(): AnalyzedClass {
    val className = this.className
    val access = this.access
    val hasNoMembers = fieldCount == 0 && methodCount == 0
    return AnalyzedClass(
      className = className,
      superClassName = superClassName,
      retentionPolicy = retentionPolicyHolder.get(),
      isAnnotation = isAnnotation,
      hasNoMembers = hasNoMembers,
      access = access,
      methods = methods.efficient(),
      innerClasses = innerClasses.efficient(),
      constantClasses = constantClasses
    )
  }

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    className = name
    superClassName = superName
    if (interfaces?.contains("java/lang/annotation/Annotation") == true) {
      isAnnotation = true
    }
    this.access = Access.fromInt(access)

    val implementsClause = if (interfaces.isNullOrEmpty()) {
      ""
    } else {
      " implements ${interfaces.joinToString(", ")}"
    }
    log("ClassNameAndAnnotationsVisitor#visit: ${this.access} $name extends $superName$implementsClause")
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if ("Ljava/lang/annotation/Retention;" == descriptor) {
      log("- ClassNameAndAnnotationsVisitor#visitAnnotation ($className): descriptor=$descriptor visible=$visible")
      return RetentionPolicyAnnotationVisitor(logger, className, retentionPolicyHolder)
    }
    return null
  }

  override fun visitMethod(
    access: Int, name: String?, descriptor: String, signature: String?, exceptions: Array<out String>?
  ): MethodVisitor? {
    log("- visitMethod: descriptor=$descriptor ame=$name signature=$signature")
    if (!("()V" == descriptor && ("<init>" == name || "<clinit>" == name))) {
      // ignore constructors and static initializers
      methodCount++
      methods.add(Method(descriptor))
    }
    return null
  }

  override fun visitField(
    access: Int, name: String, descriptor: String?, signature: String?, value: Any?
  ): FieldVisitor? {
    log("- visitField: descriptor=$descriptor name=$name signature=$signature value=$value")
    fieldCount++

    // from old ConstantVisitor
    if (isStaticFinal(access)) {
      constantClasses.add(name)
    }

    return null
  }

  override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
    log("- visitInnerClass: name=$name outerName=$outerName innerName=$innerName")
    innerClasses.add(name.replace("/", "."))
  }

  override fun visitSource(source: String?, debug: String?) {
    log("- visitSource: source=$source debug=$debug")
  }

  override fun visitEnd() {
    log("- visitEnd: fieldCount=$fieldCount methodCount=$methodCount")
  }

  private fun log(msg: String) {
    logger.debug(msg)
  }

  private class RetentionPolicyAnnotationVisitor(
    private val logger: Logger,
    private val className: String?,
    private val retentionPolicyHolder: AtomicReference<String>
  ) : AnnotationVisitor(ASM8) {

    private fun log(msg: String) {
      logger.debug(msg)
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
      if ("Ljava/lang/annotation/RetentionPolicy;" == descriptor) {
        log("  - RetentionPolicyAnnotationVisitor#visitEnum ($className): $value")
        retentionPolicyHolder.set(value)
      }
    }
  }
}

/**
 * This will collect the class name and the name of all classes used by this class and the methods of this class.
 */
class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM8) {

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
    log("ClassAnalyzer#visit: $name extends $superName")
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
    log("\n")
  }
}

class MethodAnalyzer(
  private val logger: Logger,
  private val classes: MutableSet<String>
) : MethodVisitor(ASM8) {

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
) : AnnotationVisitor(ASM8) {

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
      METHOD_DESCRIPTOR_REGEX.findAll(value).forEach { result ->
        addClass(result.value)
      }
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
) : FieldVisitor(ASM8) {

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
    log("- FieldAnalyzer#visitAnnotation: $descriptor")
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

/* ===================================================================================================
 * Below here used for parsing kotlin.Metadata with the ultimate goal of listing all inline functions.
 * ===================================================================================================
 */

internal class KotlinClassHeaderBuilder {

  var kind: Int = 1
  var metadataVersion: IntArray? = null
  var bytecodeVersion: IntArray? = null
  var data1 = mutableListOf<String>()
  var data2 = mutableListOf<String>()
  var extraString: String? = null
  var packageName: String? = null
  var extraInt: Int = 0

  fun build(): KotlinClassHeader {
    return KotlinClassHeader(
      kind = kind,
      metadataVersion = metadataVersion,
      bytecodeVersion = bytecodeVersion,
      data1 = data1.toTypedArray(),
      data2 = data2.toTypedArray(),
      extraString = extraString,
      packageName = packageName,
      extraInt = extraInt
    )
  }
}

private const val KOTLIN_METADATA = "Lkotlin/Metadata;"

internal class KotlinMetadataVisitor(
  private val logger: Logger
) : ClassVisitor(ASM8) {

  internal lateinit var className: String
  internal var builder: KotlinClassHeaderBuilder? = null

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
    log("KotlinMetadataVisitor#visit: $name extends $superName")
    className = name
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    log("KotlinMetadataVisitor#visitAnnotation: descriptor=$descriptor visible=$visible")
    return if (KOTLIN_METADATA == descriptor) {
      builder = KotlinClassHeaderBuilder()
      KotlinAnnotationVisitor(logger, builder!!)
    } else {
      null
    }
  }

  private class KotlinAnnotationVisitor(
    private val logger: Logger,
    private val builder: KotlinClassHeaderBuilder,
    private val level: Int = 0,
    private val arrayName: String? = null
  ) : AnnotationVisitor(ASM8) {

    private fun log(msg: String) {
      if (logDebug) {
        logger.debug(msg)
      } else {
        logger.warn(msg)
      }
    }

    private fun indent() = "  ".repeat(level)

    override fun visit(name: String?, value: Any?) {
      log("${indent()}- visit: name=$name, value=${if (value == null) "null" else "..."})")
      when (name) {
        "k" -> builder.kind = value as Int
        "mv" -> builder.metadataVersion = value as IntArray
        "bv" -> builder.bytecodeVersion = value as IntArray
        "xs" -> builder.extraString = value as String
        "pn" -> builder.packageName = value as String
        "xi" -> builder.extraInt = value as Int
      }

      when (arrayName) {
        "d1" -> builder.data1.add(value as String)
        "d2" -> builder.data2.add(value as String)
      }
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
      log("${indent()}- visitArray: name=$name")
      return KotlinAnnotationVisitor(logger, builder, level + 1, name)
    }
  }
}

private fun isStaticFinal(access: Int): Boolean =
  access and Opcodes.ACC_STATIC != 0 && access and Opcodes.ACC_FINAL != 0
