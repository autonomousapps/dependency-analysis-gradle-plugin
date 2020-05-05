package com.autonomousapps.jvm
/**
 * A JVM project consists of:
 * <ol>
 *   <li>A root project, with:</li>
 *   <ol>
 *     <li>Setting script</li>
 *     <li>Build script</li>
 *     <li>(Optionally) source code</li>
 *   </ol>
 *   <li>Zero or more subprojects</li>
 * </ol>
 */
final class JvmProject {

  final File rootDir
  final RootProject rootProject
  final List<Subproject> subprojects

  JvmProject(File rootDir, RootProject rootProject, List<Subproject> subprojects = []) {
    this.rootDir = rootDir
    this.rootProject = rootProject
    this.subprojects = subprojects
  }
}
