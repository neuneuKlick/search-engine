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

    boolean existsBySiteIdAndPath(Integer siteId, String url);

    Optional<PageModel> findBySiteAndPath(SiteModel site, String url);



}
