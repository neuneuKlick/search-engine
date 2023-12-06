package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
