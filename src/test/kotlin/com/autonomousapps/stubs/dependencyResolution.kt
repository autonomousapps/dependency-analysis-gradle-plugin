@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.*

/**
 *
 */
class StubResolvedComponentResult(
    private val dependencies: Set<ResolvedDependencyResult>,
    private val componentIdentifier: ComponentIdentifier
) : ResolvedComponentResult {

  override fun getDependencies(): Set<DependencyResult> {
    return dependencies
  }

  override fun getId(): ComponentIdentifier {
    return componentIdentifier
  }

  override fun getDependenciesForVariant(variant: ResolvedVariantResult): List<DependencyResult> = TODO("stub")
  override fun getDependents(): Set<ResolvedDependencyResult> = TODO("stub")
  override fun getVariants(): List<ResolvedVariantResult> = TODO("stub")
  override fun getSelectionReason(): ComponentSelectionReason = TODO("stub")
  override fun getVariant(): ResolvedVariantResult = TODO("stub")
  override fun getModuleVersion(): ModuleVersionIdentifier? = TODO("stub")

  /**
   *
   */
  class StubResolvedDependencyResult(private val resolvedComponentResult: ResolvedComponentResult) : ResolvedDependencyResult {

    override fun getSelected(): ResolvedComponentResult {
      return resolvedComponentResult
    }

    override fun getFrom(): ResolvedComponentResult = TODO("stub")
    override fun isConstraint(): Boolean = TODO("stub")
    override fun getResolvedVariant(): ResolvedVariantResult = TODO("stub")
    override fun getRequested(): ComponentSelector = TODO("stub")
  }

  /**
   *
   */
  class StubModuleComponentIdentifier(
      private val moduleIdentifier: ModuleIdentifier,
      private val version: String
  ) : ModuleComponentIdentifier {
    override fun getModuleIdentifier(): ModuleIdentifier = moduleIdentifier
    override fun getVersion(): String = version

    override fun getDisplayName(): String = TODO("stub")
    override fun getGroup(): String = TODO("stub")
    override fun getModule(): String = TODO("stub")
  }

  /**
   *
   */
  class StubProjectComponentIdentifier(private val projectPath: String) : ProjectComponentIdentifier {
    override fun getProjectPath(): String = projectPath

    override fun getDisplayName(): String = TODO("stub")
    override fun getProjectName(): String = TODO("stub")
    override fun getBuild(): BuildIdentifier = TODO("stub")
  }

  /**
   *
   */
  class StubModuleIdentifier(private val identifier: String) : ModuleIdentifier {
    override fun toString(): String = identifier

    override fun getGroup(): String = TODO("stub")
    override fun getName(): String = TODO("stub")
  }
}
