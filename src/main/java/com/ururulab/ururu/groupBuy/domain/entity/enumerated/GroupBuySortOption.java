package com.ururulab.ururu.groupBuy.domain.entity.enumerated;

public enum GroupBuySortOption {
    ORDER_COUNT,
    LATEST,
    DEADLINE,
    PRICE_LOW,
    PRICE_HIGH,
    DISCOUNT;

    public static GroupBuySortOption from(String raw) {
        return switch (raw.toLowerCase()) {
            case "latest" -> LATEST;
            case "deadline" -> DEADLINE;
            case "price_low" -> PRICE_LOW;
            case "price_high" -> PRICE_HIGH;
            case "discount" -> DISCOUNT;
            case "order_count" -> ORDER_COUNT;
            default -> throw new IllegalArgumentException("Invalid sort type: " + raw);
        };
    }
}
