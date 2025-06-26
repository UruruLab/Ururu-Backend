package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

}
