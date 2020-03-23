package com.autonomousapps

import com.autonomousapps.fixtures.MultiModuleJavaLibraryProject
import com.autonomousapps.fixtures.ProjectDirProvider

import static com.autonomousapps.fixtures.Fixtures.DEFAULT_PACKAGE_NAME
import static com.autonomousapps.fixtures.JvmFixtures.*
import static com.autonomousapps.utils.Runner.build

final class JvmTests extends AbstractFunctionalTest {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  def "finds constants in java projects (#gradleVersion)"() {
    given:
    javaLibraryProject = new MultiModuleJavaLibraryProject([
        CONSUMER_CONSTANT_JAVA, PRODUCER_CONSTANT_JAVA
    ])

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_JAVA)
    [] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  def "finds constants in kotlin projects (#gradleVersion)"() {
    given:
    javaLibraryProject = new MultiModuleJavaLibraryProject([
        CONSUMER_CONSTANT_KOTLIN, PRODUCER_CONSTANT_KOTLIN
    ])

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_KOTLIN)
    [] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  def "correctly analyzes JVM projects for inline usage (#gradleVersion)"() {
    given:
    javaLibraryProject = new MultiModuleJavaLibraryProject([INLINE_PARENT, INLINE_CHILD])

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(INLINE_PARENT)
    ['org.jetbrains.kotlin:kotlin-stdlib-jdk8'] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  def "does not declare superclass used when it's only needed for compilation (#gradleVersion)"() {
    given:
    javaLibraryProject = new MultiModuleJavaLibraryProject([
        ABI_SUPER_LIB, ABI_CHILD_LIB, ABI_CONSUMER_LIB
    ])

    when:
    build(gradleVersion, javaLibraryProject, "buildHealth")

    then:
    def actualUsedClasses = javaLibraryProject.allUsedClassesFor(ABI_CONSUMER_LIB)
    def expectedUsedClasses = [
        "${DEFAULT_PACKAGE_NAME}.kotlin.ChildClass",
        "${DEFAULT_PACKAGE_NAME}.kotlin.ConsumerClass",
        "kotlin.Metadata"
    ]
    expectedUsedClasses == actualUsedClasses

    and:
    def actualChild = javaLibraryProject.allUsedClassesFor(ABI_CHILD_LIB)
    def expectedChild = [
        "${DEFAULT_PACKAGE_NAME}.kotlin.ChildClass",
        "${DEFAULT_PACKAGE_NAME}.kotlin.SuperClass",
        "kotlin.Metadata"
    ]
    expectedChild == actualChild

    where:
    gradleVersion << gradleVersions()
  }
}
