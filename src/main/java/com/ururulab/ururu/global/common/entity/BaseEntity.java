package com.ururulab.ururu.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt; // 생성 시간

    @Column(nullable = false)
    private ZonedDateTime updatedAt; // 수정 시간

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
