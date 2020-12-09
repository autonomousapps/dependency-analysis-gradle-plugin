package com.autonomousapps.advice

data class Ripple(
  /** From the dependency project */
  val upstreamSource: UpstreamSource,
  /** On the dependent project */
  val downstreamImpact: DownstreamImpact
)

data class UpstreamSource(
  /** The path of the project that may produce downstream impacts */
  val projectPath: String,
  /** The dependency implicated in the impact */
  val providedDependency: Dependency,
  /** The configuration that [providedDependency] is currently declared on in [projectPath] */
  val fromConfiguration: String?,
  /** The configuration on which the user should declare [providedDependency] on [projectPath] */
  val toConfiguration: String?
)

data class DownstreamImpact(
  /** The path of the project that may produce downstream impacts */
  val sourceProjectPath: String,
  /** The path of the project that may be impacted by upstream changes */
  val impactProjectPath: String,
  /** The dependency implicated in the impact */
  val providedDependency: Dependency,
  /** The configuration on which the user should declare [providedDependency] on [impactProjectPath] */
  val toConfiguration: String?
)
