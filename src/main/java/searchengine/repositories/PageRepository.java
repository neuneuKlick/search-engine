package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;


@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

    @Query("select COUNT(*) from PageModel AS e group by e.site having e.site =:site")
    Integer findCountBySite(SiteModel site);

    @Query("select p from PageModel as p " +
            "join IndexModel as i on p.id = i.page.id " +
            "where i.lemma.lemma =:lemma and p.site =:site")
    CopyOnWriteArrayList<PageModel> findByLemma(String lemma, SiteModel site);

    boolean existsBySiteIdAndPath(Integer siteId, String url);

    Optional<PageModel> findBySiteAndPath(SiteModel site, String url);

    List<PageModel> findAllByIdIn(List<Integer> id);

}
