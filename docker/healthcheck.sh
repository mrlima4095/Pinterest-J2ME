#!/bin/sh
# Docker health check: the landing page returns {"ok":1,...} only when PHP can
# load the curl extension and both proxy scripts are present.
set -eu

body="$(php -r 'echo @file_get_contents("http://127.0.0.1/index.php?health=1");' 2>/dev/null || true)"

case "$body" in
  *'"ok":1'*) exit 0 ;;
  *) exit 1 ;;
esac
