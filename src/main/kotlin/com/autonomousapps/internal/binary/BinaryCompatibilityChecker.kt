// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.binary

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.BinaryClassCapability
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.visitor.GraphViewVisitor
import kotlin.collections.forEach

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

    val partitionResult = relevantMemberAccesses.mapToSet { access ->
      binaryClassCapability.findMatchingClasses(access)
    }.reduce()
    val matchingBinaryClasses = partitionResult.matchingClasses
    val nonMatchingBinaryClasses = partitionResult.nonMatchingClasses

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

  private fun Set<BinaryClassCapability.PartitionResult>.reduce(): BinaryClassCapability.PartitionResult {
    val matches = sortedSetOf<BinaryClass>()
    val nonMatches = sortedSetOf<BinaryClass>()

    forEach { result ->
      matches.addAll(result.matchingClasses)
      nonMatches.addAll(result.nonMatchingClasses)
    }

    return BinaryClassCapability.PartitionResult(
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
}
