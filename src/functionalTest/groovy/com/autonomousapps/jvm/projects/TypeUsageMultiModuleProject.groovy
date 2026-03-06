// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectTypeUsage
import com.autonomousapps.model.TypeUsageSummary

import static com.autonomousapps.internal.OutputPathsKt.getTypeUsagePath
import static com.autonomousapps.internal.utils.MoshiUtils.MOSHI
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
    def typeUsage = gradleProject.singleArtifact(projectPath, getTypeUsagePath('main'))
    def adapter = MOSHI.adapter(ProjectTypeUsage)
    return adapter.fromJson(typeUsage.asPath.text)
  }

  ProjectTypeUsage expectedAppTypeUsage() {
    return new ProjectTypeUsage(
      ':app',
      new TypeUsageSummary(
        /* totalTypes */ 8,
        /* totalFiles */ 2,
        /* internalTypes */ 1,
        /* projectDependencies */ 2,
        /* libraryDependencies */ 3
      ),
      ['com.example.app.MainActivity': 1],
      [
        ':core': ['com.example.core.User': 1, 'com.example.core.UserRepository': 1],
        ':utils': ['com.example.utils.Logger': 1],
      ],
      [
        'org.apache.commons:commons-collections4': ['org.apache.commons.collections4.bag.HashBag': 1],
        'org.jetbrains.kotlin:kotlin-stdlib': ['kotlin.Metadata': 2, 'kotlin.jvm.internal.SourceDebugExtension': 1],
        'org.jetbrains:annotations': ['org.jetbrains.annotations.NotNull': 2],
      ],
      [:]
    )
  }

  ProjectTypeUsage expectedCoreTypeUsage() {
    return new ProjectTypeUsage(
      ':core',
      new TypeUsageSummary(
        /* totalTypes */ 7,
        /* totalFiles */ 3,
        /* internalTypes */ 1,
        /* projectDependencies */ 1,
        /* libraryDependencies */ 2
      ),
      ['com.example.core.User': 2],
      [
        ':utils': ['com.example.utils.Logger': 1],
      ],
      [
        'org.jetbrains.kotlin:kotlin-stdlib': ['kotlin.jvm.internal.Intrinsics': 2, 'kotlin.Metadata': 3, 'kotlin.collections.CollectionsKt': 1],
        'org.jetbrains:annotations': ['org.jetbrains.annotations.NotNull': 3, 'org.jetbrains.annotations.Nullable': 1],
      ],
      [:]
    )
  }

  ProjectTypeUsage expectedUtilsTypeUsage() {
    return new ProjectTypeUsage(
      ':utils',
      new TypeUsageSummary(
        /* totalTypes */ 5,
        /* totalFiles */ 2,
        /* internalTypes */ 1,
        /* projectDependencies */ 0,
        /* libraryDependencies */ 3
      ),
      ['com.example.utils.Logger': 1],
      [:],
      [
        'commons-io:commons-io': ['org.apache.commons.io.FileUtils': 1],
        'org.jetbrains.kotlin:kotlin-stdlib': ['kotlin.jvm.internal.Intrinsics': 1, 'kotlin.Metadata': 2],
        'org.jetbrains:annotations': ['org.jetbrains.annotations.NotNull': 2],
      ],
      [:]
    )
  }
}
