package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class ParsingSite extends RecursiveAction {

    private String url;
    private final static Set<String> urlList = ConcurrentHashMap.newKeySet();
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private static Pattern patternUrl;
    private final SitesList sitesList;
    private static NetworkService networkService;
    private static Map<String, String> collectionsDuplicate = new ConcurrentHashMap<>();

    public ParsingSite(String url, SiteModel siteModel, PageRepository pageRepository, SitesList sitesList, NetworkService networkService) {
        this.url = url.trim();
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        ParsingSite.networkService = networkService;
        patternUrl = Pattern.compile("(jpg)|(png)|(pdf)|(doc)");
    }

    @Override
    protected void compute() {

        CopyOnWriteArrayList<ParsingSite> taskList = new CopyOnWriteArrayList<>();

        try {
            log.info("log test");
            System.out.println("Connection: " + url);
            Thread.sleep(150);
            Connection.Response response = networkService.getConnection(url);

            if (!networkService.isAvailable(response) && !urlList.contains(url)) {
                urlList.add(url);
                return;
            }

            Document doc = response.parse();
            addToPageTable(url, doc);
            Elements elements = doc.select("a");

            for (Element element : elements) {

                String absUrl = element.absUrl("href").indexOf(0) == '/'
                        ? siteModel.getUrl() + element.absUrl("href")
                        : element.absUrl("href");
                if(collectionsDuplicate.containsKey(absUrl)) {
                    continue;
                }

                if (!absUrl.isEmpty()
                        && absUrl.startsWith(siteModel.getUrl())
                        && !urlList.contains(absUrl)
                        && !absUrl.contains("#")
                        && !patternUrl.matcher(absUrl).find()) {

                    ParsingSite task = new ParsingSite(absUrl, siteModel, pageRepository, sitesList, networkService);
                    taskList.add(task);
                    task.fork();

                }
            }
            taskList.forEach(ForkJoinTask::join);

        } catch (InterruptedException interruptedException) {
            log.info("Interrupted Exception");
        } catch (MalformedURLException malformedURLException) {
            log.info("Malformed URL Exception");
        } catch (IOException ioException) {
            log.info("Input Output Exception");
        }

    }

    private PageModel addToPageTable(String url, Document document) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteModel(siteModel);
        pageModel.setPath(url.substring(siteModel.getUrl().length() - 1));
        pageModel.setCodeResponse(200);
        pageModel.setContent(document.outerHtml());
        pageRepository.saveAndFlush(pageModel);
        return pageModel;
    }

    public static void clearUrlList() {
        urlList.clear();
    }

}
