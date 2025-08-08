// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.Flags
import com.autonomousapps.internal.ClassNames.canonicalize
import com.autonomousapps.internal.asm.*
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX_ASM
import com.autonomousapps.internal.utils.METHOD_DESCRIPTOR_REGEX
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.genericTypes
import com.autonomousapps.model.internal.AccessFlags
import com.autonomousapps.model.internal.intermediates.consumer.LdcConstant
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.autonomousapps.model.internal.intermediates.producer.Constant
import com.autonomousapps.model.internal.intermediates.producer.Member
import org.gradle.api.logging.Logger
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.metadata.jvm.Metadata

private val logDebug: Boolean = Flags.logBytecodeDebug()
private const val ASM_VERSION = Opcodes.ASM9

/** This will collect the class name and information about annotations. */
internal class ClassNameAndAnnotationsVisitor(private val logger: Logger) : ClassVisitor(ASM_VERSION) {

  private lateinit var className: String
  private lateinit var access: AccessFlags
  private var outerClassName: String? = null
  private var superClassName: String? = null

  private var interfaces: Set<String>? = null
  private val retentionPolicyHolder = AtomicReference("")
  private var isAnnotation = false
  private val methods = mutableSetOf<Method>()
  private val innerClasses = mutableSetOf<String>()
  private val effectivelyPublicFields = mutableSetOf<Member.Field>()
  private val effectivelyPublicMethods = mutableSetOf<Member.Method>()

  private var methodCount = 0
  private var fieldCount = 0

  // From old ConstantVisitor
  private val constants = sortedSetOf<Constant>()

  internal fun getAnalyzedClass(): AnalyzedClass {
    val className = this.className
    val access = this.access
    val hasNoMembers = fieldCount == 0 && methodCount == 0

    return AnalyzedClass(
      className = className.intern(),
      outerClassName = outerClassName?.intern(),
      superClassName = superClassName?.intern(),
      interfaces = interfaces.orEmpty().efficient(),
      retentionPolicy = retentionPolicyHolder.get(),
      isAnnotation = isAnnotation,
      hasNoMembers = hasNoMembers,
      access = access,
      methods = methods.efficient(),
      innerClasses = innerClasses.efficient(),
      constants = constants.efficient(),
      effectivelyPublicFields = effectivelyPublicFields.efficient(),
      effectivelyPublicMethods = effectivelyPublicMethods.efficient(),
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
    this.superClassName = superName?.let { canonicalize(it) }
    this.interfaces = interfaces?.asSequence()
      ?.map { canonicalize(it) }
      ?.toSortedSet()
      .orEmpty()

    className = canonicalize(name)
    if (interfaces?.contains("java/lang/annotation/Annotation") == true) {
      isAnnotation = true
    }

    this.access = AccessFlags(access)

    val implementsClause = if (interfaces.isNullOrEmpty()) {
      ""
    } else {
      " implements ${interfaces.joinToString(", ")}"
    }
    log { "ClassNameAndAnnotationsVisitor#visit: ${this.access} $name extends $superName$implementsClause" }
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if ("Ljava/lang/annotation/Retention;" == descriptor) {
      log { "- ClassNameAndAnnotationsVisitor#visitAnnotation ($className): descriptor=$descriptor visible=$visible" }
      return RetentionPolicyAnnotationVisitor(logger, className, retentionPolicyHolder)
    }
    return null
  }

  override fun visitMethod(
    access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?
  ): MethodVisitor? {
    log { "- visitMethod: ${AccessFlags(access).getModifierString()} descriptor=$descriptor name=$name signature=$signature" }

    if (!("()V" == descriptor && ("<init>" == name || "<clinit>" == name))) {
      // ignore constructors and static initializers
      methodCount++
      methods.add(Method(descriptor))
    }

    // TODO(tsr): uncomment once intermediate artifact shrinking is complete
    // if (isEffectivelyPublic(access)) {
    //   effectivelyPublicMethods.add(
    //     Member.Method(
    //       access = access,
    //       name = name,
    //       descriptor = descriptor,
    //     )
    //   )
    // }

    return null
  }

  override fun visitField(
    access: Int, name: String, descriptor: String, signature: String?, value: Any?
  ): FieldVisitor? {
    val flags = AccessFlags(access)

    log { "- visitField: ${flags.getModifierString()} descriptor=$descriptor name=$name signature=$signature value=$value" }
    fieldCount++

    // from old ConstantVisitor
    if (flags.isPublicConstant) {
      constants.add(
        Constant(
          name = name.intern(),
          descriptor = descriptor,
          value = value.toString(),
        )
      )
    }

    // TODO(tsr): uncomment once intermediate artifact shrinking is complete
    // if (isEffectivelyPublic(access)) {
    //   effectivelyPublicFields.add(
    //     Member.Field(
    //       access = access,
    //       name = name,
    //       descriptor = descriptor,
    //     )
    //   )
    // }

    return null
  }

  override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
    log { "- visitOuterClass: owner=$owner name=$name descriptor=$descriptor" }
  }

