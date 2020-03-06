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
    private val allComponents: List<Component>,
    private val unusedDirectComponents: List<UnusedDirectComponent>,
    private val usedTransitiveComponents: List<TransitiveComponent>,
    private val abiDeps: List<Dependency>,
    private val allDeclaredDeps: List<Dependency>,
    private val ignoreSpec: IgnoreSpec
) {

    internal val filterRemove = ignoreSpec.filterRemove
    internal val filterAdd = ignoreSpec.filterAdd
    internal val filterChange = ignoreSpec.filterChange
    internal val filterCompileOnly = ignoreSpec.filterCompileOnly

    private val unusedDependencies: Set<Dependency> = findUnusedDependencies()
    private val compileOnlyCandidates: Set<Component> = findCompileOnlyCandidates()

    // This is currently built by calling the various methods in this class. It's non-ideal.
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
     * Projects should add their used transitive dependencies.
     */
    fun getAddAdvice(): String? {
        val undeclaredApiDeps = abiDeps.filter { it.configurationName == null }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreUsedTransitiveDep(it) }
            // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
            .filterNot { dep -> compileOnlyCandidates.any { dep == it.dependency } }

        val undeclaredImplDeps = usedTransitiveComponents.map { it.dependency }
            // Exclude any transitives which will be api dependencies
            .filterNot { trans -> undeclaredApiDeps.any { api -> api == trans } }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreUsedTransitiveDep(it) }
            // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
            .filterNot { dep -> compileOnlyCandidates.any { dep == it.dependency } }

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
     * Projects should remove their unused dependencies.
     */
    fun getRemoveAdvice(): String? {
        val unusedDependencies = unusedDependencies
            .filterNot { unusedDep ->
                // Don't suggest removing a compileOnly dependency
                compileOnlyCandidates.any { unusedDep == it.dependency }
            }

        if (unusedDependencies.isEmpty()) {
            return null
        }

        unusedDependencies.forEach {
            advices.add(Advice.remove(it))
        }

        return unusedDependencies.joinToString(prefix = "- ", separator = "\n- ") {
            "${it.configurationName}(${printableIdentifier(it)})"
        }
    }

    /**
     * Projects should fix dependencies that are on the wrong configuration.
     */
    fun getChangeAdvice(): String? {
        val shouldBeApi = abiDeps
            // Filter out those with a null configuration, as they are transitive. They will be reported in "addAdvice"
            .filterNot { it.configurationName == null }
            // Filter out those with an "api" configuration, as they're already correct.
            .filterNot { it.configurationName!!.endsWith("api", ignoreCase = true) }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreIncorrectConfiguration(it) }
            // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
            .filterNot { dep -> compileOnlyCandidates.any { dep == it.dependency } }

        val shouldBeImpl = allDeclaredDeps
            // filter out those that are already a flavor of implementation
            .filterNot { it.configurationName!!.endsWith("implementation", ignoreCase = true) }
            // filter out those that actually should be api
            .filterNot { declared ->
                abiDeps
                    .filter { it.configurationName?.endsWith("api", ignoreCase = true) == true }
                    .find { api -> api == declared } != null
            }
            // Exclude any which are to be ignored per user configuration
            .filterNot { ignoreSpec.shouldIgnoreIncorrectConfiguration(it) }
            // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
            .filterNot { dep -> compileOnlyCandidates.any { dep == it.dependency } }

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

    /**
     * Projects should consider making some dependencies compileOnly.
     */
    fun getCompileOnlyAdvice(): String? {
        val compileOnlyDependencies = compileOnlyCandidates.asSequence()
            .filterNot {
                // We want to exclude transitives here. In other words, let people not declare used-transitive components.
                it.isTransitive
            }
            .map { it.dependency }
            .filterNot { candidate ->
                // Not a candidate if it's unused (TODO if a dependency contains only SOURCE annotations, this would be wrong)
                unusedDirectComponents.any { candidate == it.dependency }
            }
            .filterNot {
                // Here we're saying don't tell users to add a dependency to compileOnly if it already is.
                // This is variant-aware. It'll catch compileOnly, debugCompileOnly, etc.
                it.configurationName?.endsWith("compileOny", ignoreCase = true) == true
            }.filterNot {
                ignoreSpec.shouldIgnoreCompileOnly(it)
            }
            .toList()

        val compileOnlyAdvice = compileOnlyDependencies
            .joinToString(prefix = "- ", separator = "\n- ") {
                // TODO be variant-aware
                "compileOnly(${printableIdentifier(it)}) // was ${it.configurationName}"
            }

        compileOnlyDependencies.forEach {
            // TODO be variant-aware
            advices.add(Advice.compileOnly(it, "compileOnly"))
        }

        return if (compileOnlyAdvice == "- ") {
            null
        } else {
            compileOnlyAdvice
        }
    }

    private fun printableIdentifier(dependency: Dependency): String =
        if (dependency.identifier.startsWith(":")) {
            "project(\"${dependency.identifier}\")"
        } else {
            "\"${dependency.identifier}:${dependency.resolvedVersion}\""
        }

    private fun findUnusedDependencies(): Set<Dependency> = unusedDirectComponents
        // This filter was to help find "completely unused" dependencies. However, I (provisionally) think it best
        // to suggest users remove ALL unused dependencies and replace them with the used-transitives.
        //.filter { it.usedTransitiveDependencies.isEmpty() }
        .map { it.dependency }
        // Exclude any which are to be ignored per user configuration
        .filterNot { ignoreSpec.shouldIgnoreUnusedDep(it) }
        .toSet()

    /**
     * A component is a compileOnly candidate if:
     *
     * 1. It consists of only compile-time annotations (per analysis by AnalyzedJar class) OR
     * 2. It is already declared on the compileOnly configuration. Here we assume users know what they're doing.
     */
    private fun findCompileOnlyCandidates(): Set<Component> = allComponents
        .filter {
            it.isCompileOnlyAnnotations ||
                it.dependency.configurationName?.endsWith("compileOnly", ignoreCase = true) == true
        }
        .toSet()

    internal class IgnoreSpec(
        private val anyBehavior: Behavior = Warn(),
        private val unusedDependenciesBehavior: Behavior = Warn(),
        private val usedTransitivesBehavior: Behavior = Warn(),
        private val incorrectConfigurationsBehavior: Behavior = Warn(),
        private val compileOnlyBehavior: Behavior = Warn()
    ) {
        private val shouldIgnoreAll = anyBehavior is Ignore
        internal val filterRemove = shouldIgnoreAll || unusedDependenciesBehavior is Ignore
        internal val filterAdd = shouldIgnoreAll || usedTransitivesBehavior is Ignore
        internal val filterChange = shouldIgnoreAll || incorrectConfigurationsBehavior is Ignore
        internal val filterCompileOnly = shouldIgnoreAll || compileOnlyBehavior is Ignore

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

        internal fun shouldIgnoreCompileOnly(dep: Dependency): Boolean {
            return if (anyBehavior is Ignore || compileOnlyBehavior is Ignore) {
                // If we're just ignoring everything, ignore everything
                true
            } else {
                // Otherwise, ignore if it's been specifically configured to be ignored
                anyBehavior.filter.plus(compileOnlyBehavior.filter).contains(dep.identifier)
            }
        }
    }
}
