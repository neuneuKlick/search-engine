package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = @Index(columnList = "path", name = "index_path", unique = true))
public class Page {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Site siteId;
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;
    @Column(name = "code", nullable = false)
    private int codeResponse;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}
