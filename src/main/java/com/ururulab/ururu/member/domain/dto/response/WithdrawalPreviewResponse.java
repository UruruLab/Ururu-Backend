package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WithdrawalPreviewResponse(
        @JsonProperty("member_info") MemberInfo memberInfo,
        @JsonProperty("loss_info") LossInfo lossInfo
) {
    public static WithdrawalPreviewResponse of(
            final MemberInfo memberInfo,
            final LossInfo lossInfo
    ) {
        return new WithdrawalPreviewResponse(memberInfo, lossInfo);
    }

    public record MemberInfo(
            String nickname,
            String email,
            @JsonProperty("join_date") String joinDate,
            @JsonProperty("profile_image") String profileImage
    ) {
        public static MemberInfo of(
                final String nickname,
                final String email,
                final String joinDate,
                final String profileImage
        ) {
            return new MemberInfo(nickname, email, joinDate, profileImage);
        }
    }

    public record LossInfo(
            int points,
            @JsonProperty("active_orders") int activeOrders,
            @JsonProperty("review_count") int reviewCount,
            @JsonProperty("beauty_profile_exists") boolean beautyProfileExists,
            @JsonProperty("shipping_addresses_count") int shippingAddressesCount,
            @JsonProperty("member_agreements_count") int memberAgreementsCount,
            @JsonProperty("cart_items_count") int cartItemsCount,
            @JsonProperty("point_transactions_count") int pointTransactionsCount
    ) {
        public static LossInfo of(
                final int points,
                final int activeOrders,
                final int reviewCount,
                final boolean beautyProfileExists,
                final int shippingAddressesCount,
                final int memberAgreementsCount,
                final int cartItemsCount,
                final int pointTransactionsCount
        ) {
            return new LossInfo(
                    points,
                    activeOrders,
                    reviewCount,
                    beautyProfileExists,
                    shippingAddressesCount,
                    memberAgreementsCount,
                    cartItemsCount,
                    pointTransactionsCount
            );
        }


        public static LossInfo ofSimple(
                final int points,
                final int activeOrders,
                final int reviewCount
        ) {
            return new LossInfo(points, activeOrders, reviewCount, false, 0, 0, 0, 0);
        }
    }
}