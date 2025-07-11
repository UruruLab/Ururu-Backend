#!/bin/bash

TARGET=$1  # ururu-blue / ururu-green

if [ -z "$TARGET" ]; then
  echo "[ERROR] 대상 컨테이너 이름이 필요합니다. 예: ./switch.sh ururu-green"
  exit 1
fi

echo "[INFO] Nginx 설정을 ${TARGET}으로 전환합니다..."
echo "TARGET_CONTAINER=$TARGET" > .nginx-env
export TARGET_CONTAINER=$TARGET 
envsubst '$TARGET_CONTAINER' < nginx/nginx.conf.template > nginx/nginx.conf

docker exec ururu-nginx nginx -s reload || {
  echo "[ERROR] Nginx reload 실패"; exit 1;
}
echo "[SUCCESS] 트래픽이 ${TARGET}으로 전환되었습니다."