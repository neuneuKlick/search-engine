package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;

    @SneakyThrows
    @Override
    public IndexingResponse startIndexing() {
        long start = System.currentTimeMillis();
        ParseSite.isInterrupted = false;
        if (isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        siteRepository.deleteAll();
        ParseSite.clearUrlList();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            SiteModel siteModel = new SiteModel();
            siteModel.setSiteStatus(SiteStatus.INDEXING);
            siteModel.setTimeStatus(LocalDateTime.now());
            siteModel.setUrl(sitesList.get(i).getUrl());
            siteModel.setName(sitesList.get(i).getName());
            siteModel.setLastError("");
            siteRepository.save(siteModel);

            try {
                int lambdaVariable = i;
                Runnable task = () -> {
                    StartExecute startExecute = new StartExecute(sitesList.get(lambdaVariable).getUrl(), siteModel,
                            pageRepository, siteRepository, networkService,
                            lemmaRepository, indexRepository, lemmaFinder);
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.submit(startExecute);
                };
                new Thread(task).start();

            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
        System.out.println("Duration: " + (System.currentTimeMillis() - start));
        return new IndexingResponse(true, "");
    }

    @SneakyThrows
    @Override
    public IndexingResponse stopIndexing() {
        log.info("stopIndexing test");
        ParseSite.isInterrupted = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(() -> {
            List<SiteModel> allSiteModel = siteRepository.findAll();
            for (SiteModel siteModel : allSiteModel) {
                siteModel.setSiteStatus(SiteStatus.FAILED);
                siteModel.setLastError("Индексация остановлена пользователем");
                siteRepository.saveAndFlush(siteModel);
            }
        }, 1000, TimeUnit.MILLISECONDS);
        return new IndexingResponse(true, "");
    }


    @Override
    public IndexingResponse indexingPage(String url) {
        try {
            if (url.isEmpty()) {
                return new IndexingResponse(false, "Вы ввели неверный url");
            }
            Connection.Response connection = networkService.getConnection(url);
            Document document = connection.parse();

            for (Site site : sites.getSites()) {
                SiteModel siteModel = siteRepository.findSiteModelByUrl(site.getUrl());
                if (url.contains(site.getUrl()) && siteModel == null) {
                    SiteModel newSiteModel = getSiteModel(site, SiteStatus.INDEXED);
                    siteRepository.saveAndFlush(newSiteModel);
                    PageModel newPageModel = getPageModel(document, url.substring(site.getUrl().length()), newSiteModel);
                    pageRepository.saveAndFlush(newPageModel);
                    return new IndexingResponse(true, "");
                }
                if (url.contains(site.getUrl()) && siteModel != null) {
                    return new IndexingResponse(false, "");
                }
            }
        } catch (IOException e) {
            return new IndexingResponse(false, "Сайт не доступен");
        }
        return new IndexingResponse(false, "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");

    }

    private boolean isIndexing() {
        List<SiteModel> allSiteModel = siteRepository.findAll();
        for (SiteModel siteModel : allSiteModel) {
            if (siteModel.getSiteStatus().equals(SiteStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }
    private SiteModel getSiteModel(Site site, SiteStatus siteStatus) {
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteStatus(siteStatus);
        siteModel.setTimeStatus(LocalDateTime.now());
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteModel.setLastError("");
        return siteModel;
    }
    private PageModel getPageModel(Document document, String path, SiteModel siteModel) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteModel(siteModel);
        pageModel.setPath(path);
        pageModel.setCodeResponse(200);
        pageModel.setContent(document.html());
        return pageModel;
    }
}
