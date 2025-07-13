package com.ururulab.ururu.seller.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import com.ururulab.ururu.seller.dto.request.SellerSignupRequest;
import com.ururulab.ururu.seller.dto.response.SellerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 판매자 회원가입 서비스 테스트.
 * 
 * Repository 계층만 Mock 처리하고 실제 비즈니스 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("판매자 회원가입 서비스 테스트")
class SellerSignupServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SellerService sellerService;

    @Nested
    @DisplayName("정상적인 판매자 회원가입")
    class SellerSignupSuccessTest {

        @Test
        @DisplayName("정상적인 판매자 회원가입 성공")
        void signup_success() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("seller@ururu.shop");
            assertThat(response.name()).isEqualTo("우르르 뷰티");

            // 비밀번호 암호화 검증
            verify(passwordEncoder).encode("Password123!");
            
            // Repository 메서드 호출 검증
            verify(sellerRepository).isEmailAvailable("seller@ururu.shop");
            verify(sellerRepository).isBusinessNumberAvailable("1234567890");
            verify(sellerRepository).isNameAvailable("우르르 뷰티");
            verify(sellerRepository).save(any(Seller.class));
        }

        @Test
        @DisplayName("이미지 없는 판매자 회원가입 성공")
        void signup_successWithoutImage() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequestWithoutImage();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("seller@ururu.shop");
            assertThat(response.name()).isEqualTo("우르르 뷰티");
        }
    }

    @Nested
    @DisplayName("이메일 중복 시 회원가입 실패")
    class SellerSignupEmailDuplicateTest {

        @Test
        @DisplayName("이메일 중복 시 회원가입 실패")
        void signup_duplicateEmail_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            // 이메일 중복 시 첫 번째 검증에서 예외가 발생하므로 다른 검증은 호출되지 않음
            verify(sellerRepository).isEmailAvailable("seller@ururu.shop");
        }

        @Test
        @DisplayName("이메일 대소문자 변이 시 정규화 후 중복 체크")
        void signup_emailCaseVariation_normalizedAndChecked() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithEmailCaseVariation();

            given(sellerRepository.isEmailAvailable("test@ururu.shop")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            // 이메일이 소문자로 정규화되어 체크되는지 검증
            verify(sellerRepository).isEmailAvailable("test@ururu.shop");
        }

        @Test
        @DisplayName("이메일 공백 제거 후 중복 체크")
        void signup_emailWithWhitespace_trimmedAndChecked() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithWhitespace();

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            // 공백이 제거된 이메일로 체크되는지 검증
            verify(sellerRepository).isEmailAvailable("seller@ururu.shop");
        }
    }

    @Nested
    @DisplayName("사업자등록번호 중복 시 회원가입 실패")
    class SellerSignupBusinessNumberDuplicateTest {

        @Test
        @DisplayName("사업자등록번호 중복 시 회원가입 실패")
        void signup_duplicateBusinessNumber_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createDuplicateBusinessNumberRequest();

            given(sellerRepository.isEmailAvailable("different@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_BUSINESS_NUMBER);

            verify(sellerRepository).isEmailAvailable("different@ururu.shop");
            verify(sellerRepository).isBusinessNumberAvailable("1234567890");
        }
    }

    @Nested
    @DisplayName("브랜드명 중복 시 회원가입 실패")
    class SellerSignupNameDuplicateTest {

        @Test
        @DisplayName("브랜드명 중복 시 회원가입 실패")
        void signup_duplicateName_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createDuplicateNameRequest();

            given(sellerRepository.isEmailAvailable("different@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("0987654321")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_BRAND_NAME);

            verify(sellerRepository).isEmailAvailable("different@ururu.shop");
            verify(sellerRepository).isBusinessNumberAvailable("0987654321");
            verify(sellerRepository).isNameAvailable("우르르 뷰티");
        }
    }

    @Nested
    @DisplayName("DB 제약조건 위반 시 회원가입 실패")
    class SellerSignupDataIntegrityTest {

        @Test
        @DisplayName("DB 제약조건 위반 시 회원가입 실패")
        void signup_dataIntegrityViolation_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willThrow(new DataIntegrityViolationException("Duplicate entry"));

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            verify(sellerRepository).save(any(Seller.class));
        }
    }

    @Nested
    @DisplayName("경계 조건 테스트")
    class SellerSignupBoundaryTest {

        @Test
        @DisplayName("최대 길이 경계값으로 회원가입 성공")
        void signup_maxLengthBoundary_success() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithMaxLength();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "maxlength@ururu.shop", "A".repeat(50));
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("maxlength@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("A".repeat(50))).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("A".repeat(50));
        }

        @Test
        @DisplayName("최소 길이 경계값으로 회원가입 성공")
        void signup_minLengthBoundary_success() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithMinLength();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "min@ururu.shop", "A");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("min@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("A")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("A");
        }

        @Test
        @DisplayName("특수문자가 포함된 회원가입 성공")
        void signup_specialCharacters_success() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithSpecialCharacters();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller+test@ururu.shop", "우르르 뷰티 & 코스메틱");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller+test@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티 & 코스메틱")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("우르르 뷰티 & 코스메틱");
            assertThat(response.email()).isEqualTo("seller+test@ururu.shop");
        }


    }

    @Nested
    @DisplayName("이메일 정규화 검증")
    class SellerSignupEmailNormalizationTest {

        @Test
        @DisplayName("이메일 대소문자 정규화 검증")
        void signup_emailCaseNormalization_verified() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithEmailCaseVariation();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "test@ururu.shop", "테스트 브랜드");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("test@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1111111111")).willReturn(true);
            given(sellerRepository.isNameAvailable("테스트 브랜드")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("test@ururu.shop"); // 소문자로 정규화됨

            // 정규화된 이메일로 중복 체크가 수행되는지 검증
            verify(sellerRepository).isEmailAvailable("test@ururu.shop");
        }

        @Test
        @DisplayName("이메일 공백 제거 정규화 검증")
        void signup_emailWhitespaceNormalization_verified() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithWhitespace();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true); // 정규화된 값
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true); // 정규화된 값
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            SellerResponse response = sellerService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("seller@ururu.shop"); // 공백 제거됨

            // 정규화된 값으로 중복 체크가 수행되는지 검증
            verify(sellerRepository).isEmailAvailable("seller@ururu.shop");
            verify(sellerRepository).isBusinessNumberAvailable("1234567890"); // 정규화된 값
            verify(sellerRepository).isNameAvailable("우르르 뷰티"); // 정규화된 값
        }
    }

    @Nested
    @DisplayName("비밀번호 암호화 검증")
    class SellerSignupPasswordEncryptionTest {

        @Test
        @DisplayName("비밀번호 암호화 검증")
        void signup_passwordEncryption_verified() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티");
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            sellerService.signup(request);

            // Then
            // PasswordEncoder가 호출되는지 검증
            verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("다른 비밀번호로 암호화 검증")
        void signup_differentPasswordEncryption_verified() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();
            Seller savedSeller = SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티");
            String encodedPassword = "differentEncodedPassword456";

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(savedSeller);

            // When
            sellerService.signup(request);

            // Then
            // PasswordEncoder가 올바른 비밀번호로 호출되는지 검증
            verify(passwordEncoder).encode("Password123!");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class SellerSignupExceptionTest {

        @Test
        @DisplayName("null 요청 시 예외 발생")
        void signup_nullRequest_throwsException() {
            // When & Then
            assertThatThrownBy(() -> sellerService.signup(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ARGUMENT);
        }

        @Test
        @DisplayName("빈 문자열 필드 시 예외 발생")
        void signup_emptyFields_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithEmptyFields();

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ARGUMENT);
        }

        @Test
        @DisplayName("잘못된 이메일 형식 시 예외 발생")
        void signup_invalidEmailFormat_throwsException() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithInvalidEmail();

            // When & Then
            assertThatThrownBy(() -> sellerService.signup(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("회원가입 기본 동작 검증")
    class SellerSignupBasicValidationTest {

        @Test
        @DisplayName("회원가입 기본 동작 검증")
        void signup_basicValidation_success() throws InterruptedException {
            // Given
            SellerSignupRequest request = SellerTestFixture.createValidSignupRequest();
            String encodedPassword = "encodedPassword123";
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            given(sellerRepository.isEmailAvailable("seller@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("우르르 뷰티")).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            
            // 첫 번째 save는 성공, 나머지는 DataIntegrityViolationException 발생
            given(sellerRepository.save(any(Seller.class)))
                .willReturn(SellerTestFixture.createSeller(1L, "seller@ururu.shop", "우르르 뷰티"))
                .willThrow(new DataIntegrityViolationException("Duplicate entry"));

            // When
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        sellerService.signup(request);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        }
    }

    @Nested
    @DisplayName("성능 테스트")
    class SellerSignupPerformanceTest {

        @Test
        @DisplayName("대용량 데이터 처리 성능 테스트")
        void signup_largeData_performanceTest() {
            // Given
            SellerSignupRequest request = SellerTestFixture.createRequestWithLargeData();
            String encodedPassword = "encodedPassword123";

            given(sellerRepository.isEmailAvailable("large@ururu.shop")).willReturn(true);
            given(sellerRepository.isBusinessNumberAvailable("1234567890")).willReturn(true);
            given(sellerRepository.isNameAvailable("A".repeat(50))).willReturn(true);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(sellerRepository.save(any(Seller.class))).willReturn(SellerTestFixture.createSeller(1L, "large@ururu.shop", "A".repeat(50)));

            // When
            long startTime = System.currentTimeMillis();
            SellerResponse response = sellerService.signup(request);
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(response).isNotNull();
            assertThat(endTime - startTime).isLessThan(1000); // 1초 이내 처리
        }
    }
} 