package com.ururulab.ururu.user.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "ShippingAddress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 30)
    private String label;

    @Column(length = 20)
    private String phone;

    @Column(length = 5)
    private String zonecode;

    @Column(length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

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
}
