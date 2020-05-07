package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.Source as FixtureSource
import com.autonomousapps.fixtures.jvm.SourceType

import static com.autonomousapps.fixtures.jvm.Dependency.kotlinStdlibJdk7
import static com.autonomousapps.fixtures.jvm.Plugin.KOTLIN_VERSION

final class FacadeProject {

  private static final STDLIB = new Dependency('org.jetbrains.kotlin:kotlin-stdlib', KOTLIN_VERSION, null)
  private static final STDLIB7 = new Dependency('org.jetbrains.kotlin:kotlin-stdlib-jdk7', KOTLIN_VERSION, 'implementation')

  final JvmProject jvmProject

  FacadeProject(String additions = '') {
    this.jvmProject = build(additions)
  }

  static Set<Advice> expectedNoFacadeAdvice() {
    return [addCoreStdlib(), removeStdlib7()] as Set<Advice>
  }

  static Set<Advice> expectedFacadeAdvice() {
    return [] as Set<Advice>
  }

  private static Advice addCoreStdlib() {
    return Advice.add(
      new TransitiveDependency(STDLIB, [STDLIB7] as Set<Dependency>),
      'implementation'
    )
  }

  private static Advice removeStdlib7() {
    return Advice.remove(new ComponentWithTransitives(STDLIB7, [STDLIB] as Set<Dependency>))
  }

  private static JvmProject build(String additions) {
    def builder = new JvmProject.Builder()

    builder.rootAdditions = additions

    def plugins = [Plugin.kotlinPlugin(true, null)]
    def dependencies = [kotlinStdlibJdk7('implementation')]
    def sourceCode = """\
      package com.example
      
      class Library {
        fun magic() = 42
      }
    """.stripIndent()
    def source = new FixtureSource(SourceType.KOTLIN, 'Library', 'com/example', sourceCode)
    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }
}
