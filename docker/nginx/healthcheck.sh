#!/bin/bash

PORT=$1
RETRIES=10
SLEEP_SECONDS=3

if [ -z "$PORT" ]; then
  echo "[ERROR] 포트 번호를 입력해주세요. 예: ./healthcheck.sh 8082"
  exit 1
fi

echo "[INFO] Health check 시작 (http://localhost:$PORT/health)"

for i in $(seq 1 $RETRIES); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/health)
  if [ "$STATUS" == "200" ]; then
    echo "[SUCCESS] Health check 성공 (포트: $PORT)"
    exit 0
  fi
  echo "[INFO] Health check 실패 ($i/$RETRIES)... 재시도 대기 중"
  sleep $SLEEP_SECONDS
done

echo "[ERROR] Health check 실패 - 최대 재시도 초과"
exit 1