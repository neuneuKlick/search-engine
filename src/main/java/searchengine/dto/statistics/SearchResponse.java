package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class SearchResponse {
    private boolean results;
    private String error;
    private int count;
    private List<SearchInfo> listSearchInfo;
}
