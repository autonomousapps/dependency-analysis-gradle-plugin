package com.autonomousapps.artifacts

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling

/**
 * A simplified API for registering a pair of [Configurations][org.gradle.api.artifacts.Configuration] to be used to
 * resolve a specific variant of a dependency. Those dependencies should be declared on [declarable], and resolved from
 * [resolvable]. This class is used when there is no or only loose coupling between the producer and consumer sides. For
 * example, when you want to depend on the `shadowJar` of another module, and that module lives anywhere in the world
 * (within the same repo or in another repo entirely), you'd use this class.
 *
 * To the contrary, when there is tight coupling between producer and consumer (such as when sharing "internal"
 * artifacts between modules of the same build), you should instead use the [Publisher] and [Resolver] classes, along
 * with your custom [ArtifactDescription].
 *
 * @see [ArtifactDescription]
 * @see [Publisher]
 * @see [Resolver]
 */
public class VariantArtifacts private constructor(
  public val declarable: NamedDomainObjectProvider<out Configuration>,
  public val resolvable: NamedDomainObjectProvider<out Configuration>,
) {

  public companion object {
    public fun of(
      project: Project,
      declarableName: String,
      attributeAction: Action<in AttributeContainer>,
      configureAction: Action<in Configuration>? = null,
    ): VariantArtifacts {
      val declarable = project.dependencyScopeConfiguration(declarableName)
      val resolvable = project.resolvableConfiguration("${declarableName}Classpath", declarable.get()) { c ->
        // User configuration.
        configureAction?.execute(c)

        // Configure this configuration's attributes.
        c.attributes(attributeAction)
      }

      return VariantArtifacts(declarable, resolvable)
    }

    /**
     * Registers a pair of [Configurations][org.gradle.api.artifacts.Configuration] to be used to resolve the shadow jar
     * artifacts of dependencies declared on the [declarableName] configuration. Use [VariantArtifacts.resolvable] for
     * resolving the declared dependencies in a task action.
     */
    public fun shadowed(
      project: Project,
      declarableName: String,
      configureAction: Action<in Configuration>? = null,
    ): VariantArtifacts {
      val attributeAction: Action<in AttributeContainer> = Action { a ->
        a.attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.SHADOWED))
      }

      return of(project, declarableName, attributeAction, configureAction)
    }
  }
}
