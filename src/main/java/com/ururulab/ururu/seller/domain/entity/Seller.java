package com.ururulab.ururu.seller.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationConstants;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "Seller")
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

    @Column(length = 255, nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(length = SellerValidationConstants.PHONE_MAX_LENGTH)
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
        Seller seller = new Seller();
        seller.name = name;
        seller.businessName = businessName;
        seller.ownerName = ownerName;
        seller.businessNumber = businessNumber;
        seller.email = email;
        seller.password = password;
        seller.phone = phone;
        seller.image = image;
        seller.address1 = address1;
        seller.address2 = address2;
        seller.mailOrderNumber = mailOrderNumber;
        return seller;
    }

    public void updateName(final String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다.");
        }
        this.name = name.trim();
    }

    public void updateBusinessName(final String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("사업자명은 필수입니다.");
        }
        this.businessName = businessName.trim();
    }

    public void updateOwnerName(final String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("대표 CEO명은 필수입니다.");
        }
        this.ownerName = ownerName.trim();
    }

    public void updatePhone(final String phone) {
        this.phone = phone;
    }

    public void updateImage(final String image) {
        this.image = image;
    }

    public void updateAddress(final String address1, final String address2) {
        if (address1 == null || address1.trim().isEmpty()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        this.address1 = address1.trim();
        this.address2 = address2 != null ? address2.trim() : "";
    }

    public void updateMailOrderNumber(final String mailOrderNumber) {
        if (mailOrderNumber == null || mailOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("통신판매업 신고번호는 필수입니다.");
        }
        this.mailOrderNumber = mailOrderNumber.trim();
    }

    public void delete() {
        this.isDeleted = true;
    }
}