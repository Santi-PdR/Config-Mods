#!/usr/bin/env bash
# Instala Tan's Huge Trees (1.20.1 Forge) en una instancia, verificando tamaño y SHA-256.
# Uso: ./install-tht.sh /ruta/a/instancia [archivo-local.jar]
set -euo pipefail

INSTANCE="${1:?Uso: $0 /ruta/a/instancia [jar-local]}"
LOCAL_FILE="${2:-}"

N='TansHugeTrees-1.8.0-Forge-1.20.1-configmods.jar'
SIZE=64550946
SHA='2ea601b22fb1730fe8b463459f4d491783207c362ae3b346169bcf360ef3e6e3'
URL="https://github.com/Santi-PdR/Config-Mods/releases/download/v1.1-configmods/${N}"

MODS="${INSTANCE%/}/mods"
mkdir -p "$MODS"

if [[ -n "$LOCAL_FILE" ]]; then
  TMP="$LOCAL_FILE"
else
  TMP="${TMPDIR:-/tmp}/$N"
  echo "Descargando $N ..."
  curl -fL --retry 3 -o "$TMP" "$URL"
fi

ACTUAL_SIZE=$(wc -c < "$TMP" | tr -d ' ')
if [[ "$ACTUAL_SIZE" != "$SIZE" ]]; then
  echo "TAMAÑO MAL: esperado $SIZE, obtenido $ACTUAL_SIZE" >&2; exit 1
fi

ACTUAL_SHA=$(sha256sum "$TMP" | cut -d' ' -f1)
if [[ "$ACTUAL_SHA" != "$SHA" ]]; then
  echo "HASH MAL: esperado $SHA, obtenido $ACTUAL_SHA" >&2; exit 1
fi
echo "OK tamano=$ACTUAL_SIZE sha256=$ACTUAL_SHA"

find "$MODS" -maxdepth 1 -name 'TansHugeTrees-*.jar' -delete
if [[ "$TMP" != "$MODS/$N" ]]; then mv -f "$TMP" "$MODS/$N"; fi
echo "Instalado: $MODS/$N"
