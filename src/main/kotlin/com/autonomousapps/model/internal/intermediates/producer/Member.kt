// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.kotlin.AccessFlags
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

/**
 * Represents a member of a [class][BinaryClass].
 *
 * nb: Borrowing heavily from `asmUtils.kt` and similar but substantially different from
 * [MemberAccess][com.autonomousapps.model.internal.intermediates.consumer.MemberAccess] on the consumer side.
 */
@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class Member(
  open val access: Int,
  open val name: String,
  open val descriptor: String,
  open val special: String?,
) : Comparable<Member> {

  companion object {
    const val SPECIAL_COMPOSABLE = "@Composable"
  }

  internal class Printable(
    val className: String,
    val memberName: String,
    val descriptor: String,
  ) : Comparable<Printable> {
    override fun compareTo(other: Printable): Int {
      return compareBy(Printable::className)
        .thenBy(Printable::memberName)
        .thenBy(Printable::descriptor)
        .compare(this, other)
    }
  }

  internal fun asPrintable(className: String): Printable {
    return Printable(
      className = className,
      memberName = name,
      descriptor = descriptor,
    )
  }

  override fun compareTo(other: Member): Int {
    if (this is Field && other !is Field) return -1
    if (this !is Field && other is Field) return 1

    return compareBy(Member::name)
      .thenBy(Member::descriptor)
      .thenBy(Member::accessComparator)
      .compare(this, other)
  }

  /**
   * Returns true for matching name and descriptor, unless [special] == [SPECIAL_COMPOSABLE], in which case we relax the
   * requirement that the descriptors match. `@Composable` functions get special attention from a Kotlin compiler
   * plugin, such that the source and bytecode do not match.
   */
  fun matches(memberAccess: MemberAccess): Boolean {
    return name == memberAccess.name
      && (descriptor == memberAccess.descriptor || special == SPECIAL_COMPOSABLE)
  }

  /**
   * Returns true for matching name and non-matching descriptor, unless [special] == [SPECIAL_COMPOSABLE], in which case
   * we can't say that [memberAccess] doesn't match.
   */
  fun doesNotMatch(memberAccess: MemberAccess): Boolean {
    return name == memberAccess.name
      && (descriptor != memberAccess.descriptor && special != SPECIAL_COMPOSABLE)
  }

  protected val accessFlags get() = AccessFlags(access)

  private val accessComparator: Int = when {
    accessFlags.isPublic -> 3
    accessFlags.isProtected -> 2
    accessFlags.isPrivate -> 1
    else -> 0 // package-private
  }

  abstract val signature: String

  @TypeLabel("method")
  @JsonClass(generateAdapter = false)
  data class Method(
    override val access: Int,
    override val name: String,
    override val descriptor: String,
    override val special: String? = null,
  ) : Member(
    access = access,
    name = name,
    descriptor = descriptor,
    special = special,
  ) {
    override val signature: String
      get() = "${accessFlags.getModifierString()} fun $name $descriptor"
  }

  @TypeLabel("field")
  @JsonClass(generateAdapter = false)
  data class Field(
    override val access: Int,
    override val name: String,
    override val descriptor: String,
    override val special: String? = null,
  ) : Member(
    access = access,
    name = name,
    descriptor = descriptor,
    special = special,
  ) {
    override val signature: String
      get() = "${accessFlags.getModifierString()} field $name $descriptor"
  }
}
