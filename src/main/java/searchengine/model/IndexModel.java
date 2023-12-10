package searchengine.model;

import lombok.*;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "indexes", uniqueConstraints = @UniqueConstraint(columnNames = {"page", "lemma"}))
public class IndexModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "page", nullable = false)
    public PageModel page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "lemma", nullable = false)
    public LemmaModel lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;

}
