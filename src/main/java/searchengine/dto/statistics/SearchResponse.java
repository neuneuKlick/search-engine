package searchengine.dto.statistics;

import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SearchResponse {
    private boolean results;
    private String error;
    private int count;
    private List<SearchInfo> listSearchInfo;

    public SearchResponse(boolean results, String error) {
        this.results = results;
        this.error = error;
    }
}
