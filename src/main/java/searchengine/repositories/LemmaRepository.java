package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    LemmaModel findByLemmaAndSite(String lemma, SiteModel siteModel);

    Optional<LemmaModel> findBySiteAndLemma(SiteModel site, String name);

    @Query("select a from LemmaModel as a where a.frequency < 300" +
            " and a.lemma in (:lemmas) " +
            " and a.site=:site")
    List<LemmaModel> selectLemmasBySite(Set<String> lemmas, SiteModel site);

    Set<LemmaModel> findAllBySite(SiteModel site);

    List<Optional<LemmaModel>> findByLemma(String lemma);

    Integer countBySite(SiteModel site);


}
