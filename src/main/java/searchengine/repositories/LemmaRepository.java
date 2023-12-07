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

    Optional<LemmaModel> findBySiteAndLemma(SiteModel site, String name);

    Set<LemmaModel> findAllBySite(SiteModel site);

    List<Optional<LemmaModel>> findByLemma(String lemma);

}
