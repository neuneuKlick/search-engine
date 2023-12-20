package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

    boolean existsBySiteIdAndPath(Integer siteId, String url);

    Optional<PageModel> findBySiteAndPath(SiteModel site, String url);

    Integer countBySite(SiteModel site);


}
