#!/usr/bin/env bash

red_bg=$(tput setab 1)
white=$(tput setaf 7)
reset=$(tput sgr0)

echo "🔁 Checking for missing environment variables..."
ERROR=0
if [[ -z "${FTP_SERVER}" ]]; then
  echo "🛑 ${red_bg}${white}FTP_SERVER${reset} not declared"
  ERROR=1
fi
if [[ -z "${FTP_USER}" ]]; then
  echo "🛑 ${red_bg}${white}FTP_USER${reset} not declared"
  ERROR=1
fi
if [[ -z "${FTP_PASS}" ]]; then
  echo "🛑 ${red_bg}${white}FTP_PASS${reset} not declared"
  ERROR=1
fi

if [ $ERROR -eq 1 ]; then
  echo "🛑 Some environment variables were missing, won't continue."
  exit 2
fi

SCRIPT=$(readlink -f "$0")
BASEDIR=$(dirname "$SCRIPT")

echo "📂 Accessing project root dir..."
cd "$BASEDIR" || {
  echo "🛑 Project root dir doesn't exist ($BASEDIR)"
  exit 1
}
cd ..

echo "🗑 Removing old cred.gradle..."
rm -rf base/cred.gradle

echo "✏ Writing preferences to cred.gradle..."
{
  echo "project.ext.FTP_SERVER=\"\\\"$FTP_SERVER\\\"\""
  echo "project.ext.FTP_USER=\"\\\"$FTP_USER\\\"\""
  echo "project.ext.FTP_PASS=\"\\\"$FTP_PASS\\\"\""
} >> base/cred.gradle
