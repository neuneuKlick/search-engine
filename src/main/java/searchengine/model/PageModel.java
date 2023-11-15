package searchengine.model;


import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pages", indexes = {@Index(name = "idx_page_path", columnList = "path")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"site", "path"}))
public class PageModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(columnDefinition = "INT", name = "site", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private SiteModel site;

    @Column(columnDefinition = "VARCHAR(1000) CHARACTER SET utf8", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", name = "code", nullable = false)
    private int codeResponse;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.MERGE)
    private List<IndexModel> indexes = new ArrayList<>();

}
