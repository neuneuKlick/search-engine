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
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class ParsingSite extends RecursiveAction {

    private static volatile Boolean enabled = false;
    private final String url;
    private final static Set<String> urlList = ConcurrentHashMap.newKeySet();
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ExecutorService executorService;
    private static Pattern patternUrl;
    private static NetworkService networkService;
    private static Map<String, String> collectionsDuplicate = new ConcurrentHashMap<>();

    public ParsingSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, ExecutorService executorService) {
        this.url = url.trim();
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = executorService;
    }

    public ParsingSite(String url, SiteModel siteModel, NetworkService networkService, SiteRepository siteRepository, PageRepository pageRepository) throws IOException{
        this.url = url;
        this.siteModel = siteModel;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.executorService = Executors.newFixedThreadPool(16);
        ParsingSite.networkService = networkService;
        patternUrl = Pattern.compile("(jpg)|(JPG)|(PNG)|(png)|(PDF)|(pdf)|(JPEG)|(jpeg)|(BMP)|(bmp)");
    }

    @Override
    protected void compute() {

        CopyOnWriteArrayList<ParsingSite> taskList = new CopyOnWriteArrayList<>();

        try {
            if (enabled) {
                StartExecuting.shutdown();
                return;
            }

            log.info("log test");
            System.out.println("Connection: " + url);
            Thread.sleep(1500);
            Connection.Response response = networkService.getConnection(url);

            if (!networkService.isAvailable(response)) {
                urlList.add(url);
                return;
            }
            if (!addUrlList(url)) {
                urlList.add(url);
                return;
            }

            Document doc = response.parse();
            updateTime(siteModel);
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

                        ParsingSite parsingSite = new ParsingSite(absUrl, siteModel, pageRepository, siteRepository, executorService);
                        taskList.add(parsingSite);
                        parsingSite.fork();

                }
            }
            taskList.forEach(ForkJoinTask::join);

        } catch (Exception exception) {
            urlList.add(url);
            log.debug("Ошибка подключения к сайту " + url + exception.getMessage());
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

    private void updateTime(SiteModel siteModel) {
        siteModel.setTimeStatus(new Date());
        siteRepository.saveAndFlush(siteModel);
    }

    private boolean addUrlList(String url) {
        return urlList.add(url);
    }

}
