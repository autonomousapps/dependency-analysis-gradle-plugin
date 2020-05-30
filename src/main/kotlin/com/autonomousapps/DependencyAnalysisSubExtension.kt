package com.autonomousapps

import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.extension.ProjectIssueHandler
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

open class DependencyAnalysisSubExtension(
  objects: ObjectFactory,
  rootExtension: DependencyAnalysisExtension,
  private val path: String
) : AbstractExtension(objects) {

  override val issueHandler: IssueHandler = rootExtension.issueHandler

  fun issues(action: Action<ProjectIssueHandler>) {
    issueHandler.project(path, action)
  }
}
