package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity(name = "SiteModel")
@Getter
@Setter
@Table(name = "site")
public class SiteModel {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM ('INDEXED', 'INDEXING', 'FAILED')")
    private SiteStatus siteStatus;

    @Column(name = "status_time", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime timeStatus;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "siteModel", cascade = CascadeType.REMOVE)
    private Set<PageModel> pageModel;

    @OneToMany(mappedBy = "siteModel", cascade = CascadeType.REMOVE)
    private Set<LemmaModel> lemmaModel;
}
