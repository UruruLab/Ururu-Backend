#!/bin/bash

# RDS 연결 테스트 스크립트
RDS_HOST="ururu-rds-master.coqwxjz7zumt.ap-northeast-2.rds.amazonaws.com"
RDS_PORT="3306"
RDS_USER="ururuadmin"
RDS_PASSWORD="ururupassword"
DB_NAME="ururu"

echo "=== RDS 연결 테스트 시작 ==="
echo "RDS 엔드포인트: $RDS_HOST"
echo "포트: $RDS_PORT"
echo

# 1. DNS 해석 테스트
echo "1. DNS 해석 테스트..."
nslookup $RDS_HOST
echo

# 2. Ping 테스트
echo "2. Ping 테스트..."
ping -c 3 $RDS_HOST
echo

# 3. 포트 연결 테스트
echo "3. 포트 연결 테스트..."
timeout 10 bash -c "cat < /dev/null > /dev/tcp/$RDS_HOST/$RDS_PORT"
if [ $? -eq 0 ]; then
    echo "✅ 포트 $RDS_PORT 연결 성공"
else
    echo "❌ 포트 $RDS_PORT 연결 실패"
fi
echo

# 4. MySQL 클라이언트 연결 테스트
echo "4. MySQL 클라이언트 연결 테스트..."
if command -v mysql &> /dev/null; then
    mysql -h $RDS_HOST -P $RDS_PORT -u $RDS_USER -p$RDS_PASSWORD -e "SELECT 1 as test;" $DB_NAME
    if [ $? -eq 0 ]; then
        echo "✅ MySQL 연결 성공"
    else
        echo "❌ MySQL 연결 실패"
    fi
else
    echo "⚠️ MySQL 클라이언트가 설치되어 있지 않습니다."
fi
echo

# 5. 네트워크 정보 확인
echo "5. 네트워크 정보..."
echo "현재 EC2 IP:"
curl -s http://169.254.169.254/latest/meta-data/local-ipv4
echo
echo "현재 Public IP:"
curl -s http://169.254.169.254/latest/meta-data/public-ipv4
echo
echo

echo "=== 테스트 완료 ==="
