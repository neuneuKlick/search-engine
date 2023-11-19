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


@RequiredArgsConstructor
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics(
                siteRepository.count(),
                pageRepository.count(),
                lemmaRepository.count(),
                !siteRepository.existsBySiteStatusNot(SiteStatus.INDEXED)
        );

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteModel> sitesList = siteRepository.findAll();
        for (SiteModel site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(site.getPages().size());
            item.setLemmas(site.getLemmas().size());
            item.setStatus(String.valueOf(site.getSiteStatus()));
            item.setError(site.getLastError());
            item.setStatusTime(site.getTimeStatus());
            detailed.add(item);
        }

        StatisticsData statistics = new StatisticsData(total, detailed);
        return new StatisticsResponse(statistics);
    }
}
