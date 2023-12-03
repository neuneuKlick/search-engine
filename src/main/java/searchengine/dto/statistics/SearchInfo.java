package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class SearchInfo {
    private String site;
    private String siteName;
    private String url;
    private String title;
    private String snippet;
    private double relevance;
}
