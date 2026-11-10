#!/usr/bin/env bash
# Prüft den Signierschlüssel aus den Secrets und erklärt verständlich, was nicht passt.
#
# Eingaben (Umgebung): KEYSTORE_BASE64, SIGNING_PASSWORD, KEY_ALIAS (optional),
#   LEVEL (optional: "error" oder "warning" für die Annotations, Standard "error")
# Argument 1: Zielpfad für die entpackte Keystore-Datei
# Ausgabe: der verwendete Alias auf stdout; Hinweise als GitHub-Annotations auf stderr.
# Exit-Code 0 = Schlüssel in Ordnung, 1 = Problem (Meldung wurde ausgegeben).
set -uo pipefail

target="$1"
level="${LEVEL:-error}"

# Nur Base64-Zeichen behalten: entfernt Zeilenumbrüche, Leerzeichen, Anführungszeichen und
# unsichtbare Zeichen, die beim Kopieren auf dem Handy oft mitkommen.
clean=$(printf '%s' "${KEYSTORE_BASE64:-}" | tr -cd 'A-Za-z0-9+/=')
length=${#clean}

if [ "$length" -lt 500 ]; then
  echo "::$level::SIGNING_KEYSTORE_BASE64 ist viel zu kurz ($length Zeichen). Es muss der komplette Inhalt von SIGNING_KEYSTORE_BASE64.txt sein (über 1000 Zeichen)." >&2
  exit 1
fi

if ! printf '%s' "$clean" | base64 -d > "$target" 2>/dev/null; then
  echo "::$level::SIGNING_KEYSTORE_BASE64 ist kein gültiger Base64-Text ($length Zeichen). Wahrscheinlich fehlt am Anfang oder Ende ein Stück. Bitte den kompletten Inhalt von SIGNING_KEYSTORE_BASE64.txt neu einfügen." >&2
  exit 1
fi

checksum=$(sha256sum "$target" | cut -c1-12)
echo "Schlüssel aus dem Secret: $length Zeichen, Prüfsumme $checksum" >&2

# keytool schreibt Fehlermeldungen auf stdout, daher beide Kanäle auswerten.
if output=$(keytool -list -keystore "$target" -storepass "${SIGNING_PASSWORD:-}" 2>&1); then
  :
elif printf '%s' "$output" | grep -qi "password was incorrect"; then
  echo "::$level::SIGNING_PASSWORD passt nicht zu diesem Schlüssel. Es muss genau das Passwort sein, das beim Erzeugen des Schlüssels als Secret gesetzt war." >&2
  exit 1
else
  echo "::$level::Der Text in SIGNING_KEYSTORE_BASE64 ist unvollständig oder beschädigt ($length Zeichen, Prüfsumme $checksum). Vergleiche mit Länge und Prüfsumme in ANLEITUNG.txt und füge den Text vollständig neu ein." >&2
  exit 1
fi

alias="${KEY_ALIAS:-}"
if [ -z "$alias" ]; then
  alias=$(keytool -list -v -keystore "$target" -storepass "$SIGNING_PASSWORD" 2>/dev/null | sed -n 's/^Alias name: //p' | head -n 1)
fi
if [ -z "$alias" ] || ! keytool -list -keystore "$target" -storepass "$SIGNING_PASSWORD" -alias "$alias" > /dev/null 2>&1; then
  echo "::$level::Im Schlüssel gibt es keinen Eintrag mit dem Alias \"$alias\". Bitte SIGNING_KEY_ALIAS löschen oder korrigieren." >&2
  exit 1
fi

printf '%s\n' "$alias"
