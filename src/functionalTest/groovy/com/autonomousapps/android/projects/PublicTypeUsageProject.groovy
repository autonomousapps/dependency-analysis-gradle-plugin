// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidSubproject
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.kit.gradle.kotlin.Kotlin
import com.autonomousapps.model.internal.PublicTypeUsage

import static com.autonomousapps.internal.OutputPathsKt.getPublicTypeUsagePath
import static com.autonomousapps.internal.utils.MoshiUtils.MOSHI
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class PublicTypeUsageProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  PublicTypeUsageProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins += [plugins.kspNoApply, plugins.hiltNoApply]
          bs.additions = '''\
          dependencyAnalysis {
            abi {
              exclusions {
                ignoreGeneratedCode()
                ignoreSubPackage("hilt_aggregated_deps")
                excludeClasses(
                  "^.*\\\\.Hilt_.*\\$",
                  // e.g., AutoBindRealThingSingletonModule
                  "^.*\\\\.AutoBind\\\\w+Module\\$",
                )
              }
            }
          }
          '''.stripIndent()
        }
      }
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins(androidApp() + hilt())
          bs.android = defaultAndroidAppBlock()
          bs.kotlin = Kotlin.DEFAULT
          bs.additions = appFlavors() + '\n' + excludeTypes()
          bs.dependencies(
            implementation(':annotation'),
            implementation(':inline'),
            implementation(':java-lib'),
            fireImplementation(':module-fire'),
            waterImplementation(':module-water'),
            hiltAndroid('implementation'),
            hiltAndroidCompiler('ksp'),
          )
        }
        app.sources = appSources()
        app.manifest = AndroidManifest.appWithoutStyle('com.example.app.HiltApplication')
      }
      .withAndroidLibProject('module-fire') { module ->
        module.withBuildScript { bs ->
          bs.plugins(androidLib() + hilt())
          bs.android = defaultAndroidLibBlock(true, "com.example.${packageOf(module)}")
          bs.dependencies(moduleDependencies())
          bs.additions = excludeTypes()
        }
        module.sources = moduleSources() + fireService()
        module.manifest = AndroidManifest.of(
          """\
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">            
            <application>
              <service android:name=".FireService" />
            </application>
            </manifest>
          """.stripIndent()
        )
      }
      .withAndroidLibProject('module-water') { module ->
        module.withBuildScript { bs ->
          bs.plugins(androidLib() + hilt())
          bs.android = defaultAndroidLibBlock(true, "com.example.${packageOf(module)}")
          bs.dependencies(moduleDependencies())
        }
        module.sources = moduleSources()
      }
      .withSubproject('inline') { inline ->
        inline.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.kotlin = Kotlin.DEFAULT
          bs.java = Java.of(8)
        }
        inline.sources = inlineSources()
      }
      .withSubproject('annotation') { annotation ->
        annotation.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.kotlin = Kotlin.DEFAULT
          bs.java = Java.of(8)
        }
        annotation.sources = annotationSources()
      }
      .withSubproject('java-lib') { javaLib ->
        javaLib.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.java = Java.of(8)
        }
        javaLib.sources = javaLibSources()
      }
      .write()
  }

  private static Dependency fireImplementation(String dependency) {
    new Dependency('fireImplementation', dependency)
  }

  private static Dependency waterImplementation(String dependency) {
    new Dependency('waterImplementation', dependency)
  }

  private static List<Dependency> moduleDependencies() {
    [
      autoDaggerApi('implementation'),
      autoDaggerCompiler('ksp'),
      hiltAndroid('implementation'),
      hiltAndroidCompiler('ksp'),
    ]
  }

  private static String packageOf(AndroidSubproject.Builder module) {
    return module.name.replace('-', '.')
  }

  private static String appFlavors() {
    '''\
    android {
      flavorDimensions += "element"
      productFlavors {
        create("fire") {
          dimension = "element"
        }
        create("water") {
          dimension = "element"
        }
      }
    }
    '''.stripIndent()
  }

  // TODO(tsr): this isn't global yet
  private static String excludeTypes() {
    '''\
      dependencyAnalysis {
        typeUsage {
          excludeRegex(
            "^.*_GeneratedInjector\\$",
            "^.*_ProvidesMagicFactory\\$",
            "^hilt_aggregated_deps.*\\$",
          )
        }
      }'''.stripIndent()
  }

  private static List<Source> appSources() {
    [
      Source.kotlin(
        '''\
        package com.example.app

        import android.app.Application
        import com.example.annotation.BinaryRetainedAnnotation
        import com.example.annotation.SourceRetainedAnnotation
        import com.example.inlinefun.soMuchFun
        import com.example.javalib.PublicUsedClass
        import com.example.module.Magic
        import com.example.module.Thing
        import dagger.hilt.android.HiltAndroidApp
        import javax.inject.Inject

        @BinaryRetainedAnnotation
        @SourceRetainedAnnotation
        @HiltAndroidApp
        class HiltApplication : Application() {
          @Inject lateinit var magic: Magic
          @Inject lateinit var thing: Thing
          
          private fun usesInlineFun() {
            soMuchFun()
          }
          
          private fun usesJavaLib() {
            PublicUsedClass()
          }
        }
        '''.stripIndent()
      ).build()
    ]
  }

  private static List<Source> moduleSources() {
    [
      Source.kotlin(
        '''\
        package com.example.module

        import dagger.Module
        import dagger.Provides
        import dagger.hilt.InstallIn
        import dagger.hilt.components.SingletonComponent

        @Module
        @InstallIn(SingletonComponent::class)
        class MagicModule {
          @Provides fun providesMagic(): Magic = RealMagic()
        }
        '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.module

        interface Magic {
          fun magic(): Int
        }
        '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.module

        import javax.inject.Singleton

        @Singleton
        class RealMagic : Magic {
          override fun magic(): Int = 42
        }
        '''.stripIndent()
      ).build(),

      // AutoDagger
      Source.kotlin(
        '''\
        package com.example.module
        
        import javax.inject.Inject
        import se.ansman.dagger.auto.AutoBind
        
        @AutoBind
        class RealThing @Inject constructor() : Thing
      '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.module
        
        interface Thing
      '''.stripIndent()
      ).build(),
    ]
  }

  private static Source fireService() {
    Source.kotlin(
      '''\
        package com.example.module

        import android.app.Service
        import android.content.Intent
        import android.os.IBinder
        import dagger.hilt.android.AndroidEntryPoint

        @AndroidEntryPoint
        class FireService : Service() {        
          override fun onBind(intent: Intent): IBinder? = null
        }
      '''.stripIndent()
    ).build()
  }

  private static List<Source> inlineSources() {
    [
      Source.kotlin(
        '''\
        package com.example.inlinefun
        
        inline fun soMuchFun() {
          println("I'm an inline function!")
        }
        '''.stripIndent()
      )
        .withPath('com.example.inlinefun', 'InlineFuns')
        .build()
    ]
  }

  private static List<Source> annotationSources() {
    [
      Source.kotlin(
        '''\
        package com.example.annotation
        
        @Retention(AnnotationRetention.BINARY)
        annotation class BinaryRetainedAnnotation
        '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.annotation
        
        @Retention(AnnotationRetention.SOURCE)
        annotation class SourceRetainedAnnotation
        '''.stripIndent()
      ).build(),
    ]
  }

  private static List<Source> javaLibSources() {
    [
      Source.java(
        '''\
        package com.example.javalib;
        
        public class PublicUsedClass {}
        '''.stripIndent()
      ).build(),
      Source.java(
        '''\
        package com.example.javalib;
        
        public class PublicUnusedClass {}
        '''.stripIndent()
      ).build(),
      Source.java(
        '''\
        package com.example.javalib;
        
        public class AnotherPublicUnusedClass {}
        '''.stripIndent()
      ).build(),
      Source.java(
        '''\
        package com.example.javalib;
        
        class PackagePrivateClass {}
        '''.stripIndent()
      ).build(),
    ]
  }

  PublicTypeUsage actualPublicTypeUsage() {
    def publicTypeUsage = gradleProject.singleArtifact(':', getPublicTypeUsagePath())
    def adapter = MOSHI.adapter(PublicTypeUsage)
    return adapter.fromJson(publicTypeUsage.asPath.text)
  }

  // TODO(tsr): make it easier to create these instances
  PublicTypeUsage expectedPublicTypeUsage = new PublicTypeUsage(
    [
      new PublicTypeUsage.Report(
        ':annotation',
        [
          new PublicTypeUsage.Accesses(
            'com.example.annotation.BinaryRetainedAnnotation',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.annotation.SourceRetainedAnnotation',
            [':app'] as Set<String>
          ),
        ] as Set<PublicTypeUsage.Accesses>,
        [] as Set<String>
      ),

      new PublicTypeUsage.Report(
        ':app',
        [] as Set<PublicTypeUsage.Accesses>,
        [] as Set<String>
      ),

      new PublicTypeUsage.Report(
        ':inline',
        [
          new PublicTypeUsage.Accesses(
            'com.example.inlinefun.InlineFunsKt',
            [':app'] as Set<String>
          ),
        ] as Set<PublicTypeUsage.Accesses>,
        [] as Set<String>
      ),

      new PublicTypeUsage.Report(
        ':java-lib',
        [
          new PublicTypeUsage.Accesses(
            'com.example.javalib.PublicUsedClass',
            [':app'] as Set<String>
          )
        ] as Set<PublicTypeUsage.Accesses>,
        [
          'com.example.javalib.AnotherPublicUnusedClass',
          'com.example.javalib.PublicUnusedClass',
        ] as Set<String>
      ),

      new PublicTypeUsage.Report(
        ':module-fire',
        [
          new PublicTypeUsage.Accesses(
            'com.example.module.FireService',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.Magic',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.MagicModule',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.RealThing',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.Thing',
            [':app'] as Set<String>
          ),
        ] as Set<PublicTypeUsage.Accesses>,
        ['com.example.module.RealMagic'] as Set<String>
      ),

      new PublicTypeUsage.Report(
        ':module-water',
        [
          new PublicTypeUsage.Accesses(
            'com.example.module.Magic',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.MagicModule',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.RealThing',
            [':app'] as Set<String>
          ),
          new PublicTypeUsage.Accesses(
            'com.example.module.Thing',
            [':app'] as Set<String>
          ),
        ] as Set<PublicTypeUsage.Accesses>,
        ['com.example.module.RealMagic'] as Set<String>
      ),
    ] as Set<PublicTypeUsage.Report>
  )
}
