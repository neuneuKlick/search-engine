package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public IndexingResponse startIndexing() {

        if (isIndexing()) {
            return new IndexingResponse(false, "Indexing is already running");
        }

        List<Site> siteList = sitesList.getSites();

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        executeIndexing(siteList, forkJoinPool);

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true, "");
    }

    private boolean isIndexing() {
        return false;
    }

    private void executeIndexing(List<Site> siteList, ForkJoinPool forkJoinPool) {

        siteRepository.deleteAll();
        for (Site site : siteList) {

            SiteModel siteModel = getSiteModel(site);


            Runnable task = () -> {
                ParsingSite parsingSite = getContentSite(siteModel);
                forkJoinPool.invoke(parsingSite);

                if (forkJoinPool.submit(parsingSite).isDone()) {
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

    public ParsingSite getContentSite(SiteModel siteModel) {
        ParsingSite parsingSite = new ParsingSite(siteModel.getUrl(), siteModel, pageRepository);


        return parsingSite;
    }

    private void changeSiteModel(SiteModel siteModel) {

        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteModel.setTimeStatus(LocalDateTime.now());

        siteRepository.saveAndFlush(siteModel);
    }



}