  override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
    log { "- visitInnerClass: ${AccessFlags(access).getModifierString()} name=$name outerName=$outerName innerName=$innerName" }
    if (outerName != null) {
      outerClassName = canonicalize(outerName)
    }
    innerClasses.add(canonicalize(name))
  }

  override fun visitSource(source: String?, debug: String?) {
    log { "- visitSource: source=$source debug=$debug" }
  }

  override fun visitEnd() {
    log { "- visitEnd: fieldCount=$fieldCount methodCount=$methodCount" }
  }

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
    }
  }

  private class RetentionPolicyAnnotationVisitor(
    private val logger: Logger,
    private val className: String?,
    private val retentionPolicyHolder: AtomicReference<String>
  ) : AnnotationVisitor(ASM_VERSION) {

    private fun log(msgProvider: () -> String) {
      if (!logDebug) {
        logger.quiet(msgProvider())
      }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
      if ("Ljava/lang/annotation/RetentionPolicy;" == descriptor) {
        log { "  - RetentionPolicyAnnotationVisitor#visitEnum ($className): $value" }
        retentionPolicyHolder.set(value)
      }
    }
  }
}

internal data class ClassRef(
  val classRef: String,
  val kind: Kind,
  val enclosingAnnotation: String? = null,
) : Comparable<ClassRef> {

  enum class Kind {
    ANNOTATION,
    NOT_ANNOTATION,
  }

  override fun compareTo(other: ClassRef): Int {
    return compareBy<ClassRef> { it.classRef }
      .thenBy { it.kind }
      .thenBy { it.enclosingAnnotation }
      .compare(this, other)
  }
}

/**
 * This will collect the class name and the name of all classes used by this class and the methods of this class.
 */
internal class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM_VERSION) {

  var source: String? = null
  lateinit var className: String
  var superClass: String? = null
  val interfaces = sortedSetOf<String>()

  val classes = mutableSetOf<ClassRef>()
  private val binaryClasses = sortedMapOf<String, SortedSet<MemberAccess>>()
  private val constants = sortedSetOf<LdcConstant>()

  private val methodAnalyzer = MethodAnalyzer(logger, classes, binaryClasses, constants)
  private val fieldAnalyzer = FieldAnalyzer(logger, classes)

  fun getBinaryClasses(): Map<String, Set<MemberAccess>> = binaryClasses.efficient()
  fun getInferredConstants(): Set<LdcConstant> = constants.efficient()

