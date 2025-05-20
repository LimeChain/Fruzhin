#!/bin/zsh

exec java \
  -Dlogging.level.root=DEBUG \
  -Dgenesis.path.local=genesis/zombienet/westend-local-single-auth.json \
  -jar build/libs/fruzhin-0.1.0.jar "$@"