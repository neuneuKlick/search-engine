package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.UnsupportedMimeTypeException;
import searchengine.dto.statistics.PageInfo;
import searchengine.exeption.RuntimeException;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ParseSite extends RecursiveAction {
    private final String url;
    private final Integer siteId;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final NetworkService networkService;
    private final LemmaServiceImpl lemmaService;
    private final boolean isAction;

    @SneakyThrows
    @Override
    protected void compute() {
        if (isNotFailed(siteId) && isNotVisited(siteId, url)) {
            try {
                SiteModel siteModel = getSiteModel(siteId);
                siteModel.setTimeStatus(LocalDateTime.now());
                siteRepository.save(siteModel);
                Optional<PageModel> optionalPageModel = savePage(siteId, url);
                if (optionalPageModel.isPresent()) {
                    PageModel pageModel = optionalPageModel.get();
                    if (pageModel.getCodeResponse() < 400) {
                        lemmaService.findAndSave(pageModel);
                    }
                    Set<ForkJoinTask<Void>> tasks = networkService.getPaths(pageModel.getContent()).stream()
                            .map(pathFromPage -> new ParseSite(pathFromPage, siteId,
                                    siteRepository, pageRepository,
                                    networkService,lemmaService, false).fork())
                            .collect(Collectors.toSet());
                    tasks.forEach(ForkJoinTask::join);
                }
                if (isAction && isNotFailed(siteId)) {
                    indexed(siteId);
                    lemmaService.updateLemmasFrequency(siteId);
                    log.info("Сайт закончил индексацию");
                }

            } catch (UnsupportedMimeTypeException e) {
                throw new RuntimeException(e.getMessage());
            } catch (Exception e) {
                log.error("Exception: ", e);
                failed(siteId, "Ошибка парсинга URL: " + getSiteModel(siteId).getUrl() + url);
                lemmaService.updateLemmasFrequency(siteId);
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private boolean isNotFailed(Integer siteId) {
        return !siteRepository.existsByIdAndSiteStatus(siteId, SiteStatus.FAILED);
    }

    private boolean isNotVisited(Integer siteId, String path) {
        return !pageRepository.existsBySiteIdAndPath(siteId, path);
    }

    private SiteModel getSiteModel(Integer siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new IllegalStateException("Сайт не найден"));
    }

    public Optional<PageModel> savePage(Integer siteId, String path) throws IOException, InterruptedException {
        SiteModel siteModel = getSiteModel(siteId);
        PageInfo pageInfo = networkService.getPageInfo(siteModel.getUrl() + path);
        if (isNotVisited(siteId, path)) {
            return Optional.of(pageRepository.save(PageModel.builder()
                    .site(siteModel)
                    .path(path)
                    .codeResponse(pageInfo.getStatusCode())
                    .content(pageInfo.getContent())
                    .build()));
        } else {
            return Optional.empty();
        }
    }

    private void failed(Integer siteId, String error) {
        log.warn("Ошибка индексации сайта {}: {}", siteId, error);
        SiteModel persistSite = getSiteModel(siteId);
        persistSite.setLastError(error);
        persistSite.setSiteStatus(SiteStatus.FAILED);
        siteRepository.save(persistSite);
    }

    private void indexed(Integer siteId) {
        SiteModel siteModel = getSiteModel(siteId);
        siteModel.setTimeStatus(LocalDateTime.now());
        siteModel.setSiteStatus(SiteStatus.INDEXED);
        siteRepository.save(siteModel);
    }

}
