package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class ParseSite extends RecursiveAction {

    private final String url;
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static Pattern patternUrl;
    private static NetworkService networkService;
    private static volatile Boolean isInterrupted;
    private final static Set<String> setAbsUrls = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;

    public ParseSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, ExecutorService executorService) {
        this.url = url.trim();
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = executorService;
    }

    public ParseSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, NetworkService networkService) throws IOException{
        this.url = url;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = Executors.newFixedThreadPool(16);
        ParseSite.networkService = networkService;
        patternUrl = Pattern.compile("(jpg)|(JPG)|(PNG)|(png)|(PDF)|(pdf)|(JPEG)|(jpeg)|(BMP)|(bmp)");
    }

    @SneakyThrows
    @Override
    protected void compute() {
        log.info("log test");
        /**
         * Если прерывается, тогда прерываем
         */
        if (isInterrupted) {
            return;
        }

        if (!url.contains(siteModel.getUrl()) || (url.contains("#") || (url.contains("&") || (url.contains("?"))))) {
            parseSiteFinished();
            return;
        }
        String path = url.equals(siteModel.getUrl()) ? "" : url.split(siteModel.getUrl())[1];
        path = path.replaceAll(":443", "");
        Thread.sleep(150);
        /**
         * Создаём соединение
         */
        Document document = null;
        PageModel pageModel = null;
        try {

        Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT/5.1;" +
                "en-US; rvl.8.1.6) Gecko/20070725 FireFox/2.0.0.6)").referrer("http:/www.google.com").execute();
        document = response.parse();
        pageModel = new PageModel(siteModel.getId(), path, response.statusCode(), document.html());
        } catch (UnsupportedMimeTypeException unsupportedMimeTypeException) {

        } catch (IOException ioException) {
            siteModel.setLastError("The site page was unavailable");
            siteRepository.save(siteModel);
        }
        /**
         * Если проверка Page прошла, идём дальше
         */
        if (isCheckedPage(pageModel)) {
            return;
        }

        /**
         * Если проверка Url прошла запускаем парсинг, затем заканчиваем.
         */
        if (isCheckedPageUrlWhileStarting(url, document)) {
            return;
        }

        /**
         * Проверяем статус ответа Page
         */
        if (!isCheckedPageExistDB(pageModel)) {
            return;
        }

        /**
         * Запускаем парсинг и завершаем
         */
        log.info("An entry has been added " + url + " " + Thread.currentThread());
        parseSiteStarted(document);
        parseSiteFinished();

        //TODO Правильное решение по дубликатам
        /**
         * Нужно сложить гарантированно уникальные ссылки в Set
         */
    }

    protected boolean isCheckedPageExistDB(PageModel pageModel) {
        synchronized (siteModel) {
            siteModel.setSiteStatus(SiteStatus.INDEXING);
            siteModel.setTimeStatus(LocalDateTime.now());
            siteRepository.save(siteModel);
            if (!(pageModel.getCodeResponse() == 200)) {
                return false;
            }
            long pageCount = pageRepository.findCountByPathAndId(pageModel.getPath(), pageModel.getId());
            if (pageCount > 0) {
                return false;
            }
            pageRepository.save(pageModel);
            parseSiteFinished();
            return true;
        }
    }


//    private synchronized PageModel getPageModel(String url, Document document) {
//        PageModel pageModel = new PageModel();
//        pageModel.setSiteModel(siteModel);
//        pageModel.setPath(url.substring(siteModel.getUrl().length()));
//        pageModel.setCodeResponse(200);
//        pageModel.setContent(document.outerHtml());
//        pageRepository.saveAndFlush(pageModel);
//        return pageModel;
//    }

    public static void clearSetAbsUrl() {
        setAbsUrls.clear();
    }

    private void updateSiteTime(SiteModel siteModel) {
        siteModel.setTimeStatus(LocalDateTime.now());
        siteRepository.saveAndFlush(siteModel);
    }

    private boolean addUrlList(String url) {
        return setAbsUrls.add(url);
    }

    public static void switchOffIsInterrupted() {
        isInterrupted = false;
    }

    public static void switchOnIsInterrupted() {
        isInterrupted = true;
    }

    private boolean isCheckedPage(PageModel pageModel) {
        if (pageModel == null) {
            parseSiteFinished();
            return true;
        }        
        return false;
    }
    
    private boolean isCheckedPageUrlWhileStarting(String url, Document document) {
        if (url.equals("")) {
            parseSiteStarted(document);
            parseSiteFinished();
            return true;
        }
        return false;
    }

    private void parseSiteFinished() {
        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteRepository.save(siteModel);
    }

    private void parseSiteStarted(Document document) {
        List<ParseSite> tasks = new LinkedList<>();
        Elements elements = document.select("a");
        for (Element element : elements) {
            String absUrl = element.absUrl("href").indexOf(0) == '/'
                    ? siteModel.getUrl() + element.absUrl("href")
                    : element.absUrl("href");
            if (!absUrl.isEmpty()
                    && absUrl.startsWith(siteModel.getUrl())
                    && !setAbsUrls.contains(absUrl)
                    && !absUrl.contains("#")
                    && !patternUrl.matcher(absUrl).find()) {
                ParseSite parsingSite = new ParseSite(absUrl, siteModel, pageRepository, siteRepository, executorService);
                parsingSite.fork();
                tasks.add(parsingSite);
            }
        }
        for (ParseSite task : tasks) {
            task.join();
        }
    }

    private List<PageModel> getPageModel(SiteModel siteModel, String url) {
        List<PageModel> pageModelList = pageRepository.findByPathAndSiteModel(siteModel, url);
        return pageModelList;
    }
}
