package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;

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
            SiteModel siteModel = getSiteModel(site, SiteStatus.INDEXING);
        }
    }

    public SiteModel getSiteModel(Site site, SiteStatus siteStatus) {

        SiteModel siteModel = new SiteModel();
        siteModel.setSiteStatus(siteStatus);
        siteModel.setTimeStatus(LocalDateTime.now());
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());

        siteRepository.save(siteModel);

        return siteModel;
    }
}
