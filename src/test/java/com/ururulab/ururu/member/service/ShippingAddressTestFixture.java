package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.dto.request.ShippingAddressRequest;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ShippingAddressTestFixture {

    public static Member createMember(Long id) {
        Member member = Member.of(
                "testUser",
                "test@example.com",
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                LocalDate.parse("1990-01-01"),
                "01012345678",
                "profile.jpg",
                Role.NORMAL
        );
        setMemberId(member, id);
        return member;
    }

    public static ShippingAddressRequest createValidRequest() {
        return new ShippingAddressRequest(
                "집",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                false
        );
    }

    public static ShippingAddressRequest createDefaultRequest() {
        return new ShippingAddressRequest(
                "집",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                true
        );
    }

    public static ShippingAddressRequest createUpdateRequest() {
        return new ShippingAddressRequest(
                "새집",
                "01098765432",
                "54321",
                "부산시 해운대구 센텀로 456",
                "789호",
                false
        );
    }

    public static ShippingAddress createShippingAddress(Member member) {
        ShippingAddress address = ShippingAddress.of(
                member,
                "집",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                false
        );
        setAddressId(address, 1L);
        return address;
    }

    public static ShippingAddress createDefaultShippingAddress(Member member) {
        ShippingAddress address = ShippingAddress.of(
                member,
                "집",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                true
        );
        setAddressId(address, 1L);
        return address;
    }

    public static ShippingAddress createNewDefaultShippingAddress(Member member) {
        ShippingAddress address = ShippingAddress.of(
                member,
                "새 기본 배송지",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                true
        );
        setAddressId(address, 2L);
        return address;
    }

    public static List<ShippingAddress> createShippingAddressList(Member member) {
        ShippingAddress home = ShippingAddress.of(
                member,
                "집",
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                true
        );
        setAddressId(home, 1L);

        ShippingAddress company = ShippingAddress.of(
                member,
                "회사",
                "0212345678",
                "67890",
                "서울시 종로구 종로 789",
                "10층",
                false
        );
        setAddressId(company, 2L);

        ShippingAddress parents = ShippingAddress.of(
                member,
                "부모님집",
                "01098765432",
                "54321",
                "경기도 수원시 영통구 광교로 456",
                "101동 1001호",
                false
        );
        setAddressId(parents, 3L);

        return Arrays.asList(home, company, parents);
    }

    public static List<ShippingAddress> createMaxLimitAddressList(Member member) {
        return Arrays.asList(
                createShippingAddressWithId(member, 1L, "집", true),
                createShippingAddressWithId(member, 2L, "회사", false),
                createShippingAddressWithId(member, 3L, "부모님집", false),
                createShippingAddressWithId(member, 4L, "친구집", false),
                createShippingAddressWithId(member, 5L, "카페", false)
        );
    }

    private static ShippingAddress createShippingAddressWithId(Member member, Long id, String label, boolean isDefault) {
        ShippingAddress address = ShippingAddress.of(
                member,
                label,
                "01012345678",
                "12345",
                "서울시 강남구 테헤란로 123",
                "456호",
                isDefault
        );
        setAddressId(address, id);
        return address;
    }

    public static ShippingAddressRequest createSetAsDefaultRequest() {
        return new ShippingAddressRequest(
                "업데이트된 집",
                "01098765432",
                "54321",
                "부산시 해운대구 센텀로 456",
                "789호",
                true  // 기본 배송지로 설정
        );
    }

    private static void setMemberId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set member id for test", e);
        }
    }

    private static void setAddressId(ShippingAddress address, Long id) {
        try {
            Field idField = ShippingAddress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(address, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set address id for test", e);
        }
    }
}