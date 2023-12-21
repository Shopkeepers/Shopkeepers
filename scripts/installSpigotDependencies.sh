#!/usr/bin/env bash

pushd "$(dirname "$BASH_SOURCE")"

# $1: Spigot version to build
# $2: "remapped" to check for a remapped server jar
buildSpigotIfMissing() {
  local classifier=""
  if [ "$2" = "remapped" ]; then classifier="-remapped-mojang"; fi
  if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/$1-R0.1-SNAPSHOT/craftbukkit-$1-R0.1-SNAPSHOT${classifier}.jar" ]; then
    ./installSpigot.sh "$1"
  else
    echo "Not building Spigot $1 because it is already in our Maven repo"
  fi
}

# We only re-build CraftBukkit/Spigot versions that are missing in the Maven cache.
# Add entries here for every required version of CraftBukkit/Spigot.

# The following versions require JDK 8 to build:
source installJDK.sh 8

buildSpigotIfMissing 1.16.5

# The following versions require JDK 16 to build:
source installJDK.sh 16

buildSpigotIfMissing 1.17.1 remapped

# The following versions require JDK 17 to build:
source installJDK.sh 17

buildSpigotIfMissing 1.18.2 remapped
buildSpigotIfMissing 1.19 remapped
buildSpigotIfMissing 1.19.2 remapped
buildSpigotIfMissing 1.19.3 remapped
buildSpigotIfMissing 1.19.4 remapped
buildSpigotIfMissing 1.20.1 remapped
buildSpigotIfMissing 1.20.2 remapped
buildSpigotIfMissing 1.20.4 remapped

popd
