package searchengine.dto.statistics;

import lombok.*;
import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private String error;
    private int count;
    private List<SearchInfo> data;

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public SearchResponse(boolean result, int count, List<SearchInfo> listSearchInfo) {
        this.result = result;
        this.count = count;
        this.data = listSearchInfo;
    }
}
