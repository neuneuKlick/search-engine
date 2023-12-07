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


    boolean existsByIdAndSiteStatus(Integer id, SiteStatus status);

    boolean existsBySiteStatus(SiteStatus status);

    Set<SiteModel> findAllBySiteStatus(SiteStatus status);

    boolean existsBySiteStatusNot(SiteStatus status);

    Optional<SiteModel> findByUrlIgnoreCase(String siteUrl);
}
