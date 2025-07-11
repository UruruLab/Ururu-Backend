#!/bin/bash

FAILED=$1  # ururu-blue or ururu-green
APP_DIR="/home/ec2-user/app"

if [ -z "$FAILED" ]; then
  echo "[ERROR] 실패한 컨테이너 이름을 입력해주세요. 예: ./rollback.sh ururu-green"
  exit 1
fi

if [ "$FAILED" == "ururu-green" ]; then
  ACTIVE="ururu-blue"
else
  ACTIVE="ururu-green"
fi

echo "[INFO] 실패한 컨테이너 중단: $FAILED"
docker rm -f $FAILED || true

echo "[INFO] Nginx 트래픽을 ${ACTIVE}으로 복원 중..."
bash $APP_DIR/nginx/switch.sh $ACTIVE

echo "[SUCCESS] 롤백 완료 - 현재 ${ACTIVE}이 서비스 중입니다."
