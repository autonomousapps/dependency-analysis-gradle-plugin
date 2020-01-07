@file:Suppress("MemberVisibilityCanBePrivate")

package com.autonomousapps.stubs

import com.autonomousapps.internal.Dependency
import com.autonomousapps.stubs.StubResolvedComponentResult.*

object FirstLevelResults {
    val projectPlatform = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubProjectComponentIdentifier(
                projectPath = ":platform"
            )
        )
    )

    val projectEntities = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubProjectComponentIdentifier(
                projectPath = ":entities"
            )
        )
    )

    val androidXAppCompat = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.appcompat:appcompat"),
                version = "1.1.0-rc01"
            )
        )
    )

    val androidXRoomRuntime = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.room:room-runtime"),
                version = "2.2.0-alpha01"
            )
        )
    )

    val androidXRoomRxJava2 = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.room:room-rxjava2"),
                version = "2.2.0-alpha01"
            )
        )
    )

    val androidXLifecycleExtensions = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.lifecycle:lifecycle-extensions"),
                version = "2.0.0"
            )
        )
    )

    val androidXLifecycleJava8 = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.lifecycle:lifecycle-common-java8"),
                version = "2.0.0"
            )
        )
    )

    val androidXLifecycleReactiveStreams = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("androidx.lifecycle:lifecycle-reactivestreams-ktx"),
                version = "2.0.0"
            )
        )
    )

    val moshi = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("com.squareup.moshi:moshi"),
                version = "1.8.0"
            )
        )
    )

    val moshiAdapters = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("com.squareup.moshi:moshi-adapters"),
                version = "1.8.0"
            )
        )
    )

    val moshiKotlin = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("com.squareup.moshi:moshi-kotlin"),
                version = "1.8.0"
            )
        )
    )

    val retrofitMoshi = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("com.squareup.retrofit2:converter-moshi"),
                version = "2.5.0"
            )
        )
    )
}

object Results {
    val javaxInject = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("javax.inject:javax.inject"),
                version = "1"
            )
        )
    )

    val kotlinStdlibCommon = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("org.jetbrains.kotlin:kotlin-stdlib-common"),
                version = "1.3.60"
            )
        )
    )

    val jetbrainsAnnotations = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = emptySet(),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("org.jetbrains:annotations"),
                version = "13.0"
            )
        )
    )

    val kotlinStdlib = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = setOf(kotlinStdlibCommon, jetbrainsAnnotations),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("org.jetbrains.kotlin:kotlin-stdlib"),
                version = "1.3.60"
            )
        )
    )

    val kotlinStdlibJdk7 = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = setOf(kotlinStdlib),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("org.jetbrains.kotlin:kotlin-stdlib-jdk7"),
                version = "1.3.60"
            )
        )
    )

    val kotlinStdlibJdk8 = StubResolvedDependencyResult(
        StubResolvedComponentResult(
            dependencies = setOf(kotlinStdlib),
            componentIdentifier = StubModuleComponentIdentifier(
                moduleIdentifier = StubModuleIdentifier("org.jetbrains.kotlin:kotlin-stdlib-jdk8"),
                version = "1.3.60"
            )
        )
    )
}

object Dependencies {
    val kotlinStdlibJdk7 = Dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "1.3.60")
    val kotlinStdlib = Dependency("org.jetbrains.kotlin:kotlin-stdlib", "1.3.60")
    val jetbrainsAnnotations = Dependency("org.jetbrains:annotations", "13.0")
    val javaxInject = Dependency("javax.inject:javax.inject", "1")
}
