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

fun getInlineMembersPath(variantName: String) = "${getVariantDirectory(variantName)}/inline-members.json"
fun getInlineUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/inline-usage.json"

fun getAndroidResUsagePath(variantName: String) = "${getVariantDirectory(variantName)}/android-res-usage.json"

fun getUnusedDirectDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/unused-direct-dependencies.json"

fun getUsedTransitiveDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/used-transitive-dependencies.json"

fun getMisusedDependenciesHtmlPath(variantName: String) =
    "${getVariantDirectory(variantName)}/misused-dependencies.html"

fun getAbiAnalysisPath(variantName: String) = "${getVariantDirectory(variantName)}/abi.json"

fun getAbiDumpPath(variantName: String) = "${getVariantDirectory(variantName)}/abi-dump.txt"

fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.txt"

// Root project aggregate reports. No need for variant-specific directory
fun getMisusedDependenciesAggregatePath() = "$ROOT_DIR/misused-dependencies.json"
fun getMisusedDependenciesAggregatePrettyPath() = "$ROOT_DIR/misused-dependencies-pretty.json"
fun getAbiAggregatePath() = "$ROOT_DIR/abi.json"
fun getAbiAggregatePrettyPath() = "$ROOT_DIR/abi-pretty.json"
fun getAdviceAggregatePath() = "$ROOT_DIR/advice.json"
fun getAdviceAggregatePrettyPath() = "$ROOT_DIR/advice-pretty.json"
