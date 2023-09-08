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

    List<PageModel> findBySiteModel(SiteModel siteModel);

    @Query(value = "SELECT count(*) FROM search_engine.page where path = ?1 and site_id = ?2", nativeQuery = true)
    Integer findCountByPathAndId(String path , long id); // найти кол-во пути и сайт

    @Query(value = "SELECT * FROM search_engine.page where path = ?1 and site_id = ?2", nativeQuery = true)
    List<PageModel> findPageModelListByPathAndId(String path , long id);




    boolean existsByPathAndId(long id, String path);
//
//    Integer findByPathAndSiteModelId(SiteModel siteModel);

    List<PageModel> findByPathAndSiteModel(SiteModel siteModel, String path);
}
