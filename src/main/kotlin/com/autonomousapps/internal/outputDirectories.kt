package com.autonomousapps.internal

const val ROOT_DIR = "reports/dependency-analysis"

fun getVariantDirectory(variantName: String) = "$ROOT_DIR/$variantName"

fun getLocationsPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/locations.json"

fun getArtifactsPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/artifacts.json"

fun getArtifactsPrettyPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/artifacts-pretty.json"

fun getAllUsedClassesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/all-used-classes.txt"

fun getAllDeclaredDepsPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/all-declared-dependencies.json"

fun getAllDeclaredDepsPrettyPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/all-declared-dependencies-pretty.json"

fun getImportsPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/imports.json"

fun getInlineMembersPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/inline-members.json"

fun getInlineUsagePath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/inline-usage.json"

fun getConstantUsagePath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/constant-usage.json"

fun getAndroidResToSourceUsagePath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/android-res-by-source-usage.json"

fun getAndroidResToResUsagePath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/android-res-by-res-usage.json"

fun getManifestPackagesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/manifest-packages.json"

fun getUnusedDirectDependenciesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/unused-direct-dependencies.json"

fun getUsedTransitiveDependenciesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/used-transitive-dependencies.json"

fun getDeclaredProcPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/procs-declared.json"

fun getDeclaredProcPrettyPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/procs-declared-pretty.json"

fun getUnusedProcPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/procs-unused.json"

fun getAbiAnalysisPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/abi.json"

fun getAbiDumpPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/abi-dump.txt"

fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"

// Root project aggregate reports. No need for variant-specific directory
fun getMisusedDependenciesAggregatePath() = "$ROOT_DIR/intermediates/misused-dependencies.json"
fun getMisusedDependenciesAggregatePrettyPath() =
  "$ROOT_DIR/intermediates/misused-dependencies-pretty.json"

fun getAbiAggregatePath() = "$ROOT_DIR/intermediates/abi.json"
fun getAbiAggregatePrettyPath() = "$ROOT_DIR/intermediates/abi-pretty.json"
fun getAdviceAggregatePath() = "$ROOT_DIR/advice.json"
fun getAdviceAggregatePrettyPath() = "$ROOT_DIR/advice-pretty.json"
