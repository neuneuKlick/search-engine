package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

    @Query("select e.siteStatus from SiteModel as e where e.url =:url")
    SiteStatus findByUrl(String url);

    boolean existsByIdAndSiteStatus(Integer id, SiteStatus status);

    boolean existsBySiteStatus(SiteStatus status);

    Set<SiteModel> findAllBySiteStatus(SiteStatus status);

    @Query("select e.lastError  from SiteModel as e where e.url=:url")
    String findErrorByUrl(String url);

    boolean existsBySiteStatusNot(SiteStatus status);

    Optional<SiteModel> findByUrlIgnoreCase(String siteUrl);
}
