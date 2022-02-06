package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.fixtures.*

import static com.autonomousapps.fixtures.JvmFixtures.*
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class JvmSpec extends AbstractFunctionalSpec {

  private ProjectDirProvider javaLibraryProject = null

  @SuppressWarnings('unused')
  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  def "reports redundant kotlin-jvm and kapt plugins applied (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantKotlinJvmAndKaptPluginsProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    def expectedAdvice = RedundantKotlinJvmAndKaptPluginsProject.expectedAdvice().first().pluginAdvice
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "reports redundant kotlin-jvm plugin applied (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantKotlinJvmPluginProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    assertThat(actualAdvice)
      .containsExactlyElementsIn(RedundantKotlinJvmPluginProject.expectedAdvice().first().pluginAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "does not report kotlin-jvm as redundant (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantKotlinJvmPluginProject(true)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    assertThat(actualAdvice).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  def "autoservice is used with annotationProcessor (#gradleVersion)"() {
    given:
    javaLibraryProject = new JvmAutoServiceProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    assertThat(actualAdvice).containsExactlyElementsIn(JvmAutoServiceProject.expectedAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "dagger is unused with annotationProcessor (#gradleVersion)"() {
    given:
    javaLibraryProject = new JvmDaggerProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    assertThat(actualAdvice).containsExactlyElementsIn(JvmDaggerProject.expectedAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "root projects can contain source (#gradleVersion)"() {
    given:
    javaLibraryProject = new SingleProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    assertThat(actualAdvice).containsExactlyElementsIn(SingleProject.expectedAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "configuration fails with sane error message if plugin was not applied to root (#gradleVersion)"() {
    given:
    def libSpecs = [JAVA_ERROR]
    def rootSpec = new RootSpec(
      libSpecs, "", RootSpec.defaultGradleProperties(), null,
      RootSpec.defaultSettingsScript(null, libSpecs),
      noPluginBuildScript(libSpecs)
    )
    javaLibraryProject = new MultiModuleJavaLibraryProject(rootSpec, libSpecs)

    expect:
    def result = buildAndFail(gradleVersion, javaLibraryProject, 'help')
    result.output.contains('You must apply the plugin to the root project. Current project is :error')

    where:
    gradleVersion << gradleVersions()
  }

  def "finds constants in java projects (#gradleVersion)"() {
    given:
    def libSpecs = [CONSUMER_CONSTANT_JAVA, PRODUCER_CONSTANT_JAVA]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    assertThat(javaLibraryProject.removeAdviceFor(CONSUMER_CONSTANT_JAVA)).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  def "finds constants in kotlin projects (#gradleVersion)"() {
    given:
    def libSpecs = [CONSUMER_CONSTANT_KOTLIN, PRODUCER_CONSTANT_KOTLIN]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    assertThat(javaLibraryProject.removeAdviceFor(CONSUMER_CONSTANT_KOTLIN)).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  def "correctly analyzes JVM projects for inline usage (#gradleVersion)"() {
    given:
    def libSpecs = [INLINE_PARENT, INLINE_CHILD]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    assertThat(javaLibraryProject.removeAdviceFor(INLINE_PARENT)).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  private static String noPluginBuildScript(List<LibrarySpec> librarySpecs) {
    """
      buildscript {
        repositories {
          google()
          mavenCentral()
        }
        dependencies {
          ${RootSpec.kotlinGradlePlugin(librarySpecs)}
        }
      }
      subprojects {
        repositories {
          google()
          mavenCentral()
        }
      }
    """
  }
}
