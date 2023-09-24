package com.autonomousapps.extension

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Enable the user to specify custom filtering and severity behavior for a specific source set, either globally or for
 * an individual project.
 *
 * This is not recommended. It would be much preferable for users to fix their dependency issues.
 *
 * ```
 * dependencyAnalysis {
 *   issues {
 *     all {
 *       sourceSet("...") {
 *         onUnusedDependencies { ... }
 *       }
 *     }
 *     project(":...") {
 *       sourceSet("...") {
 *         onUnusedDependencies { ... }
 *       }
 *     }
 *   }
 * }
 * ```
 */
abstract class SourceSetsHandler @Inject constructor(
  private val sourceSetName: String,
  projectPath: String,
  objects: ObjectFactory
) : Named {

  override fun getName(): String = sourceSetName

  internal val project = objects.newInstance(ProjectIssueHandler::class.java, projectPath)

  /**
   * Gets the specified issue out of [project], and sets [sourceSetName] on it for later filtering by
   * [FilterAdviceTask][com.autonomousapps.tasks.FilterAdviceTask].
   */
  internal fun issueOf(mapper: (ProjectIssueHandler) -> Issue): Issue {
    return mapper(project).apply {
      sourceSet.set(sourceSetName)
    }
  }

  internal class Factory(
    private val projectPath: String,
    private val objects: ObjectFactory
  ) : NamedDomainObjectFactory<SourceSetsHandler> {
    override fun create(name: String): SourceSetsHandler = objects.newInstance(
      SourceSetsHandler::class.java,
      name,
      projectPath
    )
  }
}
