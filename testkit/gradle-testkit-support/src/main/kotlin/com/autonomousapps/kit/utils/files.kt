// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Files")

package com.autonomousapps.kit.utils

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.internal.ensurePrefix
import java.io.File
import java.nio.file.Path

/**
 * Returns the path to the build directory of the root project. Uses "build" as the build directory name by default.
 */
@JvmOverloads
public fun GradleProject.rootBuildPath(buildDirName: String = "build"): Path = buildDir(
  projectName = ":",
  buildDirName = buildDirName,
)

/**
 * Returns the directory of the build directory of the root project. Uses "build" as the build directory name by
 * default.
 */
@JvmOverloads
public fun GradleProject.rootBuildDir(buildDirName: String = "build"): File = rootBuildPath(buildDirName).toFile()

/**
 * Returns the file specified, relative to the build directory of the root project. Uses "build" as the build directory
 * name by default.
 */
@Deprecated(
  "Use singleArtifact",
  replaceWith = ReplaceWith("singleArtifact(\":\", relativePath)")
)
@JvmOverloads
public fun GradleProject.resolveFromRoot(relativePath: String, buildDirName: String = "build"): File {
  return rootBuildPath(buildDirName).resolve(relativePath).toFile()
}

/** Returns the path to the build dir of the first subproject, asserting that there is only one. */
public fun GradleProject.singleSubprojectBuildPath(): Path {
  check(subprojects.size == 1) { "Expected only a single subproject" }
  return buildDir(subprojects.first())
}

/** Returns the directory of the build dir first subproject, asserting that there is only one. */
public fun GradleProject.singleSubprojectBuildFile(): File = singleSubprojectBuildPath().toFile()

/**
 * Returns the file specified, relative to the single subproject, asserting that there is only one.
 */
public fun GradleProject.resolveFromSingleSubproject(relativePath: String): File {
  return singleSubprojectBuildPath().resolve(relativePath).toFile()
}

/**
 * Returns the path to the subproject of the given name in the build, asserting that there is only one. Uses "build" as
 * the build directory name by default.
 */
public fun GradleProject.buildPathForName(path: String, buildDirName: String = "build"): Path {
  val project = if (path == ":") {
    rootProject
  } else {
    subprojects
      // normalize name/path string so users can pass in `:project` or `project` and it Just Works.
      .single { it.name.ensurePrefix() == path.ensurePrefix() }
  }

  return buildDir(project = project, buildDirName = buildDirName)
}

/**
 * Returns the directory of the build dir of the subproject of the given nam.
 */
public fun GradleProject.buildFileForName(path: String): File = buildPathForName(path).toFile()

/**
 * Returns the file specified, relative to the subproject specified by [projectName].
 */
@Deprecated(
  "Use singleArtifact",
  replaceWith = ReplaceWith("singleArtifact(projectName, relativePath)")
)
public fun GradleProject.resolveFromName(projectName: String, relativePath: String): File {
  return buildPathForName(projectName).resolve(relativePath).toFile()
}
