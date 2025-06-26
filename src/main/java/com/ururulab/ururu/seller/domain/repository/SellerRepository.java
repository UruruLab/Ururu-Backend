package com.ururulab.ururu.seller.domain.repository;

import com.ururulab.ururu.seller.domain.entity.Seller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 판매자 도메인 리포지토리
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    @Query("SELECT s FROM Seller s WHERE s.email = :email AND s.isDeleted = false")
    Optional<Seller> findByEmail(@Param("email") String email);
    
    @Query("SELECT s FROM Seller s WHERE s.businessNumber = :businessNumber AND s.isDeleted = false")
    Optional<Seller> findByBusinessNumber(@Param("businessNumber") String businessNumber);
    
    @Query("SELECT s FROM Seller s WHERE s.name = :name AND s.isDeleted = false")
    Optional<Seller> findByName(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seller s WHERE s.email = :email AND s.isDeleted = false")
    boolean existsByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seller s WHERE s.businessNumber = :businessNumber AND s.isDeleted = false")
    boolean existsByBusinessNumber(@Param("businessNumber") String businessNumber);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seller s WHERE s.name = :name AND s.isDeleted = false")
    boolean existsByName(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN false ELSE true END FROM Seller s WHERE s.email = :email AND s.isDeleted = false")
    boolean isEmailAvailable(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN false ELSE true END FROM Seller s WHERE s.name = :name AND s.isDeleted = false")
    boolean isNameAvailable(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN false ELSE true END FROM Seller s WHERE s.businessNumber = :businessNumber AND s.isDeleted = false")
    boolean isBusinessNumberAvailable(@Param("businessNumber") String businessNumber);

    @Query("SELECT s FROM Seller s WHERE s.isDeleted = false ORDER BY s.updatedAt DESC")
    List<Seller> findRecentActiveSellers(Pageable pageable);

    @Query("SELECT s FROM Seller s WHERE s.id = :sellerId AND s.isDeleted = false")
    Optional<Seller> findActiveSeller(@Param("sellerId") Long sellerId);
}