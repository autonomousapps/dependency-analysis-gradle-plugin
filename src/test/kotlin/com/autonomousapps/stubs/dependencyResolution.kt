@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.*
import javax.naming.OperationNotSupportedException

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

  override fun getDependenciesForVariant(variant: ResolvedVariantResult): List<DependencyResult> = 
    throw OperationNotSupportedException("stub")
  override fun getDependents(): Set<ResolvedDependencyResult> =
    throw OperationNotSupportedException("stub")
  override fun getVariants(): List<ResolvedVariantResult> =
    throw OperationNotSupportedException("stub")
  override fun getSelectionReason(): ComponentSelectionReason =
    throw OperationNotSupportedException("stub")
  override fun getVariant(): ResolvedVariantResult =
    throw OperationNotSupportedException("stub")
  override fun getModuleVersion(): ModuleVersionIdentifier? =
    throw OperationNotSupportedException("stub")

  /**
   *
   */
  class StubResolvedDependencyResult(
    private val resolvedComponentResult: ResolvedComponentResult
  ) : ResolvedDependencyResult {

    override fun getSelected(): ResolvedComponentResult {
      return resolvedComponentResult
    }

    override fun getFrom(): ResolvedComponentResult = throw OperationNotSupportedException("stub")
    override fun isConstraint(): Boolean = throw OperationNotSupportedException("stub")
    override fun getResolvedVariant(): ResolvedVariantResult =
      throw OperationNotSupportedException("stub")
    override fun getRequested(): ComponentSelector = throw OperationNotSupportedException("stub")
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

    override fun getDisplayName(): String = throw OperationNotSupportedException("stub")
    override fun getGroup(): String = throw OperationNotSupportedException("stub")
    override fun getModule(): String = throw OperationNotSupportedException("stub")
  }

  /**
   *
   */
  class StubProjectComponentIdentifier(
    private val projectPath: String
  ) : ProjectComponentIdentifier {
    override fun getProjectPath(): String = projectPath

    override fun getDisplayName(): String = throw OperationNotSupportedException("stub")
    override fun getProjectName(): String = throw OperationNotSupportedException("stub")
    override fun getBuild(): BuildIdentifier = throw OperationNotSupportedException("stub")
  }

  /**
   *
   */
  class StubModuleIdentifier(private val identifier: String) : ModuleIdentifier {
    override fun toString(): String = identifier

    override fun getGroup(): String = throw OperationNotSupportedException("stub")
    override fun getName(): String = throw OperationNotSupportedException("stub")
  }
}
