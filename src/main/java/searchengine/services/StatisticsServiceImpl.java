package searchengine.services;

import lombok.RequiredArgsConstructor;
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


import java.lang.constant.Constable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            SiteStatus siteStatus = siteRepository.findByUrl(site.getUrl());
            if (siteStatus != null && siteStatus.equals(SiteStatus.INDEXING)) {
                total.setIndexing(true);
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());

            SiteModel siteModel = siteRepository.findSiteModelByUrl(site.getUrl());

            int countPage = pageRepository.findCountBySite(siteModel) == null ? 0 :
                    pageRepository.findCountBySite(siteModel);
            item.setPages(countPage);
            int countLemma = lemmaRepository.countBySite(siteModel);
            item.setLemmas(countLemma);
            item.setStatus(siteStatus == null ? "" : siteStatus.toString());
            Constable errorSite = siteRepository.findErrorByUrl(site.getUrl());
            item.setError(errorSite == null ? "" : errorSite.toString());
            item.setStatusTime(new Date().getTime());

            total.setPages(total.getPages() + countPage);
            total.setLemmas(total.getLemmas() + countLemma);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
