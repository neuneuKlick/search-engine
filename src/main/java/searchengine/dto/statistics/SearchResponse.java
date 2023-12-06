package searchengine.dto.statistics;

import lombok.*;

import java.util.List;



@Data
public class SearchResponse {
    private boolean results;
    private String error;
    private int count;
    private List<SearchInfo> listSearchInfo;

    public SearchResponse(boolean result, String error) {
        this.results = result;
        this.error = error;
    }

    public SearchResponse(boolean result, int count, List<SearchInfo> listSearchInfo) {
        this.results = result;
        this.count = count;
        this.listSearchInfo = listSearchInfo;
    }
}
