// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.binary

import com.autonomousapps.internal.strings.dotty
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.BinaryClassCapability
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.visitor.GraphViewVisitor

/**
 * TODO(tsr): there are [reports](https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1604) that
 * this analysis is blowing up heap usage and leading to OOMs.
 */
internal class BinaryCompatibilityChecker(
  private val coordinates: Coordinates,
  private val binaryClassCapability: BinaryClassCapability,
  private val context: GraphViewVisitor.Context,
) {

  class Result(
    val relevantMemberAccesses: Set<MemberAccess>,
    val nonMatchingBinaryClasses: Set<BinaryClass>,
    val isBinaryCompatible: Boolean,
  )

  private data class PartitionResult(
    val matchingClasses: Set<BinaryClass>,
    val nonMatchingClasses: Set<BinaryClass>,
  ) {

    companion object {
      fun empty(): PartitionResult = PartitionResult(emptySet(), emptySet())
    }

    class Builder {
      val matchingClasses = sortedSetOf<BinaryClass>()
      val nonMatchingClasses = sortedSetOf<BinaryClass>()

      fun build(): PartitionResult {
        return PartitionResult(
          matchingClasses = matchingClasses.efficient(),
          nonMatchingClasses = nonMatchingClasses.efficient(),
        )
      }
    }
  }

  val result: Result? by unsafeLazy { compute() }

  private fun compute(): Result? {
    // Can't be incompatible if the code compiles in the context of no duplication
    if (context.duplicateClasses.isEmpty()) return null

    // TODO(tsr): add special handling for @Composable
    val memberAccessOwners = context.project.memberAccesses.mapToSet { it.owner }
    val relevantDuplicates = context.duplicateClasses.asSequence()
      .filter { duplicate -> coordinates in duplicate.dependencies && duplicate.className in memberAccessOwners }
      .filter { duplicate -> duplicate.classpathName == DuplicateClass.COMPILE_CLASSPATH_NAME }
      .toSortedSet()

    // Can't be incompatible if the code compiles in the context of no relevant duplication
    if (relevantDuplicates.isEmpty()) return null

    val relevantDuplicateClassNames = relevantDuplicates.mapToOrderedSet { it.className }
    val relevantMemberAccesses = context.project.memberAccesses
      .filterToOrderedSet { access -> access.owner in relevantDuplicateClassNames }

    val (matchingBinaryClasses, nonMatchingBinaryClasses) = relevantMemberAccesses.mapToSet { access ->
      binaryClassCapability.findMatchingClasses(access)
    }.reduce()

    // There must be a compatible BinaryClass.<field|method> for each MemberAccess for the usage to be binary-compatible
    val isBinaryCompatible = relevantMemberAccesses.all { access ->
      when (access) {
        is MemberAccess.Field -> {
          matchingBinaryClasses.any { bin ->
            bin.effectivelyPublicFields.any { field ->
              field.matches(access)
            }
          }
        }

        is MemberAccess.Method -> {
          matchingBinaryClasses.any { bin ->
            bin.effectivelyPublicMethods.any { method ->
              method.matches(access)
            }
          }
        }
      }
    }

    return Result(
      relevantMemberAccesses,
      nonMatchingBinaryClasses,
      isBinaryCompatible,
    )
  }

  private fun Set<PartitionResult>.reduce(): PartitionResult {
    val matches = sortedSetOf<BinaryClass>()
    val nonMatches = sortedSetOf<BinaryClass>()

    forEach { result ->
      matches.addAll(result.matchingClasses)
      nonMatches.addAll(result.nonMatchingClasses)
    }

    return PartitionResult(
      matchingClasses = matches.reduce(),
      nonMatchingClasses = nonMatches.reduce(),
    )
  }

  private fun Set<BinaryClass>.reduce(): Set<BinaryClass> {
    val builders = mutableMapOf<String, BinaryClass.Builder>()

    forEach { bin ->
      builders.merge(
        bin.className,
        BinaryClass.Builder(
          className = bin.className,
          superClassName = bin.superClassName,
          interfaces = bin.interfaces.toSortedSet(),
          effectivelyPublicFields = bin.effectivelyPublicFields.toSortedSet(),
          effectivelyPublicMethods = bin.effectivelyPublicMethods.toSortedSet(),
        )
      ) { acc, inc ->
        acc.apply {
          effectivelyPublicFields.addAll(inc.effectivelyPublicFields)
          effectivelyPublicMethods.addAll(inc.effectivelyPublicMethods)
        }
      }
    }

    return builders.values.mapToOrderedSet { it.build() }
  }

  private fun BinaryClassCapability.findMatchingClasses(memberAccess: MemberAccess): PartitionResult {
    val relevant = findRelevantBinaryClasses(memberAccess)

    // lenient
    if (relevant.isEmpty()) return PartitionResult.empty()

    return relevant
      .map { bin -> bin.partition(memberAccess) }
      .fold(PartitionResult.Builder()) { acc, (match, nonMatch) ->
        acc.apply {
          match?.let { matchingClasses.add(it) }
          nonMatch?.let { nonMatchingClasses.add(it) }
        }
      }
      .build()
  }

  /**
   * Example:
   * 1. [memberAccess] is for `groovy/lang/MetaClass#getProperty`.
   * 2. That method is actually provided by `groovy/lang/MetaObjectProtocol`, which `groovy/lang/MetaClass` implements.
   *
   * All of the above ("this" class, its super class, and its interfaces) are relevant for search purposes. Note we
   * don't inspect the member names for this check.
   */
  private fun BinaryClassCapability.findRelevantBinaryClasses(memberAccess: MemberAccess): Set<BinaryClass> {
    // direct references
    val relevant = binaryClasses.filterTo(mutableSetOf()) { bin ->
      bin.className == memberAccess.owner.dotty()
    }

    // Walk up the class hierarchy
    fun walkUp(): Int {
      binaryClasses.filterTo(relevant) { bin ->
        bin.className in relevant.map { it.superClassName }
          || bin.className in relevant.flatMap { it.interfaces }
      }
      return relevant.size
    }

    // TODO(tsr): this could be more performant
    do {
      val size = relevant.size
      val newSize = walkUp()
    } while (newSize > size)

    return relevant
  }

  /**
   * Partitions and returns artificial pair of [BinaryClasses][BinaryClass]. Non-null elements indicate relevant (to
   * [memberAccess]) matching and non-matching members of this `BinaryClass`. Matching members are binary-compatible;
   * and non-matching members have the same [name][com.autonomousapps.model.internal.intermediates.producer.Member.name]
   * but incompatible [descriptors][com.autonomousapps.model.internal.intermediates.producer.Member.descriptor], and are
   * therefore binary-incompatible.
   *
   * nb: We don't want this as a method directly in BinaryClass because it can't safely assert the prerequisite that
   * it's only called on "relevant" classes. THIS class, however, can, via [findRelevantBinaryClasses].
   */
  private fun BinaryClass.partition(memberAccess: MemberAccess): Pair<BinaryClass?, BinaryClass?> {
    // There can be only one match
    val matchingFields = effectivelyPublicFields.firstOrNull { it.matches(memberAccess) }
    val matchingMethods = effectivelyPublicMethods.firstOrNull { it.matches(memberAccess) }

    // There can be many non-matches
    val nonMatchingFields = effectivelyPublicFields.filterToOrderedSet { it.doesNotMatch(memberAccess) }
    val nonMatchingMethods = effectivelyPublicMethods.filterToOrderedSet { it.doesNotMatch(memberAccess) }

    // Create a view of the binary class containing only the matching members.
    val match = if (matchingFields != null || matchingMethods != null) {
      copy(
        effectivelyPublicFields = matchingFields?.let { setOf(it) }.orEmpty(),
        effectivelyPublicMethods = matchingMethods?.let { setOf(it) }.orEmpty()
      )
    } else {
      null
    }

    // Create a view of the binary class containing only the non-matching members.
    val nonMatch = if (nonMatchingFields.isNotEmpty() || nonMatchingMethods.isNotEmpty()) {
      copy(
        effectivelyPublicFields = nonMatchingFields,
        effectivelyPublicMethods = nonMatchingMethods,
      )
    } else {
      null
    }

    return match to nonMatch
  }
}
