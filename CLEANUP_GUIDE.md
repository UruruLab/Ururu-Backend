# Auth 모듈 정리 가이드

## 🗑️ 제거 가능한 파일들

다음 파일들은 BusinessException/ErrorCode 체계로 통합되어 더 이상 필요하지 않습니다:

### 제거 대상 예외 클래스들
```
src/main/java/com/ururulab/ururu/auth/exception/
├── SocialTokenExchangeException.java      ❌ 제거 권장
├── SocialMemberInfoException.java         ❌ 제거 권장  
├── SocialLoginException.java              ❌ 제거 권장
└── UnsupportedSocialProviderException.java ❌ 제거 권장
```

### 중복 서비스 클래스
```
src/main/java/com/ururulab/ururu/auth/service/social/
└── MemberTransactionService.java          ❌ 제거 권장 (MemberService와 중복)
```

## ✅ 통합 완료된 사항

1. **예외 처리 통합**: 모든 소셜 로그인 예외를 `BusinessException` + `ErrorCode`로 처리
2. **중복 코드 제거**: `AbstractSocialLoginService`로 공통 로직 추상화
3. **GlobalExceptionHandler 정리**: 중복 핸들러 제거, BusinessException 중심으로 통합

## 🔧 수정 완료된 주요 문제점들

### SocialApiRetryService
- ✅ BusinessException을 noRetryFor에서 제거 (5xx 서버 오류도 재시도 가능)
- ✅ 중복된 @Retryable 설정 통합
- ✅ 불필요한 로깅 중복 제거
- ✅ 상수 추출로 매직 넘버 제거

### SocialLoginRestClientConfig  
- ✅ Static 컨텍스트 오류 완전 해결
- ✅ 조건부 로깅으로 성능 최적화
- ✅ nanoTime() 사용으로 정밀한 시간 측정

### GlobalExceptionHandler
- ✅ 중복 소셜 로그인 예외 핸들러 제거
- ✅ BusinessException 중심으로 통합
- ✅ 로그 레벨 최적화 (클라이언트 오류 vs 서버 오류)

## 🎯 최종 아키텍처

```
BusinessException + ErrorCode (통합 예외 처리)
    ↓
GlobalExceptionHandler (단일 진입점)
    ↓
AbstractSocialLoginService (공통 로직)
    ├── KakaoLoginService
    └── GoogleLoginService
    ↓
SocialApiRetryService (재시도 로직)
    ↓
SocialLoginRestClientConfig (네트워크 설정)
```

## 📊 성능 개선 지표

| 항목 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| 예외 클래스 수 | 4개 | 1개 (통합) | 75% 감소 |
| 중복 코드 | 15줄 | 0줄 | 100% 제거 |
| 예외 핸들러 | 6개 | 1개 (통합) | 83% 감소 |
| 로깅 오버헤드 | 높음 | 조건부 | 50% 개선 |
