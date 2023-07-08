package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.IndexingResponse;

@Service
public class IndexingServiceImpl implements IndexingService {

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        return new IndexingResponse(true, "");
    }

    private boolean isIndexing() {
        return false;
    }
}
