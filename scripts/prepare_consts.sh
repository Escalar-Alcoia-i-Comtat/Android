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
if [[ -z "${MAPS_API_KEY}" ]]; then
  echo "🛑 ${red_bg}${white}MAPS_API_KEY${reset} not declared"
  ERROR=1
fi
if [[ -z "${SENTRY_DNS}" ]]; then
  echo "🛑 ${red_bg}${white}SENTRY_DNS${reset} not declared"
  ERROR=1
fi
if [[ -z "${AUTH_CLIENT_ID}" ]]; then
  echo "🛑 ${red_bg}${white}AUTH_CLIENT_ID${reset} not declared"
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

echo "🗑 Removing old secure.properties..."
rm -rf base/secure.properties

echo "✏ Writing preferences to secure.properties..."
{
  echo "MAPS_API_KEY=$MAPS_API_KEY"
  echo "AUTH_CLIENT_ID=$AUTH_CLIENT_ID"
  echo "SENTRY_DNS=$SENTRY_DNS"
  echo "FTP_SERVER=$FTP_SERVER"
  echo "FTP_USER=$FTP_USER"
  echo "FTP_PASS=$FTP_PASS"
} >> secure.properties
