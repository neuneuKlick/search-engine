package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "site")
public class Site {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
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
}
