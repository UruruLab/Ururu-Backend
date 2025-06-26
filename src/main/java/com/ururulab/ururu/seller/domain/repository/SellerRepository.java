package com.ururulab.ururu.seller.domain.repository;

import com.ururulab.ururu.seller.domain.entity.Seller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    // === 기본 조회 (모든 판매자) ===
    Optional<Seller> findByEmail(String email);
    Optional<Seller> findByBusinessNumber(String businessNumber);
    Optional<Seller> findByName(String name);

    // === 활성 판매자 조회 ===
    Optional<Seller> findByEmailAndIsDeletedFalse(String email);
    Optional<Seller> findByBusinessNumberAndIsDeletedFalse(String businessNumber);
    Optional<Seller> findByNameAndIsDeletedFalse(String name);

    // === 중복 확인 ===
    boolean existsByEmailAndIsDeletedFalse(String email);
    boolean existsByBusinessNumberAndIsDeletedFalse(String businessNumber);
    boolean existsByNameAndIsDeletedFalse(String name);

    // === 가용성 체크 ===
    default boolean isEmailAvailable(String email) {
        return !existsByEmailAndIsDeletedFalse(email);
    }

    default boolean isNameAvailable(String name) {
        return !existsByNameAndIsDeletedFalse(name);
    }

    default boolean isBusinessNumberAvailable(String businessNumber) {
        return !existsByBusinessNumberAndIsDeletedFalse(businessNumber);
    }

    // === 목록 조회 ===
    List<Seller> findByIsDeletedFalseOrderByUpdatedAtDesc(Pageable pageable);

    default Optional<Seller> findActiveSeller(Long sellerId) {
        return findById(sellerId).filter(seller -> !seller.getIsDeleted());
    }
}