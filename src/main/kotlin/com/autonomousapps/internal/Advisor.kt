package com.autonomousapps.internal

import com.autonomousapps.Behavior
import com.autonomousapps.Ignore
import com.autonomousapps.Warn
import org.gradle.api.GradleException

// TODO add a compute() function, and then simplify the getXXX() functions.

/**
 * Produces human- and machine-readable "advice" for how to modify a project's build scripts for a healthy build.
 */
internal class Advisor(
    private val unusedDirectComponents: List<UnusedDirectComponent>,
    private val usedTransitiveComponents: List<TransitiveComponent>,
    private val abiDeps: List<Dependency>,
    private val allDeclaredDeps: List<Dependency>,
    private val ignoreSpec: IgnoreSpec
) {

    internal val filterRemove = ignoreSpec.filterRemove
    internal val filterAdd = ignoreSpec.filterAdd
    internal val filterChange = ignoreSpec.filterChange

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

    /**
     * Should add used transitive dependencies.
     */
    fun getAddAdvice(): String? {
        val undeclaredApiDeps = abiDeps.filter { it.configurationName == null }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreUsedTransitiveDep(it) }

        val undeclaredImplDeps = usedTransitiveComponents.map { it.dependency }
            // Exclude any transitives which will be api dependencies
            .filterNot { trans -> undeclaredApiDeps.any { api -> api == trans } }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreUsedTransitiveDep(it) }

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

    /**
     * Should remove unused dependencies.
     */
    fun getRemoveAdvice(): String? {
        val completelyUnusedDeps = unusedDirectComponents
            // This filter was to help find "completely unused" dependencies. However, I (provisionally) think it best
            // to suggest users remove ALL unused dependencies and replace them with the used-transitives.
            //.filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreUnusedDep(it) }

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

    /**
     * Should fix dependencies on the wrong configuration.
     */
    fun getChangeAdvice(): String? {
        val shouldBeApi = abiDeps
            // Filter out those with a null configuration, as they are transitive. They will be reported in "addAdvice"
            .filterNot { it.configurationName == null }
            // Filter out those with an "api" configuration, as they're already correct.
            .filterNot { it.configurationName!!.contains("api", ignoreCase = true) }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreIncorrectConfiguration(it) }

        val shouldBeImpl = allDeclaredDeps
            // filter out those that are already a flavor of implementation
            .filterNot { it.configurationName!!.contains("implementation", ignoreCase = true) }
            // filter out those that actually should be api
            .filterNot { declared ->
                abiDeps
                    .filter { it.configurationName?.contains("api", ignoreCase = true) == true }
                    .find { api -> api == declared } != null
            }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreIncorrectConfiguration(it) }

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

    internal class IgnoreSpec(
        private val anyBehavior: Behavior = Warn(),
        private val unusedDependenciesBehavior: Behavior = Warn(),
        private val usedTransitivesBehavior: Behavior = Warn(),
        private val incorrectConfigurationsBehavior: Behavior = Warn()
    ) {
        private val shouldIgnoreAll = anyBehavior is Ignore
        internal val filterRemove = shouldIgnoreAll || unusedDependenciesBehavior is Ignore
        internal val filterAdd = shouldIgnoreAll || usedTransitivesBehavior is Ignore
        internal val filterChange = shouldIgnoreAll || incorrectConfigurationsBehavior is Ignore

        internal fun shouldIgnoreUnusedDep(dep: Dependency): Boolean {
            return if (anyBehavior is Ignore || unusedDependenciesBehavior is Ignore) {
                // If we're just ignoring everything, ignore everything
                true
            } else {
                // Otherwise, ignore if it's been specifically configured to be ignored
                anyBehavior.filter.plus(unusedDependenciesBehavior.filter).contains(dep.identifier)
            }
        }

        internal fun shouldIgnoreUsedTransitiveDep(dep: Dependency): Boolean {
            return if (anyBehavior is Ignore || usedTransitivesBehavior is Ignore) {
                // If we're just ignoring everything, ignore everything
                true
            } else {
                // Otherwise, ignore if it's been specifically configured to be ignored
                anyBehavior.filter.plus(usedTransitivesBehavior.filter).contains(dep.identifier)
            }
        }

        internal fun shouldIgnoreIncorrectConfiguration(dep: Dependency): Boolean {
            return if (anyBehavior is Ignore || incorrectConfigurationsBehavior is Ignore) {
                // If we're just ignoring everything, ignore everything
                true
            } else {
                // Otherwise, ignore if it's been specifically configured to be ignored
                anyBehavior.filter.plus(incorrectConfigurationsBehavior.filter).contains(dep.identifier)
            }
        }
    }
}
