// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.graph.newGraphBuilder
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.source.KmpSourceKind
import com.google.common.graph.Graph
import com.squareup.moshi.JsonClass

/**
 * Provides access to KMP source sets as a graph, based on dependsOn relationships.
 *
 * An example map:
 * ```
 * {
 *   "map": {
 *     "androidMain": [
 *       "commonJvmMain"
 *     ],
 *     "commonJvmMain": [
 *       "commonMain"
 *     ],
 *     "commonJvmTest": [
 *       "commonTest"
 *     ],
 *     "commonMain": [],
 *     "commonTest": [],
 *     "jvmMain": [
 *       "commonJvmMain"
 *     ],
 *     "jvmTest": [
 *       "commonJvmTest"
 *     ]
 *   }
 * }
 * ```
 */
@JsonClass(generateAdapter = false)
internal data class SourceGraph(val map: Map<String, Set<String>>) {

  // TODO: add test
  val graph: Graph<String> by unsafeLazy {
    val builder = newGraphBuilder<String>()
    map.forEach { (source, dependsOn) ->
      dependsOn.forEach { target ->
        builder.putEdge(source, target)
      }
    }

    // Add artificial edge from commonTest -> commonMain. There isn't a literal dependsOn relationship here, but they're
    // associated compilations, which is close enough for our analysis.
    builder.putEdge(KmpSourceKind.COMMON_TEST_NAME, KmpSourceKind.COMMON_MAIN_NAME)

    builder.build()
  }
}
