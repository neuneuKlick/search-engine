package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    LemmaModel findByLemmaAndSite(String lemma, SiteModel site);

    Optional<LemmaModel> findBySiteAndLemma(SiteModel site, String name);

    Set<LemmaModel> findAllBySite(SiteModel site);

    Integer countBySite(SiteModel site);
}
