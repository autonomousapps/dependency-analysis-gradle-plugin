package com.autonomousapps.artifacts

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/**
 * Reference implementation from the Dependency-Analysis Gradle Plugin:
 *
 * ```
 * internal interface DagpArtifacts : Named {
 *
 *   companion object {
 *     @JvmField
 *     val DAGP_ARTIFACTS_ATTRIBUTE: Attribute<DagpArtifacts> =
 *       Attribute.of("dagp.internal.artifacts", DagpArtifacts::class.java)
 *   }
 *
 *   enum class Kind : ArtifactDescription<DagpArtifacts> {
 *     COMBINED_GRAPH,
 *     PROJECT_HEALTH,
 *     RESOLVED_DEPS,
 *     ;
 *
 *     override val attribute: Attribute<DagpArtifacts> = DAGP_ARTIFACTS_ATTRIBUTE
 *
 *     override val categoryName: String = "dependency-analysis"
 *   }
 * }
 * ```
 *
 * TODO: add link once PR merged and link available.
 */
public interface ArtifactDescription<T : Named> {

  /**
   * The name for this artifact. This is used to create [configurations][org.gradle.api.artifacts.Configuration], so
   * should be human-readable.
   *
   * Implementation note: [Publisher] and [Resolver] map this to camelCase and strip non-alphanumeric characters for
   * better readability.
   */
  public val name: String

  /**
   * This attribute distinguishes this artifact from primary variants. See the reference implementation for an example
   * of how to define this.
   *
   * @see <a href="https://docs.gradle.org/current/userguide/how_to_share_outputs_between_projects.html#variant-aware-sharing">Variant-aware sharing</a>
   */
  public val attribute: Attribute<T>

  /**
   * For use in crafting a [Category][org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE] attribute, which is
   * necessary to prevent these internal variant artifacts from being substitutable for primary variants. A good example
   * of a category name might be `com.my-company.internal-artifact`. Namespacing is useful because these categories will
   * live alongside other categories that exist in the global namespace. (E.g.,
   * ["library"][org.gradle.api.attributes.Category.LIBRARY]).
   */
  public val categoryName: String
}
