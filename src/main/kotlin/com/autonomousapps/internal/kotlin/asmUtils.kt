/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Copied from https://github.com/JetBrains/kotlin/tree/master/libraries/tools/binary-compatibility-validator
 */

package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.ClassNames.canonicalize
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.asm.tree.*
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMemberSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata

internal val ACCESS_NAMES = mapOf(
  Opcodes.ACC_PUBLIC to "public",
  Opcodes.ACC_PROTECTED to "protected",
  Opcodes.ACC_PRIVATE to "private",
  Opcodes.ACC_STATIC to "static",
  Opcodes.ACC_FINAL to "final",
  Opcodes.ACC_ABSTRACT to "abstract",
  Opcodes.ACC_SYNTHETIC to "synthetic",
  Opcodes.ACC_INTERFACE to "interface",
  Opcodes.ACC_ANNOTATION to "annotation"
)

internal data class ClassBinarySignature(
  val name: String,
  val superName: String,
  val outerName: String?,
  val supertypes: List<String>,
  val genericTypes: Set<String>,
  val memberSignatures: List<MemberBinarySignature>,
  val access: AccessFlags,
  val isEffectivelyPublic: Boolean,
  val isNotUsedWhenEmpty: Boolean,
  val annotations: Set<String>,
  val invisibleAnnotations: Set<String>,
  val sourceFile: String?,
) {
  val canonicalName = name.replace('/', '.')
  val signature: String
    get() = buildString {
      // Putting the generics on top is pretty ugly, but this is not meant to be a machine-parseable representation.
      if (genericTypes.isNotEmpty()) appendReproducibleNewLine("<${genericTypes.joinToString()}>")

      append("${access.getModifierString()} class $name")
      if (supertypes.isNotEmpty()) append(" : ${supertypes.joinToString()}")
    }
}

internal interface MemberBinarySignature {
  val jvmMember: JvmMemberSignature
  val name: String get() = jvmMember.name
  val desc: String get() = jvmMember.descriptor
  val genericTypes: Set<String>
  val annotations: Set<String>
  val invisibleAnnotations: Set<String>
  val access: AccessFlags
  val isPublishedApi: Boolean

  fun isEffectivelyPublic(classAccess: AccessFlags, packageIsExported: Boolean, classVisibility: ClassVisibility?) =
    packageIsExported && access.isPublic && !(access.isProtected && classAccess.isFinal)
      && (findMemberVisibility(classVisibility)?.isPublic(isPublishedApi) ?: true)

  fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
    return classVisibility?.findMember(jvmMember)
  }

  val signature: String
}

internal data class MethodBinarySignature(
  override val jvmMember: JvmMethodSignature,
  override val genericTypes: Set<String>,
  override val annotations: Set<String>,
  override val invisibleAnnotations: Set<String>,
  override val isPublishedApi: Boolean,
  override val access: AccessFlags,
  val parameterAnnotations: List<String>,
  val typeAnnotations: List<String>,
  /** Not expressed as type descriptors, instead just `com/example/Foo`. */
  val exceptions: List<String>,
) : MemberBinarySignature {

  override val signature: String
    get() = "${access.getModifierString()} fun $name $desc$throws"

  private val throws = if (exceptions.isEmpty()) "" else exceptions.joinToString(prefix = " throws ") {
    // The ABI dump uses descriptor strings
    "L$it;"
  }

  override fun isEffectivelyPublic(
    classAccess: AccessFlags,
    packageIsExported: Boolean,
    classVisibility: ClassVisibility?,
  ) =
    super.isEffectivelyPublic(classAccess, packageIsExported, classVisibility)
      && !isAccessOrAnnotationsMethod()
      && !isDummyDefaultConstructor()

  override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
    return super.findMemberVisibility(classVisibility)
      ?: classVisibility?.let { alternateDefaultSignature(it.name)?.let(it::findMember) }
  }

  private fun isAccessOrAnnotationsMethod() = access.isSynthetic && (name.startsWith("access\$") || name.endsWith(
    "\$annotations"
  ))

  private fun isDummyDefaultConstructor() = access.isSynthetic && name == "<init>" && desc == "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"

  /**
   * Calculates the signature of this method without default parameters
   *
   * Returns `null` if this method isn't an entry point of a function
   * or a constructor with default parameters.
   * Returns an incorrect result, if there are more than 31 default parameters.
   */
  private fun alternateDefaultSignature(className: String): JvmMethodSignature? {
    return when {
      !access.isSynthetic -> null
      name == "<init>" && "ILkotlin/jvm/internal/DefaultConstructorMarker;" in desc ->
        JvmMethodSignature(name, desc.replace("ILkotlin/jvm/internal/DefaultConstructorMarker;", ""))

      name.endsWith("\$default") && "ILjava/lang/Object;)" in desc ->
        JvmMethodSignature(
          name.removeSuffix("\$default"),
          desc.replace("ILjava/lang/Object;)", ")").replace("(L$className;", "(")
        )

      else -> null
    }
  }
}

