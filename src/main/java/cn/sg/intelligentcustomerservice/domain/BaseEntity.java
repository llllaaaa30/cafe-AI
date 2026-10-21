package cn.sg.intelligentcustomerservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;

/**
 * 数据库实体基类
 * 包含通用字段：创建时间、更新时间、删除标志
 * 注解 @SoftDelete  会有一个 deleted 的字段 表示是否删除
 */
@Data
@MappedSuperclass
@SoftDelete
public class BaseEntity {
    
    /**
     * 创建时间
     * 插入时自动填充
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     * 插入和更新时自动填充
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    /**
     * 创建前自动填充创建时间和更新时间
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }
    
    /**
     * 更新前自动填充更新时间
     */
    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}