package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(final String url) {
        return ResponseEntity.ok(indexingService.indexingPage(url));
    }

    @GetMapping("/search")
    public  ResponseEntity<SearchResponse> search(@RequestParam(name = "query", defaultValue = "") String query,
                                                  @RequestParam(name = "site", defaultValue = "") String site,
                                                  @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                  @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (query.isEmpty()) {
            return ResponseEntity.ok(new SearchResponse(false, "Задан пустой поисковый запрос"));
        }
        return ResponseEntity.ok(searchService.searchResults(query, site, offset, limit));
    }
}
