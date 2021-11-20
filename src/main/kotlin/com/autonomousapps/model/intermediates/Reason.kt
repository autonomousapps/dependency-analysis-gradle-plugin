package com.autonomousapps.model.intermediates

// @Suppress("ClassName")
// internal sealed class Reason(
//   open val humanReadable: String
// ) {
//
//   data class ABI(override val humanReadable: String = "") : Reason(humanReadable)
//   data class COMPILE_TIME_ANNOTATIONS(override val humanReadable: String = "") : Reason(humanReadable)
//   data class CONSTANT(override val humanReadable: String = "") : Reason(humanReadable)
//   data class IMPL(override val humanReadable: String = "") : Reason(humanReadable)
//   data class IMPORTED(override val humanReadable: String = "") : Reason(humanReadable)
//   data class INLINE(override val humanReadable: String = "") : Reason(humanReadable)
//   data class LINT_JAR(override val humanReadable: String = "") : Reason(humanReadable)
//   data class NATIVE_LIB(override val humanReadable: String = "") : Reason(humanReadable)
//   data class RES_BY_SRC(override val humanReadable: String = "") : Reason(humanReadable)
//   data class RES_BY_RES(override val humanReadable: String = "") : Reason(humanReadable)
//   data class RUNTIME_ANDROID(override val humanReadable: String = "") : Reason(humanReadable)
//   data class SECURITY_PROVIDER(override val humanReadable: String = "") : Reason(humanReadable)
//   data class SERVICE_LOADER(override val humanReadable: String = "") : Reason(humanReadable)
//   data class UNUSED(override val humanReadable: String = "") : Reason(humanReadable)
// }

internal enum class Reason {
  ABI,
  ANNOTATION_PROCESSOR,
  COMPILE_TIME_ANNOTATIONS,
  CONSTANT,
  IMPL,
  IMPORTED,
  INLINE,
  LINT_JAR,
  NATIVE_LIB,
  RES_BY_SRC,
  RES_BY_RES,
  RUNTIME_ANDROID,
  SECURITY_PROVIDER,
  SERVICE_LOADER,
  UNUSED,
}
