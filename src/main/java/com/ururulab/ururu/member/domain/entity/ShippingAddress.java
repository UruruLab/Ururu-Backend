package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.member.controller.dto.validation.ShippingAddressValidationConstants;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "shipping_address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = ShippingAddressValidationConstants.LABEL_MAX_LENGTH, nullable = false)
    private String label;

    @Column(length = ShippingAddressValidationConstants.PHONE_MAX_LENGTH, nullable = false)
    private String phone;

    @Column(length = ShippingAddressValidationConstants.ZONECODE_LENGTH, nullable = false)
    private String zonecode;

    @Column(nullable = false)
    private String address1;

    private String address2;

    @Column(nullable = false)
    private boolean isDefault;

    public static ShippingAddress of(
            Member member,
            String label,
            String phone,
            String zonecode,
            String address1,
            String address2,
            boolean isDefault
    ) {
        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.member = member;
        shippingAddress.label = label;
        shippingAddress.phone = phone;
        shippingAddress.zonecode = zonecode;
        shippingAddress.address1 = address1;
        shippingAddress.address2 = address2;
        shippingAddress.isDefault = isDefault;
        return shippingAddress;
    }

    public void updateAddress(
            String label,
            String phone,
            String zonecode,
            String address1,
            String address2,
            boolean isDefault
    ) {
        this.label = label;
        this.phone = phone;
        this.zonecode = zonecode;
        this.address1 = address1;
        this.address2 = address2;
        this.isDefault = isDefault;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetAsDefault() {
        this.isDefault = false;
    }
}
