package searchengine.services;

import searchengine.dto.statistics.SearchResponse;

public interface SearchService {
    SearchResponse searchResults(String query, String site, int offset, int limit);
}
