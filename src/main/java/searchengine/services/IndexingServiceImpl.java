package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.PageInfo;
import searchengine.exeption.RuntimeException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;



@Slf4j
@RequiredArgsConstructor
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaServiceImpl lemmaService;

    @Override
    public IndexingResponse startIndexing() {
        if (siteRepository.existsBySiteStatus(SiteStatus.INDEXING)) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        List<Site> sitesList = sites.getSites();
        deleteSites();

        for (Site site : sitesList) {
            String url = site.getUrl();
            log.info("Индексируется сайт: {}", url);
            siteRepository.save(SiteModel.builder()
                    .name(site.getName())
                    .siteStatus(SiteStatus.INDEXING)
                    .url(url.toLowerCase())
                    .timeStatus(LocalDateTime.now())
                    .build());
        }

        for (SiteModel siteModel : siteRepository.findAll()) {
            new Thread(()-> new ForkJoinPool().invoke(
                    new ParseSite(siteModel.getUrl(),siteModel.getId(), siteRepository,
                            pageRepository, networkService, lemmaService, true))).start();
        }
        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!siteRepository.existsBySiteStatus(SiteStatus.INDEXING)) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        log.info("Индексация остановлена");
        siteRepository.findAllBySiteStatus(SiteStatus.INDEXING).forEach(site -> {
            log.info("Индексация остановлена пользователем");
            site.setLastError("Индексация остановлена пользователем");
            site.setSiteStatus(SiteStatus.FAILED);
            siteRepository.save(site);
            lemmaService.updateLemmasFrequency(site.getId());
        });
        return new IndexingResponse(true, "");
    }


    @Override
    public IndexingResponse indexingPage(String url) {
        String siteUrl = "";
        String path = "/";
        try {
            URL gotUrl = new URL(url);
            siteUrl = gotUrl.getProtocol() + "://" + gotUrl.getHost() + "/";
            path = gotUrl.getPath();
        } catch (MalformedURLException e) {
            log.error("Ошибка парсинга URL, ", e);
        }

        path = path.trim();
        path = path.isBlank() ? "/" : path;
        Optional<SiteModel> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
        if (optional.isPresent()) {
            SiteModel siteModel = optional.get();

            indexing(siteModel.getId());
            deletePage(siteModel, path);
            parsePage(siteModel, path);

            Optional<PageModel> optionalPage = pageRepository.findBySiteAndPath(siteModel, path);
            if (optionalPage.isPresent()) {
                PageModel pageModel = optionalPage.get();

                lemmaService.findAndSave(pageModel);
                lemmaService.updateLemmasFrequency(siteModel.getId());
                indexed(siteModel.getId());
                return new IndexingResponse(true, "");
            }
            log.warn("Страница не найдена: {}", path);
            return new IndexingResponse(false, "Страница не найдена");
        }
        log.warn("Сайт не найден: {}", siteUrl);
        return new IndexingResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }

    private void deleteSites() {
        log.info("Все сайты удалены");
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();

    }
    private void indexing(int siteId) {
        SiteModel siteModel = siteRepository.findById(siteId)
                .orElseThrow(()-> new IllegalStateException("Сайт не найден"));
        siteModel.setSiteStatus(SiteStatus.INDEXING);
        siteRepository.save(siteModel);
    }

    private void deletePage(SiteModel siteModel, String url) {
        Optional<PageModel> optionalPage = pageRepository.findBySiteAndPath(siteModel, url);

        if (optionalPage.isPresent()) {
            PageModel page = optionalPage.get();
            List<IndexModel> indexes = page.getIndexes();
            for (IndexModel indexModel : indexes) {
                indexRepository.delete(indexModel);
                LemmaModel lemmaModel = indexModel.getLemma();
                if (lemmaModel.getFrequency() <= 0) {
                    lemmaRepository.delete(lemmaModel);
                } else {
                    lemmaModel.setFrequency(lemmaModel.getFrequency() - 1);
                    lemmaRepository.save(lemmaModel);
                }
            }
            pageRepository.delete(page);
        }
    }

    private void parsePage(SiteModel site, String path) {
        PageInfo pageInfo;
        try {
            pageInfo = networkService.getPageInfo(site.getUrl() + path);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }

        pageRepository.save(PageModel.builder()
                .site(site)
                .path(path)
                .codeResponse(pageInfo.getStatusCode())
                .content(pageInfo.getContent())
                .build());
    }

    private void indexed(int siteId) {
        SiteModel siteModel = siteRepository.findById(siteId).orElseThrow(()-> new IllegalStateException("Site not found"));
        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteRepository.save(siteModel);
        log.info("Indexing finished");
    }
}
