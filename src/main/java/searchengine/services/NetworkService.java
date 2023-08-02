package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final SitesList sitesList;

    public Connection.Response getConnection(String url) throws IOException {
        return Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent("SearchBotFix/2.0")
                .referrer("https://google.com")
                .timeout(3000)
                .followRedirects(false)
                .execute();
    }

    public boolean isAvailable(Connection.Response response) {
        return ((response != null)
                && ((response.contentType().equalsIgnoreCase("text/html; charset=utf-8"))
                && (response.statusCode() == HttpStatus.OK.value())));
    }
}