  private fun addClass(className: String?, kind: ClassRef.Kind) {
    classes.addClass(className, kind)
  }

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
    }
  }

  override fun visitSource(source: String?, debug: String?) {
    log { "- visitSource: source=$source debug=$debug" }
    this.source = source
  }

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    log { "ClassAnalyzer#visit: ${AccessFlags(access).getModifierString()} $name extends $superName" }
    className = name
    superClass = superName
    this.interfaces.addAll(interfaces.orEmpty())

    addClass("L$superName;", ClassRef.Kind.NOT_ANNOTATION)
    interfaces?.forEach { i ->
      addClass("L$i;", ClassRef.Kind.NOT_ANNOTATION)
    }
  }

  override fun visitField(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    value: Any?
  ): FieldVisitor {
    log { "ClassAnalyzer#visitField: ${AccessFlags(access).getModifierString()} descriptor=$descriptor name=$name signature=$signature value=$value" }

    addClass(descriptor, ClassRef.Kind.NOT_ANNOTATION)

    // TODO probably do this for other `visitX` methods as well
    signature?.genericTypes()?.forEach {
      addClass(it, ClassRef.Kind.NOT_ANNOTATION)
    }

    return fieldAnalyzer
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    log { "ClassAnalyzer#visitMethod: ${AccessFlags(access).getModifierString()} $name $descriptor" }

    METHOD_DESCRIPTOR_REGEX.findAll(descriptor).forEach { result ->
      addClass(result.value, ClassRef.Kind.NOT_ANNOTATION)
    }

    return methodAnalyzer
  }

  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    log { "ClassAnalyzer#visitAnnotation: descriptor=$descriptor visible=$visible" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitTypeAnnotation(
    typeRef: Int,
    typePath: TypePath?,
    descriptor: String,
    visible: Boolean
  ): AnnotationVisitor {
    log { "ClassAnalyzer#visitTypeAnnotation: typeRef=$typeRef typePath=$typePath descriptor=$descriptor visible=$visible" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitEnd() {
    log { "\n" }
  }
}

private class MethodAnalyzer(
  private val logger: Logger,
  private val classes: MutableSet<ClassRef>,
  private val binaryClasses: MutableMap<String, SortedSet<MemberAccess>>,
  private val constants: MutableSet<LdcConstant>,
) : MethodVisitor(ASM_VERSION) {

  private fun addClass(className: String?, kind: ClassRef.Kind) {
    classes.addClass(className, kind)
  }

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
    }
  }

  override fun visitTypeInsn(opcode: Int, type: String?) {
    log { "- MethodAnalyzer#visitTypeInsn: $type" }

    // Type can look like `java/lang/Enum` or `[Lcom/package/Thing;`, which is fucking weird
    addClass(if (type?.startsWith("[") == true) type else "L$type;", ClassRef.Kind.NOT_ANNOTATION)
  }

  override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
    log { "- MethodAnalyzer#visitFieldInsn: $owner.$name $descriptor" }

    val field = MemberAccess.Field(
      owner = owner,
      name = name,
      descriptor = descriptor,
    )
    binaryClasses.merge(owner, sortedSetOf(field)) { acc, inc ->
      acc.apply { addAll(inc) }
    }

    addClass("L$owner;", ClassRef.Kind.NOT_ANNOTATION)
    addClass(descriptor, ClassRef.Kind.NOT_ANNOTATION)
  }

  override fun visitMethodInsn(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean
  ) {
    log { "- MethodAnalyzer#visitMethodInsn: $owner.$name $descriptor" }

    // Owner can look like `java/lang/Enum` or `[Lcom/package/Thing;`, which is fucking weird
    addClass(if (owner.startsWith("[")) owner else "L$owner;", ClassRef.Kind.NOT_ANNOTATION)
    METHOD_DESCRIPTOR_REGEX.findAll(descriptor).forEach { result ->
      addClass(result.value, ClassRef.Kind.NOT_ANNOTATION)
    }

    val method = MemberAccess.Method(
      owner = owner,
      name = name,
      descriptor = descriptor,
    )
    binaryClasses.merge(owner, sortedSetOf(method)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }

  override fun visitInvokeDynamicInsn(
    name: String?,
    descriptor: String?,
    bootstrapMethodHandle: Handle?,
    vararg bootstrapMethodArguments: Any?
  ) {
    log { "- MethodAnalyzer#visitInvokeDynamicInsn: $name $descriptor" }
    addClass(descriptor, ClassRef.Kind.NOT_ANNOTATION)
  }

  override fun visitLocalVariable(
    name: String?,
    descriptor: String?,
    signature: String?,
    start: Label?,
    end: Label?,
    index: Int
  ) {
    log { "- MethodAnalyzer#visitLocalVariable: $name $descriptor" }
    // TODO probably do this for other `visitX` methods as well
    signature?.genericTypes()?.forEach {
      addClass(it, ClassRef.Kind.NOT_ANNOTATION)
    }
    addClass(descriptor, ClassRef.Kind.NOT_ANNOTATION)
  }

  override fun visitLocalVariableAnnotation(
    typeRef: Int,
    typePath: TypePath?,
    start: Array<out Label>?,
    end: Array<out Label>?,
    index: IntArray?,
    descriptor: String,
    visible: Boolean
  ): AnnotationVisitor {
    log { "- MethodAnalyzer#visitLocalVariableAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.NOT_ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    log { "- MethodAnalyzer#visitAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitInsnAnnotation(
    typeRef: Int,
    typePath: TypePath?,
    descriptor: String,
    visible: Boolean
  ): AnnotationVisitor {
    log { "- MethodAnalyzer#visitInsnAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor {
    log { "- MethodAnalyzer#visitParameterAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitTypeAnnotation(
    typeRef: Int,
    typePath: TypePath?,
    descriptor: String,
    visible: Boolean
  ): AnnotationVisitor {
    log { "- MethodAnalyzer#visitTypeAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
    log { "- MethodAnalyzer#visitTryCatchBlock: $type" }
    addClass("L$type;", ClassRef.Kind.NOT_ANNOTATION)
  }

  override fun visitTryCatchAnnotation(
    typeRef: Int,
    typePath: TypePath?,
    descriptor: String,
    visible: Boolean
  ): AnnotationVisitor {
    log { "- MethodAnalyzer#visitTryCatchAnnotation: $descriptor" }
    addClass(descriptor, ClassRef.Kind.ANNOTATION)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }

  override fun visitLdcInsn(value: Any?) {
    log { "- MethodAnalyzer#visitLdcInsn: $value${value?.javaClass?.canonicalName?.let { " (type=$it)" }}" }
    constants.add(LdcConstant.of(value))
  }
}

private class AnnotationAnalyzer(
  private val annotationClass: String,
  private val visible: Boolean,
  private val logger: Logger,
  private val classes: MutableSet<ClassRef>,
  private val level: Int = 0,
  private val arrayName: String? = null
) : AnnotationVisitor(ASM_VERSION) {

  private var arraySize = 0
  private var isTypeAlias = false
  private val arrayElements = mutableSetOf<ClassRef>()

  private fun addClass(className: String?, enclosingAnnotation: String? = null) {
    classes.addClass(className, ClassRef.Kind.ANNOTATION, enclosingAnnotation)

    if (arrayName == "d2") {
      // Use "Kind.NOT_ANNOTATION" always so that our "is this typealias used?" algorithm works.
      arrayElements.addClass(className, ClassRef.Kind.NOT_ANNOTATION)
    }
  }

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
    }
  }

  private fun indent() = "  ".repeat(level)

  override fun visit(name: String?, value: Any?) {
    val valueString = stringValueOfArrayElement(value)
    log { "${indent()}- AnnotationAnalyzer#visit: name=$name, value=(${value?.javaClass?.simpleName}, ${valueString})" }

    if (arrayName != null) {
      arraySize++
      if (valueString == "alias") {
        isTypeAlias = true
      }
    }

    if (value is String) {
      METHOD_DESCRIPTOR_REGEX.findAll(value).forEach { result ->
        if (visible) {
          addClass(result.value, annotationClass.fqcnFromSpec)
        } else {
          addClass(result.value)
        }
      }
    } else if (value is Type) {
      if (visible) {
        addClass(value.descriptor, annotationClass.fqcnFromSpec)
      } else {
        addClass(value.descriptor)
      }
    }
  }

  override fun visitEnum(name: String?, descriptor: String?, value: String?) {
    log { "${indent()}- AnnotationAnalyzer#visitEnum: name=$name, descriptor=$descriptor, value=$value" }
    addClass(descriptor)
  }

  override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor {
    log { "${indent()}- AnnotationAnalyzer#visitAnnotation: name=$name, descriptor=$descriptor" }
    addClass(descriptor)
    return AnnotationAnalyzer(descriptor, visible, logger, classes, level + 1)
  }

  override fun visitArray(name: String?): AnnotationVisitor {
    log { "${indent()}- AnnotationAnalyzer#visitArray: name=$name" }
    return AnnotationAnalyzer(annotationClass, visible, logger, classes, level + 1, name)
  }

  override fun visitEnd() {
    if (isTypeAlias()) {
      classes.addAll(arrayElements)
    }
  }

  // The elements of the array will look like... (<type>:<value>)
  // { String:MyAlias, String:L/com/example/Aliased;, String:alias }
  private fun isTypeAlias() = arraySize == 3 && isTypeAlias
}

private class FieldAnalyzer(
  private val logger: Logger,
  private val classes: MutableSet<ClassRef>,
) : FieldVisitor(ASM_VERSION) {

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
    }
  }

  private fun addClass(className: String?) {
    classes.addClass(className, ClassRef.Kind.ANNOTATION)
  }

  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    log { "- FieldAnalyzer#visitAnnotation: $descriptor" }
    addClass(descriptor)
    return AnnotationAnalyzer(descriptor, visible, logger, classes)
  }
}

