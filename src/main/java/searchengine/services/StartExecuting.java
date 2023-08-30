package searchengine.services;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public class StartExecuting implements Runnable {
    private final SiteModel siteModel;
    private final NetworkService networkService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static ForkJoinPool fjp = new ForkJoinPool();

    public StartExecuting(SiteModel siteModel, NetworkService networkService, SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteModel = siteModel;
        this.networkService = networkService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        fjp = new ForkJoinPool();
    }

    @Override
    public void run() {

        long startTime = System.currentTimeMillis();
        ParsingSite parsingSite = new ParsingSite(siteModel.getUrl(), siteModel, networkService, siteRepository, pageRepository);
        fjp.invoke(parsingSite);

        if (!fjp.isShutdown()) {
            siteIndexed();
            log.info("Индексация сайта " + siteModel.getName() + " завершена, за время: " + (System.currentTimeMillis() - startTime));
        }


    }

    public static void shutdown() {
        if (fjp != null && !fjp.isShutdown()) {
            fjp.shutdownNow();
        }
    }

    private void siteIndexed() {
        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteModel.setTimeStatus(new Date());
        siteRepository.saveAndFlush(siteModel);
    }
}
