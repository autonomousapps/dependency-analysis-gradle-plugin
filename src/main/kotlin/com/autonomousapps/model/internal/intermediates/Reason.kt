// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.internal.AndroidResSource
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.Member
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class Reason(open val reason: String) {

  abstract val configurationName: String

  fun reason(prefix: String = "", isCompileOnly: Boolean): String = buildString {
    val effectiveConfiguration = if (this@Reason is AnnotationProcessor || !isCompileOnly) {
      configurationName
    } else {
      "compileOnly"
    }

    append(reason)

    if (this@Reason is BinaryIncompatible) {
      // do nothing
    } else if (prefix.isEmpty()) {
      append(" (implies ${effectiveConfiguration}).")
    } else {
      append(" (implies ${prefix}${effectiveConfiguration.capitalizeSafely()}).")
    }
  }

  @TypeLabel("abi")
  @JsonClass(generateAdapter = false)
  data class Abi(override val reason: String) : Reason(reason) {
    constructor(exposedClasses: Set<String>) : this(
      buildReason(exposedClasses, "Exposes", Kind.Class)
    )

    override val configurationName: String = "api"
  }

  @TypeLabel("proc")
  @JsonClass(generateAdapter = false)
  data class AnnotationProcessor(
    override val reason: String,
    val isKapt: Boolean
  ) : Reason(reason) {

    override val configurationName: String = if (isKapt) "kapt" else "annotationProcessor"

    internal companion object {
      fun imports(imports: Set<String>, isKapt: Boolean) = AnnotationProcessor(
        buildReason(
          imports,
          "Imports",
          Kind.Annotation
        ),
        isKapt
      )

      fun classes(classes: Set<String>, isKapt: Boolean) = AnnotationProcessor(
        buildReason(
          classes,
          "Uses",
          Kind.Annotation
        ),
        isKapt
      )
    }
  }

  @TypeLabel("binaryIncompatible")
  @JsonClass(generateAdapter = false)
  data class BinaryIncompatible(override val reason: String) : Reason("Is binary-incompatible") {
    constructor(
      memberAccesses: Set<MemberAccess>,
      nonMatchingClasses: Set<BinaryClass>,
    ) : this(reasonString(memberAccesses, nonMatchingClasses))

    override fun toString(): String = reason
    override val configurationName: String = "n/a"

    private companion object {
      fun reasonString(memberAccesses: Set<MemberAccess>, nonMatchingClasses: Set<BinaryClass>): String {
        require(memberAccesses.isNotEmpty()) { "memberAccesses must not be empty" }
        require(nonMatchingClasses.isNotEmpty()) { "nonMatchingClasses must not be empty" }

        // A list of pairs, where each pair is of an access to a set of non-matches
        val nonMatches = memberAccesses
          .map { access ->
            access to when (access) {
              is MemberAccess.Field -> nonMatchingClasses.winnowedBy(access) { it.effectivelyPublicFields }
              is MemberAccess.Method -> nonMatchingClasses.winnowedBy(access) { it.effectivelyPublicMethods }
            }
          }
          // We don't want to display information for matches, that would be redundant.
          .filter { (_, nonMatches) -> nonMatches.isNotEmpty() }

        return buildString {
          appendLine("Is binary-incompatible, and should be removed from the classpath:")
          nonMatches.forEachIndexed { i, (access, incompatibleMembers) ->
            if (i > 0) appendLine()

            when (access) {
              is MemberAccess.Field -> {
                append("  Expected FIELD ${access.descriptor} ${access.owner}.${access.name}, but was ")
                append(incompatibleMembers.joinToString { "${it.className}.${it.memberName} ${it.descriptor}" })
              }

              is MemberAccess.Method -> {
                append("  Expected METHOD ${access.owner}.${access.name}${access.descriptor}, but was ")
                append(incompatibleMembers.joinToString { "${it.className}.${it.memberName}${it.descriptor}" })
              }
            }
          }
        }
      }

      private fun Set<BinaryClass>.winnowedBy(
        access: MemberAccess,
        selector: (BinaryClass) -> Set<Member>,
      ): Set<Member.Printable> {
        return asSequence()
          .map { bin -> bin.className to selector(bin) }
          .map { (className, fields) -> className to fields.filter { it.doesNotMatch(access) } }
          .filterNot { (_, fields) -> fields.isEmpty() }
          .flatMap { (className, fields) -> fields.map { it.asPrintable(className) } }
          .toSortedSet()
      }
    }
  }

  @TypeLabel("compile_time_anno")
  @JsonClass(generateAdapter = false)
  data class CompileTimeAnnotations(override val reason: String) : Reason(reason) {
    constructor() : this("Provides compile-time annotations")

    override val configurationName: String = "compileOnly"
  }

  @TypeLabel("constant")
  @JsonClass(generateAdapter = false)
  data class Constant(override val reason: String) : Reason(reason) {
    constructor(importedConstants: Set<String>) : this(
      buildReason(importedConstants, "Imports", Kind.Constant)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("impl")
  @JsonClass(generateAdapter = false)
  data class Impl(override val reason: String) : Reason(reason) {
    constructor(implClasses: Set<String>) : this(
      buildReason(implClasses, "Uses", Kind.Class)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("super_interface")
  @JsonClass(generateAdapter = false)
  data class ImplSuper(override val reason: String) : Reason(reason) {
    constructor(superClasses: Set<String>) : this(
      buildReason(superClasses, "Compiles against", Kind.SuperClass)
    )

    override val configurationName: String = "implementation"
  }

  /**
   * For example, we might detect `SomeClass` used in the context of an annotation like so:
   * ```
   * @Annotation(SomeClass::class)
   * ```
   * For runtime retention especially, we probably need to keep this class as an "implementation" dependency.
   */
  @TypeLabel("annotation")
  @JsonClass(generateAdapter = false)
  data class Annotation(override val reason: String) : Reason(reason) {
    constructor(inAnnotationClasses: Set<String>) : this(
      buildReason(inAnnotationClasses, "Uses (in an annotation)", Kind.Class)
    )

    // TODO: ugh.
    override val configurationName: String = "implementation, sometimes"
  }

  @TypeLabel("invisibleAnnotation")
  @JsonClass(generateAdapter = false)
  data class InvisibleAnnotation(override val reason: String) : Reason(reason) {
    constructor(inAnnotationClasses: Set<String>) : this(
      buildReason(inAnnotationClasses, "Uses (as an annotation)", Kind.Class)
    )

    override val configurationName: String = "compileOnly"
  }

  @TypeLabel("imported")
  @JsonClass(generateAdapter = false)
  data class Imported(override val reason: String) : Reason(reason) {
    constructor(imports: Set<String>) : this(
      buildReason(imports, "Imports", Kind.Class)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("testInstrumentationRunner")
  @JsonClass(generateAdapter = false)
  data class TestInstrumentationRunner(override val reason: String) : Reason(reason) {
    init {
      buildReason(setOf(reason), "Declares", Kind.AndroidTestInstrumentationRunner)
    }

    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("inline")
  @JsonClass(generateAdapter = false)
  data class Inline(override val reason: String) : Reason(reason) {
    constructor(imports: Set<String>) : this(
      buildReason(imports, "Imports", Kind.InlineMember)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("lint")
  @JsonClass(generateAdapter = false)
  data class LintJar(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"

    internal companion object {
      fun of(lintRegistry: String) = LintJar(
        buildReason(setOf(lintRegistry), "Provides", Kind.LintRegistry)
      )
    }
  }

  @TypeLabel("native")
  @JsonClass(generateAdapter = false)
  data class NativeLib(override val reason: String) : Reason(reason) {
    constructor(fileNames: Set<String>) : this(
      buildReason(fileNames, "Provides", Kind.NativeBinary)
    )

    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("res_by_src")
  @JsonClass(generateAdapter = false)
  data class ResBySrc(override val reason: String) : Reason(reason) {
    constructor(imports: Set<String>) : this(
      buildReason(imports, "Imports", Kind.AndroidRes)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("res_by_res")
  @JsonClass(generateAdapter = false)
  data class ResByRes(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"

    internal companion object {
      fun resRefs(resources: Set<AndroidResSource.ResRef>) = ResByRes(
        buildReason(resources.map { it.toString() }, "Uses", Kind.AndroidRes)
      )
    }
  }

  @TypeLabel("res_by_res_runtime")
  @JsonClass(generateAdapter = false)
  data class ResByResRuntime(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"

    internal companion object {
      fun resRefs(resources: Set<AndroidResSource.ResRef>) = ResByResRuntime(
        buildReason(resources.map { it.toString() }, "Uses", Kind.AndroidRes)
      )
    }
  }

  @TypeLabel("asset")
  @JsonClass(generateAdapter = false)
  data class Asset(override val reason: String) : Reason(reason) {
    constructor(assets: List<String>) : this(
      buildReason(assets, "Provides", Kind.AndroidAsset)
    )

    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("runtime_android")
  @JsonClass(generateAdapter = false)
  data class RuntimeAndroid(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"

    internal companion object {
      fun activities(activities: Set<String>) = RuntimeAndroid(
        buildReason(activities, "Provides", Kind.AndroidActivity)
      )

      fun providers(providers: Set<String>) = RuntimeAndroid(
        buildReason(providers, "Provides", Kind.AndroidProvider)
      )

      fun receivers(receivers: Set<String>) = RuntimeAndroid(
        buildReason(receivers, "Provides", Kind.AndroidReceiver)
      )

      fun services(services: Set<String>) = RuntimeAndroid(
        buildReason(services, "Provides", Kind.AndroidService)
      )
    }
  }

  @TypeLabel("security_provider")
  @JsonClass(generateAdapter = false)
  data class SecurityProvider(override val reason: String) : Reason(reason) {
    constructor(providers: Set<String>) : this(
      buildReason(providers, "Provides", Kind.SecurityProvider)
    )

    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("service_loader")
  @JsonClass(generateAdapter = false)
  data class ServiceLoader(override val reason: String) : Reason(reason) {
    constructor(providers: Set<String>) : this(
      buildReason(providers, "Provides", Kind.ServiceLoader)
    )

    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("typealias")
  @JsonClass(generateAdapter = false)
  data class Typealias(override val reason: String) : Reason(reason) {
    constructor(usedClasses: Set<String>) : this(
      buildReason(usedClasses, "Uses", Kind.Typealias)
    )

    override val configurationName: String = "implementation"
  }

  @TypeLabel("undeclared")
  @JsonClass(generateAdapter = false)
  object Undeclared : Reason("undeclared") {
    override fun toString(): String = "UNDECLARED"
    override val configurationName: String = "n/a"
  }

  @TypeLabel("unused")
  @JsonClass(generateAdapter = false)
  object Unused : Reason("unused") {
    override fun toString(): String = "UNUSED"
    override val configurationName: String = "n/a"
  }

  @TypeLabel("excluded")
  @JsonClass(generateAdapter = false)
  object Excluded : Reason("excluded") {
    override fun toString(): String = "EXCLUDED"
    override val configurationName: String = "n/a"
  }
}

private const val LIMIT = 5

private fun buildReason(
  items: Collection<String>,
  prefix: String,
  kind: Kind
) = buildString {
  require(items.isNotEmpty()) { "items must not be empty" }

  val count = items.size
  if (count == 1) {
    append("$prefix 1 ${kind.singular}: ")
  } else if (count <= LIMIT) {
    append("$prefix $count ${kind.plural}: ")
  } else {
    append("$prefix $count ${kind.plural}, $LIMIT of which are shown: ")
  }

  append(items.take(LIMIT).joinToString())
}

private enum class Kind(
  val singular: String,
  val plural: String
) {
  AndroidActivity("Android Activity", "Android Activities"),
  AndroidAsset("asset", "assets"),
  AndroidProvider("Android Provider", "Android Providers"),
  AndroidRes("resource", "resources"),
  AndroidService("Android Service", "Android Services"),
  AndroidTestInstrumentationRunner("test instrumentation runner", "test instrumentation runners"),
  Annotation("annotation", "annotations"),
  Class("class", "classes"),
  Constant("constant", "constants"),
  InlineMember("inline member", "inline members"),
  LintRegistry("lint registry", "lint registries"),
  NativeBinary("native binary", "native binaries"),
  AndroidReceiver("Android Receiver", "Android Receivers"),
  SecurityProvider("security provider", "security providers"),
  ServiceLoader("service loader", "service loaders"),
  SuperClass("super class or interface", "super classes or interfaces"),
  Typealias("typealias", "typealiases"),
}
