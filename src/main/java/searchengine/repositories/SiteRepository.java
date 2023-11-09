package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

    @Query("select e.siteStatus from SiteModel as e where e.url =:url")
    SiteStatus findByUrl(String url);

    SiteModel findSiteModelByUrl(String url);

    @Query("select e.lastError  from SiteModel as e where e.url=:url")
    String findErrorByUrl(String url);
}
