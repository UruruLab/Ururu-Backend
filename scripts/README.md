# Scripts

이 디렉토리에는 프로젝트 운영에 필요한 스크립트들이 포함되어 있습니다.

## test-rds-connection.sh

RDS 데이터베이스 연결을 테스트하는 스크립트입니다.

### 사용법

```bash
# 환경변수 설정 후 실행
export RDS_HOST="your-rds-endpoint"
export RDS_PORT="3306"
export RDS_USER="your-username"
export RDS_PASSWORD="your-password"
export DB_NAME="your-database"

./scripts/test-rds-connection.sh
```

또는 한 줄로:

```bash
RDS_HOST=your-host RDS_USER=your-user RDS_PASSWORD=your-password ./scripts/test-rds-connection.sh
```

### 테스트 항목

1. DNS 해석 테스트
2. Ping 테스트
3. 포트 연결 테스트
4. MySQL 클라이언트 연결 테스트
5. 네트워크 정보 확인

### 주의사항

- **절대로 스크립트에 비밀번호를 하드코딩하지 마세요!**
- 환경변수나 별도의 설정 파일을 사용하세요
- 프로덕션 환경에서는 더욱 주의해서 사용하세요
