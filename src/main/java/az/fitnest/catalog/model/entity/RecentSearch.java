package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recent_searches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentSearch extends BaseAuditableEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "search_query", nullable = false, length = 255)
    private String query;

    @Column(name = "search_type", nullable = false, length = 50)
    private String type; // e.g. "GYM" or "STORE"
}
