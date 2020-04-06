package com.autonomousapps.internal

const val ROOT_DIR = "reports/dependency-analysis"

fun getVariantDirectory(variantName: String) = "$ROOT_DIR/$variantName"

fun getArtifactsPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts.json"

fun getArtifactsPrettyPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts-pretty.json"

fun getAllUsedClassesPath(variantName: String) = "${getVariantDirectory(variantName)}/all-used-classes.txt"

fun getAllDeclaredDepsPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies.json"

fun getAllDeclaredDepsPrettyPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies-pretty.json"

fun getImportsPath(variantName: String) = "${getVariantDirectory(variantName)}/imports.json"

fun getInlineMembersPath(variantName: String) = "${getVariantDirectory(variantName)}/inline-members.json"
fun getInlineUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/inline-usage.json"

fun getConstantUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/constant-usage.json"

fun getAndroidResToSourceUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/android-res-by-source-usage.json"
fun getAndroidResToResUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/android-res-by-res-usage.json"

fun getManifestPackagesPath(variantName: String) = "${getVariantDirectory(variantName)}/manifest-packages.json"

fun getUnusedDirectDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/unused-direct-dependencies.json"

fun getUsedTransitiveDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/used-transitive-dependencies.json"

fun getAbiAnalysisPath(variantName: String) = "${getVariantDirectory(variantName)}/abi.json"

fun getAbiDumpPath(variantName: String) = "${getVariantDirectory(variantName)}/abi-dump.txt"

fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"

// Root project aggregate reports. No need for variant-specific directory
fun getMisusedDependenciesAggregatePath() = "$ROOT_DIR/misused-dependencies.json"
fun getMisusedDependenciesAggregatePrettyPath() = "$ROOT_DIR/misused-dependencies-pretty.json"
fun getAbiAggregatePath() = "$ROOT_DIR/abi.json"
fun getAbiAggregatePrettyPath() = "$ROOT_DIR/abi-pretty.json"
fun getAdviceAggregatePath() = "$ROOT_DIR/advice.json"
fun getAdviceAggregatePrettyPath() = "$ROOT_DIR/advice-pretty.json"
