package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        int totalCountPages = 0;
        int totalCountLemmas = 0;
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            SiteModel siteModel = siteRepository.findByUrl(site.getUrl());
            int countPages = pageRepository.countBySite(siteModel);
            int countLemmas = lemmaRepository.countBySite(siteModel);
            totalCountPages += countPages;
            totalCountLemmas += countLemmas;
            DetailedStatisticsItem item = getDetailedStatisticsItem(site, countPages, countLemmas, siteModel);
            detailed.add(item);
        }
        TotalStatistics totalStatistics = getTotalStatistics(totalCountPages, totalCountLemmas);
        StatisticsData statisticsData = getStatisticsData(detailed, totalStatistics);

        return getResponse(statisticsData);
    }

    private TotalStatistics getTotalStatistics(int totalPages, int totalLemmas) {
        return TotalStatistics.builder()
                .sites(sites.getSites().size())
                .indexing(getIsIndexingStarted())
                .lemmas(totalLemmas)
                .pages(totalPages).build();
    }

    private StatisticsData getStatisticsData(List<DetailedStatisticsItem> detailed, TotalStatistics total) {
        return StatisticsData.builder()
                .total(total)
                .detailed(detailed).build();
    }

    private DetailedStatisticsItem getDetailedStatisticsItem(Site site, int pages, int lemmas, SiteModel siteModel) {
        return DetailedStatisticsItem.builder()
                .name(site.getName())
                .url(site.getUrl())
                .pages(pages)
                .lemmas(lemmas)
                .status(siteModel.getSiteStatus().toString())
                .error(siteModel.getLastError())
                .statusTime(Date.from(siteModel.getTimeStatus().atZone(ZoneId.systemDefault()).toInstant())).build();
    }

    private StatisticsResponse getResponse(StatisticsData statisticsData) {
        return StatisticsResponse.builder()
                .statistics(statisticsData)
                .result(true).build();
    }

    private Boolean getIsIndexingStarted() {
        List<SiteModel> sites = siteRepository.findAll();
        for (SiteModel siteModel : sites) {
            if (siteModel.getSiteStatus() == SiteStatus.INDEXING) {
                return true;
            }
        }
        return false;
    }
}
