package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.ReasonableDependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class ReasonableDependencyTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a 'full' report of all dependencies and the facilities they contribute"
  }

  /**
   * [`List<TransitiveComponent>`][TransitiveComponent] -- the transitive dependencies used directly
   * by this project. Includes the classes provided by these dependencies that are directly used.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedTransitiveComponents: RegularFileProperty

  /**
   * The [Component] representation of this project's dependencies. In our nomenclature here, these
   * are the "producers" -- and what they produce.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val components: RegularFileProperty

  /**
   * TODO
   */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val publicComponents: RegularFileProperty

  /**
   * Dependencies presumed to contribute used inline members.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val inlineUsage: RegularFileProperty

  /**
   * Dependencies presumed to contribute used constants.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val constantUsage: RegularFileProperty

  /**
   * Dependencies presumed to contribute ... things.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val generalUsage: RegularFileProperty

  /**
   * Dependencies presumed to contribute Android libraries that contain manifest components.
   */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val manifests: RegularFileProperty

  /**
   * Dependencies presumed to contribute Android libraries that contain Android resources used
   * by resources in the current project.
   */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val resByRes: RegularFileProperty

  /**
   * Dependencies presumed to contribute Android libraries that contain Android resources used
   * by Java/Kotlin source in the current project.
   */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val resBySource: RegularFileProperty

  /**
   * Dependencies presumed to contribute Android libraries that contain native libs whose usage
   * cannot be readily determined.
   */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val nativeDeps: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val components = components.fromJsonList<Component>()
    val usedTransitiveComponents = usedTransitiveComponents.fromJsonList<TransitiveComponent>()
    val publicComponents = publicComponents.fromNullableJsonSet<PublicComponent>() ?: emptySet()
    val inlineDependencies = inlineUsage.fromJsonSet<Dependency>()
    val constantDependencies = constantUsage.fromJsonSet<Dependency>()
    val generalDependencies = generalUsage.fromJsonSet<Dependency>()
    val manifests = manifests.fromNullableJsonSet<Manifest>() ?: emptySet()
    val resByRes = resByRes.fromNullableJsonSet<AndroidPublicRes>() ?: emptySet()
    val resBySource = resBySource.fromNullableJsonSet<Dependency>() ?: emptySet()
    val nativeDeps = nativeDeps.fromNullableJsonSet<NativeLibDependency>() ?: emptySet()

    val reasonableDependencyMap = linkedMapOf<String, ReasonableDependency.Builder>()

    components.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        component = it
      }) { old, new ->
        old.apply {
          component = new.component
        }
      }
    }
    usedTransitiveComponents.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        variants = it.variants
        usedTransitiveClasses = it.usedTransitiveClasses
      }) { old, new ->
        old.apply {
          variants = new.variants
          usedTransitiveClasses = new.usedTransitiveClasses
        }
      }
    }
    publicComponents.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        publicClasses = it.classes
      }) { old, new ->
        old.apply {
          publicClasses = new.publicClasses
        }
      }
    }
    inlineDependencies.forEach {
      reasonableDependencyMap.merge(it.identifier, ReasonableDependency.Builder(it).apply {
        providesInlineMembers = true
      }) { old, new ->
        old.apply {
          providesInlineMembers = new.providesInlineMembers
        }
      }
    }
    constantDependencies.forEach {
      reasonableDependencyMap.merge(it.identifier, ReasonableDependency.Builder(it).apply {
        providesConstants = true
      }) { old, new ->
        old.apply {
          providesConstants = new.providesConstants
        }
      }
    }
    generalDependencies.forEach {
      reasonableDependencyMap.merge(it.identifier, ReasonableDependency.Builder(it).apply {
        providesGeneralImports = true
      }) { old, new ->
        old.apply {
          providesGeneralImports = new.providesGeneralImports
        }
      }
    }
    manifests.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        providesManifestComponents = it.hasComponents
      }) { old, new ->
        old.apply {
          providesManifestComponents = new.providesManifestComponents
        }
      }
    }
    resByRes.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        providesResByRes = true
      }) { old, new ->
        old.apply {
          providesResByRes = new.providesResByRes
        }
      }
    }
    resBySource.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        providesResBySource = true
      }) { old, new ->
        old.apply {
          providesResBySource = new.providesResBySource
        }
      }
    }
    nativeDeps.forEach {
      reasonableDependencyMap.merge(it.dependency.identifier, ReasonableDependency.Builder(it.dependency).apply {
        providesNativeLibs = true
      }) { old, new ->
        old.apply {
          providesNativeLibs = new.providesNativeLibs
        }
      }
    }

    val reasonableDependencies = reasonableDependencyMap.values.mapToOrderedSet { it.build() }
    outputFile.writeText(reasonableDependencies.toJson())
  }
}
