package com.autonomousapps

import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.extension.ProjectIssueHandler
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

open class DependencyAnalysisSubExtension(
  objects: ObjectFactory,
  private val rootExtProvider: () -> DependencyAnalysisExtension,
  private val path: String
) : AbstractExtension(objects) {

  override val issueHandler: IssueHandler by lazy {
    rootExtProvider().issueHandler
  }

  fun issues(action: Action<ProjectIssueHandler>) {
    issueHandler.project(path, action)
  }
}
