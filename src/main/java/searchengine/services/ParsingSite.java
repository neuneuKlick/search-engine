package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class ParsingSite extends RecursiveAction {

    private final String url;
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static Pattern patternUrl;
    private static NetworkService networkService;
    public static volatile Boolean stop = false;
    private final static Set<String> setAbsUrls = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;

    public ParsingSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, ExecutorService executorService) {
        this.url = url.trim();
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = executorService;
    }

    public ParsingSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, NetworkService networkService) throws IOException{
        this.url = url;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = Executors.newFixedThreadPool(16);
        ParsingSite.networkService = networkService;
        patternUrl = Pattern.compile("(jpg)|(JPG)|(PNG)|(png)|(PDF)|(pdf)|(JPEG)|(jpeg)|(BMP)|(bmp)");
    }

    @Override
    protected void compute() {

        CopyOnWriteArrayList<ParsingSite> tasks = new CopyOnWriteArrayList<>();
        try {
            if (stop) {
                stopExecute();
                return;
            }

            log.info("log test");
            System.out.println("Connection: " + url);
            Thread.sleep(150);
            Connection.Response response = networkService.getConnection(url);

            if (!networkService.isAvailable(response)) {
                setAbsUrls.add(url);
                return;
            }
            if (!addUrlList(url)) {
                setAbsUrls.add(url);
                return;
            }
            Document doc = response.parse();
            updateSiteTime(siteModel);
            PageModel pageModel = getPageModel(url, doc);
            pageRepository.saveAndFlush(pageModel);
            log.info("An entry has been added " + url + " " + Thread.currentThread());

            Elements elements = doc.select("a");
            for (Element element : elements) {
                String absUrl = element.absUrl("href").indexOf(0) == '/'
                        ? siteModel.getUrl() + element.absUrl("href")
                        : element.absUrl("href");

                if (!absUrl.isEmpty()
                        && absUrl.startsWith(siteModel.getUrl())
                        && !setAbsUrls.contains(absUrl)
                        && !absUrl.contains("#")
                        && !patternUrl.matcher(absUrl).find()) {
                    ParsingSite parsingSite = new ParsingSite(absUrl, siteModel, pageRepository, siteRepository, executorService);
                    tasks.add(parsingSite);
                    parsingSite.fork();
                }
            }
            tasks.forEach(ForkJoinTask::join);

        } catch (Exception exception) {
            setAbsUrls.add(url);
            log.debug("Error connecting to the site " + url + exception.getMessage());
        }
    }

    private PageModel getPageModel(String url, Document document) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteModel(siteModel);
        pageModel.setPath(url.substring(siteModel.getUrl().length()));
        pageModel.setCodeResponse(200);
        pageModel.setContent(document.outerHtml());
        return pageModel;
    }

    public static void clearSetAbsUrl() {
        setAbsUrls.clear();
    }

    private void updateSiteTime(SiteModel siteModel) {
        siteModel.setTimeStatus(new Date());
        siteRepository.saveAndFlush(siteModel);
    }

    private boolean addUrlList(String url) {
        return setAbsUrls.add(url);
    }

    private void stopExecute() {
        StartExecuting.shutdown();
    }

}
