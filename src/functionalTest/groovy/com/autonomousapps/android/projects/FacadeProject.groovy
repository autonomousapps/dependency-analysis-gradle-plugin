package com.autonomousapps.android.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.kotlinStdlibJdk7

final class FacadeProject {

  private static final STDLIB = new Dependency('org.jetbrains.kotlin:kotlin-stdlib', Plugin.KOTLIN_VERSION, null)
  private static final STDLIB7 = new Dependency('org.jetbrains.kotlin:kotlin-stdlib-jdk7', Plugin.KOTLIN_VERSION, 'implementation')

  final GradleProject gradleProject

  FacadeProject(String additions = '') {
    this.gradleProject = build(additions)
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedNoFacadeAdvice() {
    return [addCoreStdlib(), removeStdlib7()] as Set<Advice>
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedFacadeAdvice() {
    return [] as Set<Advice>
  }

  private static Advice addCoreStdlib() {
    return Advice.ofAdd(
      new TransitiveDependency(STDLIB, [STDLIB7] as Set<Dependency>),
      'implementation'
    )
  }

  private static Advice removeStdlib7() {
    return Advice.ofRemove(new ComponentWithTransitives(STDLIB7, [STDLIB] as Set<Dependency>))
  }

  private static GradleProject build(String additions) {
    def builder = new GradleProject.Builder()

    builder.rootAdditions = additions

    def plugins = [Plugin.kotlinPlugin(true, null)]
    def dependencies = [kotlinStdlibJdk7('implementation')]
    def sourceCode = """\
      package com.example
      
      class Library {
        fun magic() = 42
      }
    """.stripIndent()
    def source = new Source(SourceType.KOTLIN, 'Library', 'com/example', sourceCode)
    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }
}
