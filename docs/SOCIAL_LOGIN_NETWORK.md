# 🌐 소셜 로그인 네트워크 안정성 설정

## 개요

외부 소셜 API(카카오, 구글) 연동의 안정성을 위해 다음과 같은 네트워크 설정을 적용했습니다:

- **Connection Timeout**: 5초
- **Read Timeout**: 10초  
- **Retry Policy**: 최대 3회, Exponential Backoff
- **Connection Pool**: 최대 50개 연결

## 🔧 설정 구조

### RestClient Bean 구성

```java
@Bean("socialLoginRestClient")
public RestClient socialLoginRestClient(
    final HttpComponentsClientHttpRequestFactory socialLoginHttpRequestFactory) {
    // 소셜 로그인 전용 RestClient 설정
}
```

### 주요 설정값

| 설정 항목 | 값 | 설명 |
|----------|----|----|
| Connection Timeout | 5초 | 외부 API 서버 연결 대기시간 |
| Read Timeout | 10초 | 응답 데이터 읽기 대기시간 |
| Write Timeout | 10초 | 요청 데이터 전송 대기시간 |
| Connection Request Timeout | 3초 | 커넥션 풀에서 연결 획득 대기시간 |
| Max Total Connections | 50개 | 전체 커넥션 풀 크기 |
| Max Connections Per Route | 10개 | 라우트당 최대 연결 수 |

## 🔄 Retry 정책

### SocialApiRetryService

```java
@Retryable(
    retryFor = {
        ResourceAccessException.class,      // 네트워크 연결 오류
        SocketTimeoutException.class,       // 소켓 타임아웃
        ConnectException.class              // 연결 실패
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000L, multiplier = 2.0, maxDelay = 8000L)
)
```

### 재시도 시나리오

1. **1차 시도**: 즉시 실행
2. **2차 시도**: 1초 후 실행
3. **3차 시도**: 2초 후 실행 (실패 시 최종 포기)

## 📊 에러 처리

### HTTP 상태 코드별 처리

- **4xx 클라이언트 오류**: `SOCIAL_API_CLIENT_ERROR`
- **5xx 서버 오류**: `SOCIAL_API_SERVER_ERROR`
- **네트워크 오류**: `SOCIAL_API_NETWORK_ERROR`

## 🧪 테스트

### 설정 검증 테스트

```bash
./gradlew test --tests SocialLoginRestClientConfigTest
```

## 📋 모니터링

### 주요 지표

- **응답 시간**: 평균 200ms 이하 목표
- **성공률**: 99% 이상 목표
- **재시도율**: 5% 이하 목표

## 🏆 베스트 프랙티스

### 1. 민감 정보 마스킹

```java
private String maskSensitiveData(final String data) {
    if (data == null || data.length() <= 10) {
        return "***";
    }
    return data.substring(0, 10) + "...";
}
```

### 2. 예외 처리 패턴

```java
try {
    return socialApiRetryService.executeTokenExchange(
        () -> requestAccessToken(code), "KAKAO");
} catch (BusinessException e) {
    throw e;
} catch (Exception e) {
    throw new BusinessException(ErrorCode.SOCIAL_API_NETWORK_ERROR);
}
```

---

> **⚠️ 주의사항**
> 
> - 타임아웃 설정 변경 시 사용자 경험을 고려해야 합니다
> - 커넥션 풀 크기는 서버 리소스를 고려하여 설정하세요
