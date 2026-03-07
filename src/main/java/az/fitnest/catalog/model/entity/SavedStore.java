package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "saved_stores", indexes = {@Index(name = "idx_saved_stores_user_id", columnList = "user_id")}, uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "store_id"})})
@EntityListeners(value = {AuditingEntityListener.class})
public class SavedStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    public SavedStore() {
    }

    public SavedStore(Long id, Long userId, Store store, LocalDateTime createdDate) {
        this.id = id;
        this.userId = userId;
        this.store = store;
        this.createdDate = createdDate;
    }

    public static SavedStoreBuilder builder() {
        return new SavedStoreBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Store getStore() {
        return this.store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public static class SavedStoreBuilder {
        private Long id;
        private Long userId;
        private Store store;
        private LocalDateTime createdDate;

        SavedStoreBuilder() {
        }

        public SavedStoreBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SavedStoreBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public SavedStoreBuilder store(Store store) {
            this.store = store;
            return this;
        }

        public SavedStoreBuilder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public SavedStore build() {
            return new SavedStore(this.id, this.userId, this.store, this.createdDate);
        }

        public String toString() {
            return "SavedStore.SavedStoreBuilder(id=" + this.id + ", userId=" + this.userId + ", store=" + this.store + ", createdDate=" + this.createdDate + ")";
        }
    }
}
