package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.RefundItem;
import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.dto.request.RefundProcessRequestDto;
import com.ururulab.ururu.payment.dto.request.RefundRequestDto;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;

public class RefundTestFixture {

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
        setMemberId(member, id);
        setMemberPoint(member, 5000);
        return member;
    }

    public static Member createMember(Long id) {
        return createMember(id, "testMember", "test@example.com");
    }

    public static Seller createSeller(Long id, String name) {
        try {
            Constructor<Seller> constructor = Seller.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Seller seller = constructor.newInstance();

            Field idField = Seller.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(seller, id);

            Field nameField = Seller.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(seller, name);

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

    public static GroupBuyOption createGroupBuyOption(Long id, GroupBuy groupBuy, ProductOption productOption, Integer salePrice, Integer stock) {
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

        // 디버깅용 로그
        System.out.println("Created Order with ID: " + id);

        return order;
    }

    public static OrderItem createOrderItem(Long id, Order order, GroupBuyOption groupBuyOption, Integer quantity) {
        OrderItem orderItem = OrderItem.create(groupBuyOption, quantity);
        setFieldValue(orderItem, "id", id);
        setFieldValue(orderItem, "order", order);
        return orderItem;
    }

    public static Payment createPayment(Long id, Member member, Order order, Integer totalAmount, Integer amount, Integer point) {
        Payment payment = Payment.create(member, order, totalAmount, amount, point);
        setFieldValue(payment, "id", id);
        setFieldValue(payment, "status", PaymentStatus.PAID);
        return payment;
    }

    public static Refund createRefund(String id, Payment payment, RefundType type, String reason, Integer amount, Integer point, RefundStatus status) {
        Refund refund = Refund.create(payment, type, reason, amount, point, "1234567890", status);
        setFieldValue(refund, "id", id);
        return refund;
    }

    public static Refund createRefundWithItems(String id, Payment payment, RefundType type, String reason,
                                               Integer amount, Integer point, RefundStatus status, OrderItem orderItem) {
        Refund refund = createRefund(id, payment, type, reason, amount, point, status);

        // RefundItem 생성 및 추가
        RefundItem refundItem = createRefundItem(1L, refund, orderItem);

        // refundItems 리스트에 추가 (Reflection 사용)
        try {
            Field refundItemsField = Refund.class.getDeclaredField("refundItems");
            refundItemsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<RefundItem> refundItems = (java.util.List<RefundItem>) refundItemsField.get(refund);
            refundItems.add(refundItem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add RefundItem for test", e);
        }

        return refund;
    }

    public static RefundItem createRefundItem(Long id, Refund refund, OrderItem orderItem) {
        RefundItem refundItem = RefundItem.create(orderItem);
        setFieldValue(refundItem, "id", id);
        setFieldValue(refundItem, "refund", refund);
        return refundItem;
    }

    // Request DTOs
    public static RefundRequestDto createChangeOfMindRequest() {
        return new RefundRequestDto("CHANGE_OF_MIND", "단순 변심", "1234567890");
    }

    public static RefundProcessRequestDto createApproveRequest() {
        return new RefundProcessRequestDto("APPROVE", null);
    }

    public static RefundProcessRequestDto createRejectRequest(String reason) {
        return new RefundProcessRequestDto("REJECT", reason);
    }

    public static RefundTestScenario createCompleteScenario() {
        Member member = createMember(1L);
        Seller seller = createSeller(1L, "testSeller");
        Product product = createProduct(1L, "테스트 상품");
        ProductOption productOption = createProductOption(1L, "기본 옵션", 8000, "image.jpg");
        GroupBuy groupBuy = createGroupBuy(1L, product, seller);
        GroupBuyOption groupBuyOption = createGroupBuyOption(1L, groupBuy, productOption, 8000, 100);
        Order order = createOrder("test-order-id", member, OrderStatus.ORDERED);
        OrderItem orderItem = createOrderItem(1L, order, groupBuyOption, 2);

        // 간단한 금액 설정: 상품 16000원, 포인트 1000원, 총 17000원
        Payment payment = createPayment(1L, member, order, 17000, 16000, 1000);

        // Order와 Payment의 연결 확실히 하기
        setFieldValue(payment, "order", order);

        return new RefundTestScenario(member, seller, product, productOption, groupBuy,
                groupBuyOption, order, orderItem, payment);
    }

    // Helper methods
    private static void setMemberId(Member member, Long id) {
        setFieldValue(member, "id", id);
    }

    private static void setMemberPoint(Member member, Integer point) {
        setFieldValue(member, "point", point);
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
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

    // 테스트 시나리오를 담는 클래스
    public static class RefundTestScenario {
        public final Member member;
        public final Seller seller;
        public final Product product;
        public final ProductOption productOption;
        public final GroupBuy groupBuy;
        public final GroupBuyOption groupBuyOption;
        public final Order order;
        public final OrderItem orderItem;
        public final Payment payment;

        public RefundTestScenario(Member member, Seller seller, Product product, ProductOption productOption,
                                  GroupBuy groupBuy, GroupBuyOption groupBuyOption, Order order,
                                  OrderItem orderItem, Payment payment) {
            this.member = member;
            this.seller = seller;
            this.product = product;
            this.productOption = productOption;
            this.groupBuy = groupBuy;
            this.groupBuyOption = groupBuyOption;
            this.order = order;
            this.orderItem = orderItem;
            this.payment = payment;
        }
    }
}