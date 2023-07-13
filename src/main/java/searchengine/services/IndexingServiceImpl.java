package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.repositories.SiteRepository;

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
            return new IndexingResponse(false, "Индексация уже запущена");
        }

        List<Site> siteList = sitesList.getSites();

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true, "");
    }

    private boolean isIndexing() {
        return false;
    }
}
