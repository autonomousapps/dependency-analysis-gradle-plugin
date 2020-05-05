package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.jvm.*
import org.gradle.util.GradleVersion

import static com.autonomousapps.jvm.Repository.DEFAULT_REPOS
import static com.autonomousapps.utils.Runner.build

final class FacadeFilterSpec extends AbstractFunctionalSpec {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
//      clean(javaLibraryProject)
    }
  }

  def "test"() {
    given: 'a project'
    def jvmProject = newJvmProject()
    def writer = new JvmProjectWriter(jvmProject)
    writer.write()

    when:
    build(GradleVersion.version('6.3'), jvmProject.rootDir, 'buildHealth')

    then:
    true
  }

  private static final String KOTLIN_VERSION = '1.3.72'

  private static JvmProject newJvmProject() {
    def rootDir = new File("build/functionalTest/${UUID.randomUUID()}")

    def subprojects = newSubprojects()
    def settingScript = new SettingScript(subprojects)

    def rootPlugins = [
      new Plugin(
        'com.autonomousapps.dependency-analysis',
        System.getProperty("com.autonomousapps.pluginversion")
      ),
      new Plugin('org.jetbrains.kotlin.jvm', KOTLIN_VERSION, false)
    ]

    def extras = """\
      dependencyAnalysis {
        setFacadeGroups()
      }
    """.stripIndent()
    def rootBuildScript = new BuildScript(rootPlugins, DEFAULT_REPOS, [], extras)
    def rootProject = new RootProject(settingScript, rootBuildScript)
    return new JvmProject(rootDir, rootProject, subprojects)
  }

  private static List<Subproject> newSubprojects() {
    def plugins = [new Plugin('org.jetbrains.kotlin.jvm')]
    def dependency = new Dependency(
      'implementation',
      "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION"
    )

    def buildScript = new BuildScript(plugins, DEFAULT_REPOS, [dependency])
    def sourceCode = """\
      package com.example
      
      class Library {
        fun magic() = 42
      }
    """.stripIndent()
    def source = new Source(SourceType.KOTLIN, 'Library', 'com/example', sourceCode)
    def subproject = new Subproject('proj-a', buildScript, [source])

    return [subproject]
  }
}
