package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final NetworkService networkService;
    public static boolean isIndexed = false;
    public static boolean isInterrupted;

    @Override
    public IndexingResponse startIndexing() {

//        if (isIndexed()) {
//            return new IndexingResponse(false, "Indexing is already running");
//        }
        List<Site> siteList = sitesList.getSites();

        isIndexed();
        isInterrupted();

        executeIndexing(siteList);

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true, "");
    }

    private void executeIndexing(List<Site> siteList) {

        cleaningData();
        ParsingSite.clearUrlList();

        ForkJoinPool fjp = new ForkJoinPool();

        for (Site site : siteList) {

            SiteModel siteModel = getSiteModel(site);

            Runnable task = () -> {
                //TODO сделать проверку дубликатов
                //TODO Написать подобный класс StartExecutor чтобы в методе run() правильно передавать Network, ловить exception и делать проверки
                ParsingSite parsingSite = getContentSite(siteModel);

                fjp.invoke(parsingSite);

                if (fjp.submit(parsingSite).isDone()) {

                    isIsIndexedStopped();

                    changeSiteModel(siteModel);
                }
            };

            Thread thread = new Thread(task);
            thread.start();
        }
    }

    private SiteModel getSiteModel(Site site) {

        SiteModel siteModel = new SiteModel();
        siteModel.setSiteStatus(SiteStatus.INDEXING);
        siteModel.setTimeStatus(LocalDateTime.now());
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());

        siteRepository.saveAndFlush(siteModel);

        return siteModel;
    }

    private ParsingSite getContentSite(SiteModel siteModel) {

        return new ParsingSite(siteModel.getUrl(), siteModel, pageRepository, sitesList, networkService);
    }

    private void changeSiteModel(SiteModel siteModel) {

        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteModel.setTimeStatus(LocalDateTime.now());

        siteRepository.saveAndFlush(siteModel);
    }

    private static boolean isIndexed() {
        return isIndexed = true;
    }

    private static boolean isIsIndexedStopped() {
        return isIndexed = false;
    }

    private static boolean isInterrupted() {
        return isInterrupted = false;
    }

    private void cleaningData() {
        siteRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
    }

}
