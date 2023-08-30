package searchengine.model;

import ch.qos.logback.classic.db.names.ColumnName;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.mapping.Set;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = @Index(columnList = "path", name = "index_path", unique = true))
public class PageModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private SiteModel siteModel;
    @Column(nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8")
    private String path;
    @Column(name = "code", nullable = false)
    private int codeResponse;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;
}
