package searchengine.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sites")
public class SiteModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM ('INDEXED', 'INDEXING', 'FAILED')", nullable = false)
    private SiteStatus siteStatus;

    @Column(nullable = false)
    private LocalDateTime timeStatus;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.MERGE)
    private List<PageModel> pages= new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.MERGE)
    private List<LemmaModel> lemmas = new ArrayList<>();
}
