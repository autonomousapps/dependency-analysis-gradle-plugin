package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.utils.capitalizeSafely
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import javax.naming.OperationNotSupportedException

@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class Reason(open val reason: String) {

  abstract val configurationName: String

  open fun reason(prefix: String = ""): String = buildString {
    append(reason)
    if (prefix.isEmpty()) {
      append(" (implies ${configurationName}).")
    } else {
      append(" (implies ${prefix}${configurationName.capitalizeSafely()}).")
    }
  }

  @TypeLabel("abi")
  @JsonClass(generateAdapter = false)
  data class Abi(override val reason: String) : Reason(reason) {
    override val configurationName: String = "api"
  }

  @TypeLabel("proc")
  @JsonClass(generateAdapter = false)
  data class AnnotationProcessor(override val reason: String) : Reason(reason) {
    override val configurationName: String
      get() = throw OperationNotSupportedException("Annotation processor configuration name is indeterminate")

    override fun reason(prefix: String): String = buildString {
      append(reason)
      append(" (implies kapt or annotationProcessor.")
    }
  }

  @TypeLabel("compile_time_anno")
  @JsonClass(generateAdapter = false)
  data class CompileTimeAnnotations(override val reason: String) : Reason(reason) {
    override val configurationName: String = "compileOnly"
  }

  @TypeLabel("constant")
  @JsonClass(generateAdapter = false)
  data class Constant(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("impl")
  @JsonClass(generateAdapter = false)
  data class Impl(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("imported")
  @JsonClass(generateAdapter = false)
  data class Imported(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("inline")
  @JsonClass(generateAdapter = false)
  data class Inline(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("lint")
  @JsonClass(generateAdapter = false)
  data class LintJar(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("native")
  @JsonClass(generateAdapter = false)
  data class NativeLib(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("res_by_src")
  @JsonClass(generateAdapter = false)
  data class ResBySrc(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("res_by_res")
  @JsonClass(generateAdapter = false)
  data class ResByRes(override val reason: String) : Reason(reason) {
    override val configurationName: String = "implementation"
  }

  @TypeLabel("runtime_android")
  @JsonClass(generateAdapter = false)
  data class RuntimeAndroid(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("security_provider")
  @JsonClass(generateAdapter = false)
  data class SecurityProvider(override val reason: String) : Reason(reason) {
    override val configurationName: String = "runtimeOnly"
  }

  @TypeLabel("service_loader")
  @JsonClass(generateAdapter = false)
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
