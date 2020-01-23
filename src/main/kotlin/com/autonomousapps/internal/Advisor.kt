package com.autonomousapps.internal

import org.gradle.api.GradleException

/**
 * Produces human- and machine-readable "advice" for how to modify a project's build scripts for a healthy build.
 */
internal class Advisor(
    private val unusedDirectComponents: List<UnusedDirectComponent>,
    private val usedTransitiveComponents: List<TransitiveComponent>,
    private val abiDeps: List<Dependency>,
    private val allDeclaredDeps: List<Dependency>
) {

    private val advices = mutableSetOf<Advice>()

    /**
     * A machine-readable format (JSON) for the advice.
     */
    fun getAdvices(): Set<Advice> {
        // We strip the configuration name from the internal dependency instance to reduce noise in the output
        return advices.map {
            it.copy(dependency = it.dependency.copy(configurationName = null))
        }.toSortedSet()
    }

    fun getAddAdvice(): String? {
        val undeclaredApiDeps = abiDeps.filter { it.configurationName == null }
        val undeclaredImplDeps = usedTransitiveComponents.map { it.dependency }
            // Exclude any transitives which will be api dependencies
            .filterNot { trans -> undeclaredApiDeps.any { api -> api == trans } }

        if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) {
            return null
        }

        undeclaredApiDeps.forEach {
            advices.add(Advice.add(it, "api"))
        }
        undeclaredImplDeps.forEach {
            advices.add(Advice.add(it, "implementation"))
        }

        val apiAdvice = undeclaredApiDeps.joinToString(prefix = "- ", separator = "\n- ") {
            "api(${printableIdentifier(it)})"
        }
        // TODO some of these might need to be compileOnly, e.g. javax:inject (but not necessarily this one!)
        val implAdvice = undeclaredImplDeps.joinToString(prefix = "- ", separator = "\n- ") {
            "implementation(${printableIdentifier(it)})"
        }

        return if (undeclaredApiDeps.isNotEmpty() && undeclaredImplDeps.isNotEmpty()) {
            "$apiAdvice\n$implAdvice"
        } else if (undeclaredApiDeps.isNotEmpty()) {
            apiAdvice
        } else if (undeclaredImplDeps.isNotEmpty()) {
            implAdvice
        } else {
            // One or the other list must be non-empty
            throw GradleException("Impossible")
        }
    }

    fun getRemoveAdvice(): String? {
        val completelyUnusedDeps = unusedDirectComponents
            // This filter was to help find "completely unused" dependencies. However, I (provisionally) think it best
            // to suggest users remove ALL unused dependencies and replace them with the used-transitives.
            //.filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency }

        if (completelyUnusedDeps.isEmpty()) {
            return null
        }

        completelyUnusedDeps.forEach {
            advices.add(Advice.remove(it))
        }

        return completelyUnusedDeps.joinToString(prefix = "- ", separator = "\n- ") {
            "${it.configurationName}(${printableIdentifier(it)})"
        }
    }

    fun getChangeAdvice(): String? {
        val shouldBeApi = abiDeps
            // Filter out those with a null configuration, as they are transitive. They will be reported in "addAdvice"
            .filterNot { it.configurationName == null }
            // Filter out those with an "api" configuration, as they're already correct.
            .filterNot { it.configurationName!!.contains("api", ignoreCase = true) }
        val shouldBeImpl = allDeclaredDeps
            // filter out those that are already a flavor of implementation
            .filterNot { it.configurationName!!.contains("implementation", ignoreCase = true) }
            // filter out those that actually should be api
            .filterNot { declared ->
                abiDeps
                    .filter { it.configurationName?.contains("api", ignoreCase = true) == true }
                    .find { api -> api == declared } != null
            }

        if (shouldBeApi.isEmpty() && shouldBeImpl.isEmpty()) {
            return null
        }

        shouldBeApi.forEach {
            advices.add(Advice.change(it, toConfiguration = "api"))
        }
        shouldBeImpl.forEach {
            advices.add(Advice.change(it, toConfiguration = "implementation"))
        }

        val apiAdvice = shouldBeApi.joinToString(prefix = "- ", separator = "\n- ") {
            "api(${printableIdentifier(it)}) // was ${it.configurationName}"
        }
        val implAdvice = shouldBeImpl.joinToString(prefix = "- ", separator = "\n- ") {
            "implementation(${printableIdentifier(it)}) // was ${it.configurationName}"
        }
        return if (shouldBeApi.isNotEmpty() && shouldBeImpl.isNotEmpty()) {
            "$apiAdvice\n$implAdvice"
        } else if (shouldBeApi.isNotEmpty()) {
            apiAdvice
        } else if (shouldBeImpl.isNotEmpty()) {
            implAdvice
        } else {
            // One or the other list must be non-empty
            throw GradleException("Impossible")
        }
    }

    private fun printableIdentifier(dependency: Dependency): String =
        if (dependency.identifier.startsWith(":")) {
            "project(\"${dependency.identifier}\")"
        } else {
            "\"${dependency.identifier}:${dependency.resolvedVersion}\""
        }
}
