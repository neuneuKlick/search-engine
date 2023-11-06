package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public class StartExecute implements Runnable {

    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private static ForkJoinPool fjp;
    private String url;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;

    public StartExecute(String url, SiteModel siteModel, PageRepository pageRepository, SiteRepository siteRepository,
                        NetworkService networkService, LemmaRepository lemmaRepository, IndexRepository indexRepository,
                        LemmaFinder lemmaFinder) {
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.networkService = networkService;
        fjp = new ForkJoinPool();
        this.url = url;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            ParseSite parsingSite = new ParseSite(url, siteModel, pageRepository, siteRepository, networkService,
                    lemmaRepository, indexRepository, lemmaFinder);
            fjp.invoke(parsingSite);

            if (!fjp.isShutdown()) {
                siteModel.setSiteStatus(SiteStatus.INDEXED);
                siteModel.setTimeStatus(LocalDateTime.now());
                siteRepository.save(siteModel);
                log.info("Site indexing " + siteModel.getName() + " is complected, in time: " + (System.currentTimeMillis() - start));
            }
            System.out.println("Duration: " + (System.currentTimeMillis() - start));
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

    private synchronized void siteIndexed() {
        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteModel.setTimeStatus(LocalDateTime.now());
        siteRepository.saveAndFlush(siteModel);
    }
}
