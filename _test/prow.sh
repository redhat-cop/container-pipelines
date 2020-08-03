#!/usr/bin/env bash

run() {
  echo "Running applier for all..."

  for file in $(find . -name ".test.sh" -type f | xargs); do
    pushd $(dirname $file) > /dev/null

    echo ""
    echo "## $(pwd)"
    echo ""

    ./.test.sh applier || exit $?
    ./.test.sh test || exit $?
    ./.test.sh cleanup || exit $?

    popd > /dev/null
  done
}

run