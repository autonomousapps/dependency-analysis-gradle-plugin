package com.autonomousapps.internal.utils

internal fun Sequence<MatchResult>.allItems(): List<String> =
  flatMap { matchResult ->
    val groupValues = matchResult.groupValues
    // Ignore the 0th element, as it is the entire match
    if (groupValues.isNotEmpty()) groupValues.subList(1, groupValues.size).asSequence()
    else emptySequence()
  }.toList()
