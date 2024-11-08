// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.intermediates.consumer

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

/**
 * Represents, from the consumer's bytecode, the access of a member from a producer. E.g., if "our" code calls
 * `"some string".substring(1)`, that a member access on the substring method of the String class.
 *
 * nb: Borrowing heavily from `asmUtils.kt` and similar but substantially different from
 * [Member][com.autonomousapps.model.intermediates.producer.Member] on the producer side.
 *
 * TODO: ideally this would be internal. Do the Capabilities really need to be public?
 */
@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class MemberAccess(
  /** The class that owns this member, e.g., `java/lang/String`. */
  open val owner: String,
  /** The name of this member, e.g., `substring`. */
  open val name: String,
  /** The descriptor of this member, e.g., `(I)Ljava/Lang/String;` for a [Method] or `I` for a [Field]. */
  open val descriptor: String,
) : Comparable<MemberAccess> {

  override fun compareTo(other: MemberAccess): Int {
    if (this is Field && other !is Field) return -1
    if (this !is Field && other is Field) return 1

    return compareBy(MemberAccess::owner)
      .thenBy(MemberAccess::name)
      .thenBy(MemberAccess::descriptor)
      .compare(this, other)
  }

  @TypeLabel("method")
  @JsonClass(generateAdapter = false)
  data class Method(
    /** `java/lang/String` */
    override val owner: String,
    /** substring */
    override val name: String,
    /** (I)Ljava/Lang/String; */
    override val descriptor: String,
  ) : MemberAccess(
    owner = owner,
    name = name,
    descriptor = descriptor,
  )

  @TypeLabel("field")
  @JsonClass(generateAdapter = false)
  data class Field(
    /** kotlin/io/encoding/Base64 */
    override val owner: String,
    /** bytesPerGroup */
    override val name: String,
    /** I */
    override val descriptor: String,
  ) : MemberAccess(
    owner = owner,
    name = name,
    descriptor = descriptor,
  )
}
