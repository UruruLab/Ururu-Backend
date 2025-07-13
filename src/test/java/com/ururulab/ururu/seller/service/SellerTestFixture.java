package com.ururulab.ururu.seller.service;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.dto.request.SellerSignupRequest;

import java.lang.reflect.Field;
import java.time.Instant;

/**
 * 판매자 테스트를 위한 공통 테스트 데이터 및 유틸리티 클래스.
 * 
 * 테스트 데이터 중앙화를 통해 객체 생성 방식 변경 시 한 곳에서만 수정 가능하며,
 * 테스트 코드의 가독성과 일관성을 향상시킵니다.
 */
public class SellerTestFixture {

    // === 정상 판매자 데이터 ===
    
    /**
     * 기본 판매자 엔티티 생성
     */
    public static Seller createSeller(Long id, String email, String name) {
        Seller seller = Seller.of(
                name,
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                email,
                "encodedPassword123",
                "01012345678",
                "https://example.com/image.jpg",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
        setSellerId(seller, id);
        return seller;
    }

    /**
     * 기본 판매자 엔티티 생성 (ID 없음)
     */
    public static Seller createSeller(String email, String name) {
        return Seller.of(
                name,
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                email,
                "encodedPassword123",
                "01012345678",
                "https://example.com/image.jpg",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 유효한 회원가입 요청 생성
     */
    public static SellerSignupRequest createValidSignupRequest() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "seller@ururu.shop",
                "Password123!",
                "01012345678",
                "https://example.com/image.jpg",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 이미지 없는 회원가입 요청 생성
     */
    public static SellerSignupRequest createValidSignupRequestWithoutImage() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "seller@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    // === 예외 케이스 데이터 ===

    /**
     * 중복 이메일이 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createDuplicateEmailRequest() {
        return new SellerSignupRequest(
                "다른 브랜드",
                "다른 회사(주)",
                "이상민",
                "0987654321",
                "existing@ururu.shop", // 중복 이메일
                "Password123!",
                "01098765432",
                null,
                "12345",
                "부산시 해운대구 센텀로 456",
                "789호",
                "2024-부산해운대-5678"
        );
    }

    /**
     * 중복 사업자등록번호가 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createDuplicateBusinessNumberRequest() {
        return new SellerSignupRequest(
                "다른 브랜드",
                "다른 회사(주)",
                "이상민",
                "1234567890", // 중복 사업자등록번호
                "different@ururu.shop",
                "Password123!",
                "01098765432",
                null,
                "12345",
                "부산시 해운대구 센텀로 456",
                "789호",
                "2024-부산해운대-5678"
        );
    }

    /**
     * 중복 브랜드명이 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createDuplicateNameRequest() {
        return new SellerSignupRequest(
                "우르르 뷰티", // 중복 브랜드명
                "다른 회사(주)",
                "이상민",
                "0987654321",
                "different@ururu.shop",
                "Password123!",
                "01098765432",
                null,
                "12345",
                "부산시 해운대구 센텀로 456",
                "789호",
                "2024-부산해운대-5678"
        );
    }

    // === 삭제된 판매자 데이터 ===

    /**
     * 삭제된 판매자 엔티티 생성
     */
    public static Seller createDeletedSeller(Long id, String email, String name) {
        Seller seller = createSeller(id, email, name);
        seller.delete();
        return seller;
    }

    /**
     * 삭제된 판매자 엔티티 생성 (ID 없음)
     */
    public static Seller createDeletedSeller(String email, String name) {
        Seller seller = createSeller(email, name);
        seller.delete();
        return seller;
    }

    // === 경계 조건 데이터 ===

    /**
     * 이메일 대소문자 변이가 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithEmailCaseVariation() {
        return new SellerSignupRequest(
                "테스트 브랜드",
                "테스트 회사(주)",
                "테스트",
                "1111111111",
                "TEST@URURU.SHOP", // 대문자 이메일
                "Password123!",
                "01011111111",
                null,
                "12345",
                "서울시 강남구 테스트로 111",
                "111호",
                "2024-서울강남-1111"
        );
    }

    /**
     * 공백이 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithWhitespace() {
        return new SellerSignupRequest(
                "  우르르 뷰티  ", // 앞뒤 공백
                "  우르르 뷰티(주)  ",
                "  김태현  ",
                "  1234567890  ",
                "  seller@ururu.shop  ",
                "Password123!",
                "  01012345678  ",
                null,
                "12345",
                "  서울시 강남구 테헤란로 123  ",
                "  456호  ",
                "  2024-서울강남-1234  "
        );
    }

    /**
     * 최대 길이 경계값을 포함한 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithMaxLength() {
        return new SellerSignupRequest(
                "A".repeat(50), // 최대 길이
                "B".repeat(100), // 최대 길이
                "C".repeat(50), // 최대 길이
                "1234567890",
                "maxlength@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "D".repeat(100), // 최대 길이
                "E".repeat(100), // 최대 길이
                "F".repeat(50) // 최대 길이
        );
    }

    /**
     * 최소 길이 경계값을 포함한 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithMinLength() {
        return new SellerSignupRequest(
                "A", // 최소 길이
                "B", // 최소 길이
                "C", // 최소 길이
                "1234567890",
                "min@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "D", // 최소 길이
                "E", // 최소 길이
                "F" // 최소 길이
        );
    }

    // === 특수 케이스 데이터 ===

    /**
     * 특수문자가 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithSpecialCharacters() {
        return new SellerSignupRequest(
                "우르르 뷰티 & 코스메틱",
                "우르르 뷰티(주) - 서울지점",
                "김태현 (CEO)",
                "1234567890",
                "seller+test@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123, 456동",
                "456호 (우르르빌딩)",
                "2024-서울강남-1234 (통신판매업)"
        );
    }

    /**
     * 한글이 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithKoreanCharacters() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티 주식회사",
                "김태현",
                "1234567890",
                "seller@ururu-korean.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울특별시 강남구 테헤란로 123",
                "456호 (우르르빌딩)",
                "2024-서울강남-1234"
        );
    }

    /**
     * 빈 필드가 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithEmptyFields() {
        return new SellerSignupRequest(
                "", // 빈 브랜드명
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "seller@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 잘못된 이메일 형식이 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithInvalidEmail() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "invalid-email", // 잘못된 이메일 형식
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 대용량 데이터가 포함된 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithLargeData() {
        return new SellerSignupRequest(
                "A".repeat(50), // 최대 길이 브랜드명
                "B".repeat(100), // 최대 길이 회사명
                "C".repeat(50), // 최대 길이 대표자명
                "1234567890",
                "large@ururu.shop",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "D".repeat(100), // 최대 길이 주소
                "E".repeat(100), // 최대 길이 상세주소
                "F".repeat(50) // 최대 길이 통신판매업번호
        );
    }

    /**
     * 이메일 최대 길이(100자) 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithMaxEmailLength() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "a".repeat(48) + "@" + "b".repeat(47) + ".com",
                "Password123!",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 비밀번호 최대 길이(50자) 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithMaxPasswordLength() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "maxpassword@ururu.shop",
                "A".repeat(25) + "a".repeat(20) + "1!@#$",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    /**
     * 이메일/비밀번호 모두 최대 길이 회원가입 요청 생성
     */
    public static SellerSignupRequest createRequestWithMaxEmailAndPasswordLength() {
        return new SellerSignupRequest(
                "우르르 뷰티",
                "우르르 뷰티(주)",
                "김태현",
                "1234567890",
                "a".repeat(48) + "@" + "b".repeat(47) + ".com",
                "A".repeat(25) + "a".repeat(20) + "1!@#$",
                "01012345678",
                null,
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                "2024-서울강남-1234"
        );
    }

    // === 유틸리티 메서드 ===

    /**
     * 판매자 ID를 리플렉션을 통해 설정
     */
    private static void setSellerId(Seller seller, Long id) {
        try {
            Field idField = Seller.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(seller, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set seller id for test", e);
        }
    }

    /**
     * 판매자 생성/수정 시간을 리플렉션을 통해 설정
     */
    public static void setSellerTimestamps(Seller seller, Instant createdAt, Instant updatedAt) {
        try {
            // BaseEntity를 명시적으로 참조하여 안정성 향상
            Field createdAtField = BaseEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(seller, createdAt);

            Field updatedAtField = BaseEntity.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(seller, updatedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set seller timestamps for test", e);
        }
    }

    /**
     * 판매자 삭제 상태를 리플렉션을 통해 설정
     */
    public static void setSellerDeleted(Seller seller, boolean isDeleted) {
        try {
            Field isDeletedField = Seller.class.getDeclaredField("isDeleted");
            isDeletedField.setAccessible(true);
            isDeletedField.set(seller, isDeleted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set seller deleted status for test", e);
        }
    }
} 