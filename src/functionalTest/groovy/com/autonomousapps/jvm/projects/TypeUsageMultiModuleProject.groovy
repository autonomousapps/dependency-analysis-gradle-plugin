// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectTypeUsage

import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class TypeUsageMultiModuleProject extends AbstractProject {

  final GradleProject gradleProject

  TypeUsageMultiModuleProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('app') { s ->
        s.sources = appSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            project('implementation', ':core'),
            project('implementation', ':utils'),
            commonsCollections('implementation'),
            kotlinStdLib('implementation')
          ]
        }
      }
      .withSubproject('core') { s ->
        s.sources = coreSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            project('implementation', ':utils'),
            kotlinStdLib('implementation')
          ]
        }
      }
      .withSubproject('utils') { s ->
        s.sources = utilsSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            commonsIO('implementation'),
            kotlinStdLib('implementation')
          ]
        }
      }
      .write()
  }

  private appSources = [
    new Source(
      SourceType.KOTLIN, "MainActivity", "com/example/app",
      """\
        package com.example.app

        import com.example.core.UserRepository
        import com.example.core.User
        import com.example.utils.Logger
        import org.apache.commons.collections4.bag.HashBag

        class MainActivity {
          private val repository = UserRepository()
          private val logger = Logger()
          private val cache = HashBag<String>()

          fun loadUsers() {
            val users = repository.getUsers()
            logger.log("Loaded \${users.size} users")
            users.forEach { cache.add(it.name) }
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "AppHelper", "com/example/app",
      """\
        package com.example.app

        internal class AppHelper {
          fun getMainActivity() = MainActivity()
        }
      """.stripIndent()
    )
  ]

  private coreSources = [
    new Source(
      SourceType.KOTLIN, "UserRepository", "com/example/core",
      """\
        package com.example.core

        import com.example.utils.Logger

        class UserRepository {
          private val logger = Logger()

          fun getUsers(): List<User> {
            logger.log("Fetching users")
            return listOf(User("Alice"), User("Bob"))
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "User", "com/example/core",
      """\
        package com.example.core

        data class User(val name: String)
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "CoreInternal", "com/example/core",
      """\
        package com.example.core

        internal class CoreInternal {
          fun createUser(name: String) = User(name)
        }
      """.stripIndent()
    )
  ]

  private utilsSources = [
    new Source(
      SourceType.KOTLIN, "Logger", "com/example/utils",
      """\
        package com.example.utils

        import org.apache.commons.io.FileUtils
        import java.io.File

        class Logger {
          fun log(message: String) {
            FileUtils.write(File("log.txt"), message, "UTF-8", true)
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "UtilsHelper", "com/example/utils",
      """\
        package com.example.utils

        internal class UtilsHelper {
          fun createLogger() = Logger()
        }
      """.stripIndent()
    )
  ]

  ProjectTypeUsage actualTypeUsageFor(String projectPath) {
    def typeUsage = gradleProject.singleArtifact(projectPath,
      com.autonomousapps.internal.OutputPathsKt.getTypeUsagePath('main'))
    def adapter = com.autonomousapps.internal.utils.MoshiUtils.MOSHI
      .adapter(com.autonomousapps.model.ProjectTypeUsage)
    return adapter.fromJson(typeUsage.asPath.text)
  }
}
