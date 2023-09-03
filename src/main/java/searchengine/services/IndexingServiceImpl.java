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

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private final Pattern pattern = Pattern.compile("^(https?://)?([\\w-]{1,32}\\.[\\w-]{1,32})[^\\s@]*$");

    @SneakyThrows
    @Override
    public IndexingResponse startIndexing() {
        ParsingSite.stop = false;
        if (isIndexing()) {
            return new IndexingResponse(false, "Indexing is already running");
        }
        Thread thread = new Thread(() -> {
            log.info("Indexing started");
            siteRepository.deleteAll();
            ParsingSite.clearSetAbsUrl();
            for (Site site : sitesList.getSites()) {
                SiteModel siteModel = getSiteModel(site, SiteStatus.INDEXING);
                siteRepository.saveAndFlush(siteModel);
                StartExecuting startExecute = new StartExecuting(siteModel, pageRepository, siteRepository, networkService);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(startExecute);
            }
        });
        thread.start();
        System.out.println();

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true, "");
    }

    private SiteModel getSiteModel(Site site, SiteStatus siteStatus) {
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteStatus(siteStatus);
        siteModel.setTimeStatus(new Date());
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteModel.setLastError("");
        return siteModel;
    }

    private boolean isIndexing() {
        List<SiteModel> all = siteRepository.findAll();
        for (SiteModel siteModel : all) {
            if (siteModel.getSiteStatus().equals(SiteStatus.INDEXING))
                return true;
        }
        return false;
    }

}
