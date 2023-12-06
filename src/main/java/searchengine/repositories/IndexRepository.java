package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;

import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {
    int countByLemma(LemmaModel lemma);

    Set<IndexModel> findAllByLemma(LemmaModel lemma);

    IndexModel findByLemmaAndPage(LemmaModel lemma, PageModel page);

    Set<IndexModel> findAllByLemmaAndPageIn(LemmaModel lemmaModel, Set<PageModel> pages);

    Set<IndexModel> findAllByPageAndLemmaIn(PageModel pageModel, Set<LemmaModel> lemmas);

}