private fun MutableSet<ClassRef>.addClass(classRef: String?, kind: ClassRef.Kind, enclosingAnnotation: String? = null) {
  classRef ?: return

  JAVA_FQCN_REGEX_ASM.findAll(classRef)
    .map { it.value }
    // strip off leading "L" and trailing ";"
    .forEach { add(ClassRef(it.fqcnFromSpec, kind, enclosingAnnotation)) }
}

private val String.fqcnFromSpec get() = substring(1, length - 1)

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

  fun build(): Metadata {
    return Metadata(
      kind = kind,
      metadataVersion = metadataVersion,
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
) : ClassVisitor(ASM_VERSION) {

  internal lateinit var className: String
  internal var builder: KotlinClassHeaderBuilder? = null

  private fun log(msgProvider: () -> String) {
    if (!logDebug) {
      logger.quiet(msgProvider())
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
    log { "KotlinMetadataVisitor#visit: $name extends $superName" }
    className = name
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    log { "KotlinMetadataVisitor#visitAnnotation: descriptor=$descriptor visible=$visible" }
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
  ) : AnnotationVisitor(ASM_VERSION) {

    private fun log(msgProvider: () -> String) {
      if (!logDebug) {
        logger.quiet(msgProvider())
      }
    }

    private fun indent() = "  ".repeat(level)

    override fun visit(name: String?, value: Any?) {
      log { "${indent()}- visit: name=$name, value=(${value?.javaClass?.simpleName}, ${stringValueOfArrayElement(value)})" }

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

    override fun visitArray(name: String?): AnnotationVisitor {
      log { "${indent()}- visitArray: name=$name" }
      return KotlinAnnotationVisitor(logger, builder, level + 1, name)
    }
  }
}

private fun stringValueOfArrayElement(value: Any?): String {
  return if (value is String && value.contains("\n")) {
    "..."
  } else {
    value.toString()
  }
}
