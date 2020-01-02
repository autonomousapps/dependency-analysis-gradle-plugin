@file:Suppress("MemberVisibilityCanBePrivate")

package com.autonomousapps.stubs

import com.autonomousapps.internal.Dependency
import com.autonomousapps.stubs.StubResolvedComponentResult.*

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
}

object Dependencies {
    val kotlinStdlibJdk7 = Dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "1.3.60")
    val kotlinStdlib = Dependency("org.jetbrains.kotlin:kotlin-stdlib", "1.3.60")
    val jetbrainsAnnotations = Dependency("org.jetbrains:annotations", "13.0")
    val javaxInject = Dependency("javax.inject:javax.inject", "1")
}
