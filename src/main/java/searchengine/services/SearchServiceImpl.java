package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchResponse;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchServiceImpl implements SearchService {
    @Override
    public SearchResponse searchResults(String query, String site, int offset, int limit) {
        return null;
    }
}
