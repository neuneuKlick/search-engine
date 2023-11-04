package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "`index`")
public class IndexModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"), name = "page_id", nullable = false)
    public PageModel pageModel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"), name = "lemma_id", nullable = false)
    public LemmaModel lemmaModel;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    public IndexModel(PageModel pageModel, LemmaModel lemmaModel, float rank) {
        this.pageModel = pageModel;
        this.lemmaModel = lemmaModel;
        this.rank = rank;
    }
}
