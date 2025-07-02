package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupBuyDetailImageRepository extends JpaRepository<GroupBuyImage, Long> {

}
