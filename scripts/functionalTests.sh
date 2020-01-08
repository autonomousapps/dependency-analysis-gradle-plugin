#!/usr/bin/env bash
# Script to execute functional tests against a list of AGP versions
# Must be run from the project root directory (not THIS directory!)

pwd
agpVersions=('3.5.3' '3.6.0-rc01' '4.0.0-alpha08')

for v in "${agpVersions[@]}"; do
  echo "Executing functional tests against AGP $v"
  ./gradlew functionalTest -DfuncTest.agpVersion="$v"
done
