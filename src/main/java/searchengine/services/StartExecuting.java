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
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private static ForkJoinPool fjp;

    public StartExecuting(SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository, NetworkService networkService) {
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.networkService = networkService;
        fjp = new ForkJoinPool();
    }

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            ParsingSite parsingSite = new ParsingSite(siteModel.getUrl(), siteModel, pageRepository, siteRepository, networkService);
            fjp.invoke(parsingSite);

            if (!fjp.isShutdown()) {
                siteIndexed();
                log.info("Site indexing " + siteModel.getName() + " is complected, in time: " + (System.currentTimeMillis() - startTime));
            }
        } catch (MalformedURLException m) {
            log.info("Interrupted I/O operations " + m.getMessage());
        } catch (IOException i) {
            log.info("Error StartExecuting class " + i.getMessage());
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
