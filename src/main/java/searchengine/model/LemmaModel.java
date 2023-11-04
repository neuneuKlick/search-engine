package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lemma", indexes = @Index(name = "lemma_index", columnList = "lemma, site_id, id", unique = true))
public class LemmaModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "lemma_site_FK"), columnDefinition = "Integer",
            referencedColumnName = "id", name = "site_id", nullable = false, updatable = false)
    private SiteModel siteModel;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "lemmaModel")
    private Set<PageModel> pageModel = new HashSet<>();

    public LemmaModel(SiteModel siteModel, String lemma, int frequency) {
        this.siteModel = siteModel;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
