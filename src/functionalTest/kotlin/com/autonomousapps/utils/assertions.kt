package com.autonomousapps.utils

import kotlin.test.assertNotNull

/**
 * Asserts that every element of [this] list has a corresponding member in [actual]. Checks that _some_ element of
 * `actual` ends with _each_ element of `this`.
 *
 * Will throw an [AssertionError] if the size of `actual` is less than the size of `this`.
 */
infix fun List<String>.shouldBeIn(actual: List<String>) {
    val expected = this
    if (actual.size < expected.size) {
        throw AssertionError("Actual list smaller than expected list. Was $actual")
    }

    for (element in expected) {
        assertNotNull(
            actual.find { it.endsWith(element) },
            "$actual does not contain an element like $element"
        )
    }
}

/**
 * A convenience function for when we have a list of only one element.
 */
infix fun String.shouldBeIn(actual: List<String>) {
    listOf(this) shouldBeIn actual
}