package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.PageInfo;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final JsoupConfig jsoupConfig;

    public PageInfo getPageInfo(String url) throws IOException, InterruptedException {
        Connection.Response response = getResponse(url);
        return new PageInfo(response.parse().html(), response.statusCode());
    }

    private Connection.Response getResponse(String url) throws IOException, InterruptedException {
        Thread.sleep(500);

        return Jsoup.connect(url)
                .maxBodySize(0)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .header("Accept-Language", "ru")
                .ignoreHttpErrors(true)
                .execute();
    }
    public Set<String> getPaths(String content) {
        Document document = Jsoup.parse(content);
        return document.select("a[href]").stream()
                .map(element -> element.attr("href"))
                .filter(path -> path.startsWith("/"))
                .collect(Collectors.toSet());
    }

    public String htmlToText(String content) {
        return Jsoup.parse(content).text();
    }

    public String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }
}
