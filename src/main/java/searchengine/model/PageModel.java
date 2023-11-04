package searchengine.model;

import ch.qos.logback.classic.db.names.ColumnName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = @Index(name = "path_siteId_index", columnList = "path, site_id", unique = true))
@NoArgsConstructor
public class PageModel {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(foreignKey = @ForeignKey(name = "site_page_FK"), columnDefinition = "Integer",
            referencedColumnName = "id", name = "site_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SiteModel.class,
            cascade = {CascadeType.MERGE, CascadeType.REFRESH}, optional = false)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private SiteModel siteModel;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8")
    private String path;

    @Column(name = "code", nullable = false)
    private int codeResponse;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "`index`",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
    private Set<LemmaModel> lemmaModel = new HashSet<>();

    public PageModel(SiteModel siteModel, String path, int statusCode, String content) {
        this.siteModel = siteModel;
        this.path = path;
        this.codeResponse = statusCode;
        this.content = content;

    }
}
