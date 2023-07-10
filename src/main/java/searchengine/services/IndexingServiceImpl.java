package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
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
