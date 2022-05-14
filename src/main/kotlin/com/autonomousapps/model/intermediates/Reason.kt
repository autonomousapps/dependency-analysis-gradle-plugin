package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.utils.capitalizeSafely
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import javax.naming.OperationNotSupportedException

@JsonClass(generateAdapter = true, generator = "sealed:type")
internal sealed class Reason(open val reason: String) {

  abstract val configurationName: String

  open fun reason(prefix: String = "", isCompileOnly: Boolean): String = buildString {
    val theConfiguration = if (isCompileOnly) "compileOnly" else configurationName

    append(reason)

    if (prefix.isEmpty()) {
      append(" (implies ${theConfiguration}).")
    } else {
      append(" (implies ${prefix}${theConfiguration.capitalizeSafely()}).")
    }
  }

  @TypeLabel("abi")
  @JsonClass(generateAdapter = true)
  data class Abi(override val reason: String) : Reason(reason) {
    override val configurationName: String = "api"
  }

  @TypeLabel("proc")
  @JsonClass(generateAdapter = true)
  data class AnnotationProcessor(override val reason: String) : Reason(reason) {
    override val configurationName: String
      get() = throw OperationNotSupportedException("Annotation processor configuration name is indeterminate")

    override fun reason(prefix: String, isCompileOnly: Boolean): String = buildString {
      append(reason)
      append(" (implies kapt or annotationProcessor.")
    }
  }

  @TypeLabel("compile_time_anno")
  @JsonClass(generateAdapter = true)
  data class CompileTimeAnnotations(override val reason: String) : Reason(reason) {
    override val configurationName: String = "compileOnly"
  }

  @TypeLabel("constant")
  @JsonClass(generateAdapter = true)
  data class Constant(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("impl")
  @JsonClass(generateAdapter = true)
  data class Impl(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("imported")
  @JsonClass(generateAdapter = true)
  data class Imported(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("inline")
  @JsonClass(generateAdapter = true)
  data class Inline(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("lint")
  @JsonClass(generateAdapter = true)
  data class LintJar(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("native")
  @JsonClass(generateAdapter = true)
  data class NativeLib(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("res_by_src")
  @JsonClass(generateAdapter = true)
  data class ResBySrc(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("res_by_res")
  @JsonClass(generateAdapter = true)
  data class ResByRes(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("asset")
  @JsonClass(generateAdapter = true)
  data class Asset(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("runtime_android")
  @JsonClass(generateAdapter = true)
  data class RuntimeAndroid(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("security_provider")
  @JsonClass(generateAdapter = true)
  data class SecurityProvider(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("service_loader")
  @JsonClass(generateAdapter = true)
  data class ServiceLoader(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
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
}