internal data class FieldBinarySignature(
  override val jvmMember: JvmFieldSignature,
  override val genericTypes: Set<String>,
  override val annotations: Set<String>,
  override val invisibleAnnotations: Set<String>,
  override val isPublishedApi: Boolean,
  override val access: AccessFlags,
) : MemberBinarySignature {
  override val signature: String
    get() = "${access.getModifierString()} field $name $desc"

  override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
    return super.findMemberVisibility(classVisibility)
      ?: takeIf { access.isStatic }?.let { super.findMemberVisibility(classVisibility?.companionVisibilities) }
  }
}

private val MemberBinarySignature.kind: Int
  get() = when (this) {
    is FieldBinarySignature -> 1
    is MethodBinarySignature -> 2
    else -> error("Unsupported $this")
  }

internal val MEMBER_SORT_ORDER = compareBy<MemberBinarySignature>(
  { it.kind },
  { it.name },
  { it.desc }
)

internal data class AccessFlags(val access: Int) {
  val isPublic: Boolean get() = isPublic(access)
  val isProtected: Boolean get() = isProtected(access)
  val isStatic: Boolean get() = isStatic(access)
  val isFinal: Boolean get() = isFinal(access)
  val isSynthetic: Boolean get() = isSynthetic(access)

  fun getModifiers(): List<String> = ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }
  fun getModifierString(): String = getModifiers().joinToString(" ")
}

internal fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
internal fun isProtected(access: Int) = access and Opcodes.ACC_PROTECTED != 0
internal fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0
internal fun isFinal(access: Int) = access and Opcodes.ACC_FINAL != 0
internal fun isSynthetic(access: Int) = access and Opcodes.ACC_SYNTHETIC != 0

internal fun ClassNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
  isPublic(access)
    && !isLocal()
    && !isWhenMappings()
    && (classVisibility?.isPublic(isPublishedApi()) ?: true)

internal val ClassNode.innerClassNode: InnerClassNode? get() = innerClasses.singleOrNull { it.name == name }
internal fun ClassNode.isLocal() = innerClassNode?.run { innerName == null && outerName == null } ?: false
internal fun ClassNode.isInner() = innerClassNode != null
internal fun ClassNode.isWhenMappings() = isSynthetic(access) && name.endsWith("\$WhenMappings")

internal val ClassNode.effectiveAccess: Int get() = innerClassNode?.access ?: access
internal val ClassNode.outerClassName: String? get() = innerClassNode?.outerName

internal fun ClassNode.packageName() = name.split("/").let { it.subList(0, it.size - 1) }.joinToString(".")

internal fun ModuleNode.exportedPackages() = exports?.map { canonicalize(it.packaze) }

internal const val publishedApiAnnotationName = "kotlin/PublishedApi"
internal fun ClassNode.isPublishedApi() = findAnnotation(publishedApiAnnotationName, includeInvisible = true) != null
internal fun MethodNode.isPublishedApi() = findAnnotation(publishedApiAnnotationName, includeInvisible = true) != null
internal fun FieldNode.isPublishedApi() = findAnnotation(publishedApiAnnotationName, includeInvisible = true) != null

internal fun ClassNode.isDefaultImpls(metadata: KotlinClassMetadata?) = isInner() && name.endsWith(
  "\$DefaultImpls"
) && metadata.isSyntheticClass()

internal fun ClassNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(
  annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible
)

internal fun MethodNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(
  annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible
)

internal fun FieldNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(
  annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible
)

internal operator fun AnnotationNode.get(key: String): Any? = values?.annotationValue(key)

private fun List<Any>.annotationValue(key: String): Any? {
  for (index in (0 until size / 2)) {
    if (this[index * 2] == key)
      return this[index * 2 + 1]
  }
  return null
}

private fun findAnnotation(
  annotationName: String,
  visibleAnnotations: List<AnnotationNode>?,
  invisibleAnnotations: List<AnnotationNode>?,
  includeInvisible: Boolean,
): AnnotationNode? =
  visibleAnnotations?.firstOrNull { it.refersToName(annotationName) }
    ?: if (includeInvisible) invisibleAnnotations?.firstOrNull { it.refersToName(annotationName) } else null

internal fun AnnotationNode.refersToName(name: String) = desc.startsWith('L') && desc.endsWith(
  ';'
) && desc.regionMatches(1, name, 0, name.length)
