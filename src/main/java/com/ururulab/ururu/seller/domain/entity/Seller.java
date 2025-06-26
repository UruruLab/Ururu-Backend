package com.ururulab.ururu.seller.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationConstants;
import com.ururulab.ururu.seller.domain.policy.SellerPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "sellers", indexes = {
    @Index(name = "idx_seller_business_number", columnList = "businessNumber", unique = true),
    @Index(name = "idx_seller_name", columnList = "name", unique = true),
    @Index(name = "idx_seller_deleted_updated_at", columnList = "isDeleted, updatedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = SellerValidationConstants.NAME_MAX_LENGTH, nullable = false)
    private String name; // 브랜드명

    @Column(length = SellerValidationConstants.BUSINESS_NAME_MAX_LENGTH, nullable = false)
    private String businessName; // 사업자명

    @Column(length = SellerValidationConstants.OWNER_NAME_MAX_LENGTH, nullable = false)
    private String ownerName; // 대표 CEO명

    @Column(length = SellerValidationConstants.BUSINESS_NUMBER_LENGTH, nullable = false)
    private String businessNumber; // 사업자등록번호

    @Column(length = SellerValidationConstants.EMAIL_MAX_LENGTH, nullable = false, unique = true)
    private String email;

    @Column(length = SellerValidationConstants.PASSWORD_MAX_LENGTH, nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(length = SellerValidationConstants.PHONE_MAX_LENGTH, nullable = false)
    private String phone;

    @Column(length = SellerValidationConstants.IMAGE_MAX_LENGTH)
    private String image; // 브랜드 대표 이미지

    @Column(length = SellerValidationConstants.ADDRESS_MAX_LENGTH, nullable = false)
    private String address1;

    @Column(length = SellerValidationConstants.ADDRESS_MAX_LENGTH, nullable = false)
    private String address2;

    @Column(length = SellerValidationConstants.MAIL_ORDER_NUMBER_MAX_LENGTH, nullable = false)
    private String mailOrderNumber; // 통신판매업 신고번호

    @Column(nullable = false)
    private Boolean isDeleted = false;

    public static Seller of(
            String name,
            String businessName,
            String ownerName,
            String businessNumber,
            String email,
            String password,
            String phone,
            String image,
            String address1,
            String address2,
            String mailOrderNumber
    ) {
        // 도메인 무결성 검증
        SellerPolicy.validateName(name);
        SellerPolicy.validateBusinessName(businessName);
        SellerPolicy.validateOwnerName(ownerName);
        SellerPolicy.validateBusinessNumber(businessNumber);
        SellerPolicy.validateEmail(email);
        SellerPolicy.validatePassword(password);
        SellerPolicy.validatePhone(phone);
        SellerPolicy.validateAddress1(address1);
        SellerPolicy.validateAddress2(address2);
        SellerPolicy.validateMailOrderNumber(mailOrderNumber);

        Seller seller = new Seller();
        seller.name = name.trim();
        seller.businessName = businessName.trim();
        seller.ownerName = ownerName.trim();
        seller.businessNumber = businessNumber.trim();
        seller.email = email.trim();
        seller.password = password; // 암호화는 Service 레이어에서 처리
        seller.phone = phone;
        seller.image = image;
        seller.address1 = address1.trim();
        seller.address2 = address2 != null ? address2.trim() : "";
        seller.mailOrderNumber = mailOrderNumber.trim();
        return seller;
    }

    public void updateName(final String name) {
        SellerPolicy.validateName(name);
        this.name = name.trim();
    }

    public void updateBusinessName(final String businessName) {
        SellerPolicy.validateBusinessName(businessName);
        this.businessName = businessName.trim();
    }

    public void updateOwnerName(final String ownerName) {
        SellerPolicy.validateOwnerName(ownerName);
        this.ownerName = ownerName.trim();
    }

    public void updatePhone(final String phone) {
        SellerPolicy.validatePhone(phone);
        this.phone = phone;
    }

    public void updateImage(final String image) {
        this.image = image;
    }

    public void updateAddress(final String address1, final String address2) {
        SellerPolicy.validateAddress1(address1);
        SellerPolicy.validateAddress2(address2);
        this.address1 = address1.trim();
        this.address2 = address2 != null ? address2.trim() : "";
    }

    public void updateMailOrderNumber(final String mailOrderNumber) {
        SellerPolicy.validateMailOrderNumber(mailOrderNumber);
        this.mailOrderNumber = mailOrderNumber.trim();
    }

    public void delete() {
        this.isDeleted = true;
    }
}
