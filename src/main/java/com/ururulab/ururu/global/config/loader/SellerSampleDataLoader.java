package com.ururulab.ururu.global.config.loader;

import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SellerSampleDataLoader implements CommandLineRunner {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (sellerRepository.existsByIdAndIsDeletedFalse(1L)) {
            log.info("Seller sample data already exists. Skipping data loading.");
            return;
        }

        log.info("🚀 Starting to load seller sample data...");
        loadSampleData();
        log.info("✅ Seller sample data loading completed!");
    }

    public void loadSampleData() {
        try {
            String encodedPassword = passwordEncoder.encode("sample123!@#");

            Seller seller = Seller.of(
                    "우르르 샘플 스토어",           // name (브랜드명)
                    "(주)우르르랩",               // businessName
                    "김우르",                    // ownerName
                    "1234567890",               // businessNumber
                    "sample@ururu.com",         // email
                    encodedPassword,            // password (암호화됨)
                    "01012345678",            // phone
                    null,                      // image
                    "12345",                  // zonecode (우편번호)
                    "서울특별시 강남구 테헤란로 123", // address1
                    "1층 101호",                // address2
                    "2023-서울강남-1234"        // mailOrderNumber
            );

            sellerRepository.save(seller);
            log.info("✅ Sample seller created with ID: {}", seller.getId());

        } catch (Exception e) {
            log.error("❌ Failed to create sample seller", e);
            throw new RuntimeException("Seller sample data loading failed", e);
        }
    }

}
