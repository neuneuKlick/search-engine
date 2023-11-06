package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class ParseSite extends RecursiveAction {

    private final String url;
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static NetworkService networkService;
    public static volatile Boolean isInterrupted = false;
    private final static Set<String> urlList = new HashSet<>();
    private final ExecutorService executorService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;


    public ParseSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository,
                     ExecutorService executorService, LemmaRepository lemmaRepository, IndexRepository indexRepository,
                     LemmaFinder lemmaFinder) {
        this.url = url.trim();
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.executorService = executorService;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }

    public ParseSite(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository,
                     NetworkService networkService, LemmaRepository lemmaRepository,
                     IndexRepository indexRepository, LemmaFinder lemmaFinder) throws IOException{
        this.url = url;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
        this.executorService = Executors.newFixedThreadPool(16);
        ParseSite.networkService = networkService;

    }

    @Override
    protected void compute() {
        if (isInterrupted) {
            StartExecute.shutdown();
            return;
        }
        log.info("log test");
        List<ParseSite> taskList = new LinkedList<>();
        try {
            System.out.println("Connection: " + url);
            Thread.sleep(150);

            Connection.Response response = networkService.getConnection(url);
            if (!networkService.isAvailable(response)) {
                System.out.println("WOW");
                return;
            }
            Document doc = response.parse();

            PageModel pageModel = new PageModel();
            pageModel.setSiteModel(siteModel);
            pageModel.setPath(url.substring(siteModel.getUrl().length() - 1));
            pageModel.setCodeResponse(200);
            pageModel.setContent(doc.outerHtml());
            pageRepository.saveAndFlush(pageModel);

            Future<?> submit = executorService.submit(new LemmaRun(siteModel, pageModel,
                    indexRepository, lemmaRepository, lemmaFinder));
            submit.get();

            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String attrUrl = element.absUrl("href");
                if (!attrUrl.isEmpty()
                        && attrUrl.startsWith(url)
                        && !urlList.contains(attrUrl)
                        && !attrUrl.contains("#") //
                        && !attrUrl.matches("\\.[0-9a-z]{1,5}$")
                        && !attrUrl.equals(url)) {
                    urlList.add(attrUrl);
                    ParseSite task = new ParseSite(attrUrl, siteModel, pageRepository, siteRepository,
                            executorService, lemmaRepository, indexRepository, lemmaFinder);
                    task.fork();
                    taskList.add(task);
                }
            }
            for (ParseSite task : taskList) {
                task.join();
            }
        } catch (HttpStatusException e) {

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();

        }
    }

    public static void clearUrlList() {
        urlList.clear();
    }
}
