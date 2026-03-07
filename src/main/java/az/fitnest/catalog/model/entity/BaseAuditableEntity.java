package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.*;

@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseAuditableEntity
        extends BaseEntity {
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
}
