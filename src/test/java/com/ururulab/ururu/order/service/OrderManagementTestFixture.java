package com.ururulab.ururu.order.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.dto.request.ShippingInfoUpdateRequest;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;

public class OrderManagementTestFixture {

    public static ShippingInfoUpdateRequest createShippingRequest(String trackingNumber) {
        return new ShippingInfoUpdateRequest(trackingNumber);
    }

    public static ShippingInfoUpdateRequest createValidShippingRequest() {
        return createShippingRequest("520987654321");
    }

    // 엔티티 생성 메서드들 (RefundTestFixture에서 복사)
    public static Member createMember(Long id, String nickname, String email) {
        Member member = Member.of(
                nickname,
                email,
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                LocalDate.parse("1990-01-01"),
                "01012345678",
                "profile.jpg",
                Role.NORMAL
        );
        setFieldValue(member, "id", id);
        setFieldValue(member, "point", 5000);
        return member;
    }

    public static Seller createSeller(Long id, String name) {
        try {
            Constructor<Seller> constructor = Seller.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Seller seller = constructor.newInstance();

            setFieldValue(seller, "id", id);
            setFieldValue(seller, "name", name);

            return seller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Seller for test", e);
        }
    }

    public static Product createProduct(Long id, String name) {
        try {
            Constructor<Product> constructor = Product.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Product product = constructor.newInstance();

            setFieldValue(product, "id", id);
            setFieldValue(product, "name", name);

            return product;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Product for test", e);
        }
    }

    public static ProductOption createProductOption(Long id, String name, Integer price, String imageUrl) {
        try {
            Constructor<ProductOption> constructor = ProductOption.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProductOption option = constructor.newInstance();

            setFieldValue(option, "id", id);
            setFieldValue(option, "name", name);
            setFieldValue(option, "price", price);
            setFieldValue(option, "imageUrl", imageUrl);

            return option;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ProductOption for test", e);
        }
    }

    public static GroupBuy createGroupBuy(Long id, Product product, Seller seller) {
        try {
            Constructor<GroupBuy> constructor = GroupBuy.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            GroupBuy groupBuy = constructor.newInstance();

            setFieldValue(groupBuy, "id", id);
            setFieldValue(groupBuy, "product", product);
            setFieldValue(groupBuy, "seller", seller);

            return groupBuy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GroupBuy for test", e);
        }
    }

    public static GroupBuyOption createGroupBuyOption(Long id, GroupBuy groupBuy, ProductOption productOption,
                                                      Integer salePrice, Integer stock) {
        try {
            Constructor<GroupBuyOption> constructor = GroupBuyOption.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            GroupBuyOption option = constructor.newInstance();

            setFieldValue(option, "id", id);
            setFieldValue(option, "groupBuy", groupBuy);
            setFieldValue(option, "productOption", productOption);
            setFieldValue(option, "salePrice", salePrice);
            setFieldValue(option, "stock", stock);

            return option;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GroupBuyOption for test", e);
        }
    }

    public static Order createOrder(String id, Member member, OrderStatus status) {
        Order order = Order.create(member);
        setFieldValue(order, "id", id);
        setFieldValue(order, "status", status);
        return order;
    }

    public static OrderItem createOrderItem(Long id, Order order, GroupBuyOption groupBuyOption, Integer quantity) {
        OrderItem orderItem = OrderItem.create(groupBuyOption, quantity);
        setFieldValue(orderItem, "id", id);
        setFieldValue(orderItem, "order", order);
        return orderItem;
    }

    public static TestScenario createTestScenario() {
        Member member = createMember(1L, "testMember", "test@example.com");
        Seller seller = createSeller(1L, "testSeller");
        Product product = createProduct(1L, "테스트 상품");
        ProductOption productOption = createProductOption(1L, "기본 옵션", 8000, "image.jpg");
        GroupBuy groupBuy = createGroupBuy(1L, product, seller);
        GroupBuyOption groupBuyOption = createGroupBuyOption(1L, groupBuy, productOption, 8000, 100);
        Order order = createOrder("test-order-id", member, OrderStatus.ORDERED);
        OrderItem orderItem = createOrderItem(1L, order, groupBuyOption, 2);

        order.addOrderItem(orderItem);

        return new TestScenario(member, seller, product, productOption, groupBuy,
                groupBuyOption, order, orderItem);
    }

    // Reflection 유틸리티 메서드
    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName + " for test", e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field " + fieldName + " not found");
    }

    // 테스트 시나리오 클래스
    public static class TestScenario {
        public final Member member;
        public final Seller seller;
        public final Product product;
        public final ProductOption productOption;
        public final GroupBuy groupBuy;
        public final GroupBuyOption groupBuyOption;
        public final Order order;
        public final OrderItem orderItem;

        public TestScenario(Member member, Seller seller, Product product, ProductOption productOption,
                            GroupBuy groupBuy, GroupBuyOption groupBuyOption, Order order, OrderItem orderItem) {
            this.member = member;
            this.seller = seller;
            this.product = product;
            this.productOption = productOption;
            this.groupBuy = groupBuy;
            this.groupBuyOption = groupBuyOption;
            this.order = order;
            this.orderItem = orderItem;
        }
    }
}
