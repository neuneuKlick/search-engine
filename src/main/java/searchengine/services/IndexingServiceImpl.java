package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

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
                            pageRepository, siteRepository, networkService);
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
        if (url.isEmpty()) {
            return new IndexingResponse(false, "Вы ввели неверный url");
        }
        for (Site site : sites.getSites()) {
            if (url.contains(site.getUrl())) {
                return new IndexingResponse(true, "");
            }
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
}
