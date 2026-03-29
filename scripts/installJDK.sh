#!/usr/bin/env bash
if [ $# -eq 0 ]; then
    echo "Missing argument(s)"
    exit 1
fi

# Check for predefined JDK version aliases:
# Some JDK versions are not available on some platforms via SDKMAN.
USE_JABBA=false
case "$1" in
  8)
    JDK_VERSION="8.0.412-tem"
    ;;
  16)
    JDK_VERSION="adopt@1.16.0-1"
    USE_JABBA=true
    ;;
  17)
    JDK_VERSION="17.0.11-tem"
    ;;
  21)
    JDK_VERSION="21.0.3-tem"
    ;;
  25)
    JDK_VERSION="25.0.2-tem"
    ;;
  *)
    JDK_VERSION=$1
    ;;
esac

echo Installing Java JDK: $JDK_VERSION

if [ $USE_JABBA = true ]; then
  # Use Jabba to manage multiple JDK versions regardless of the platform.
  # Install Jabba if it is not already installed.
  pushd "$(dirname "$BASH_SOURCE")"
  source installJabba.sh
  popd

  # Install the requested JDK version:
  jabba install $JDK_VERSION

  # Update environment variables:
  # 'jabba use' does not work on msys currently.
  case "$OSTYPE" in
      msys)
      # Workaround for msys: Set JAVA_HOME and PATH manually.
      # We need to prepare the path returned by Jabba for this to work in msys.
      JABBA_JDK_HOME=$(jabba which --home $JDK_VERSION)
      JABBA_JDK_HOME=${JABBA_JDK_HOME//\\/\\\\}
      JABBA_JDK_HOME=$(cygpath -u $JABBA_JDK_HOME)
      JAVA_HOME=$JABBA_JDK_HOME
      PATH=$JABBA_JDK_HOME/bin:$PATH
      ;;
      *)
      jabba use $JDK_VERSION
      ;;
  esac
else
  # Use Sdkman to manage multiple JDK versions regardless of the platform.
  # Workaround for zip missing on some platforms (e.g. Git bash for Windows):
  if [ ! -f ~/bin/zip ]; then
    echo "Installing unzip -> zip"
    mkdir -p ~/bin
    cp /usr/bin/unzip ~/bin/zip
  fi

  # Install Sdkman if it is not already installed:
  curl -s "https://get.sdkman.io?rcupdate=false" | bash
  source "${HOME}/.sdkman/bin/sdkman-init.sh"

  # Update if an update is available:
  sdk selfupdate

  # Install the requested JDK version:
  sdk install java $JDK_VERSION

  sdk use java $JDK_VERSION
fi

# Verify that the expected Java version is used:
#echo Java home: $JAVA_HOME
#echo Path: $PATH
echo Active Java version: $(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
