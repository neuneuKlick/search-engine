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
                .userAgent(sitesList.getName())
                .referrer(sitesList.getReferer())
                .timeout(sitesList.getTimeout())
                .followRedirects(false)
                .execute();
    }

    public boolean isAvailable(Connection.Response response) {
        return ((response != null)
                && ((response.contentType().equalsIgnoreCase(sitesList.getContentType()))
                && (response.statusCode() == HttpStatus.OK.value())));
    }
}
