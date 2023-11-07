package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

    @Query("select COUNT(*) from PageModel AS e group by e.siteModel having e.siteModel =:site")
    Integer findCountBySite(SiteModel site);
}
