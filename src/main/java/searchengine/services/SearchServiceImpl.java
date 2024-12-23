package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchInfo;
import searchengine.dto.statistics.SearchResponse;
import searchengine.exeption.SearchIndexingRuntimeException;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final NetworkService networkService;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lemmaFinder;

    @Override
    public SearchResponse searchResults(String query, String urlSite, int offset, int limit) {
        log.info("Поиск запроса: " + query);
        Map<String, Integer> lemmasFromLemmaFinder = lemmaFinder.collectLemmas(query);
        Map<LemmaModel, Integer> lemmaModelsAndFreq = getLemmaModelsAndFreq(urlSite, lemmasFromLemmaFinder);
        assert lemmaModelsAndFreq != null;
        Map<LemmaModel, Integer> sortedLemmasByFreq = getSortedLemmasByFreq(lemmaModelsAndFreq);
        Set<PageModel> pageModels = getPageModels(sortedLemmasByFreq.keySet());

        if (!pageModels.isEmpty()) {
            Map<PageModel, Double> relevantPages = getRelevantPages(pageModels, sortedLemmasByFreq.keySet());
            List<SearchInfo> organizedSearch = searchByRelPages(relevantPages, query);
            List<SearchInfo> subInfo = subList(organizedSearch, offset, limit);
            return new SearchResponse(true, organizedSearch.size(), subInfo);
        }
        return new SearchResponse(false, "Запрос не найден");
    }

    private Set<PageModel> getPageModels(Set<LemmaModel> sortedLemmasByFreq) {
        if (sortedLemmasByFreq.isEmpty()) {
            return Set.of();
        }

        List<LemmaModel> lemmas = sortedLemmasByFreq.stream().toList();
        Set<PageModel> pages = lemmas.get(0).getIndexes().stream()
                .map(IndexModel::getPage)
                .collect(Collectors.toSet());

        for (int i = 1; i < lemmas.size(); i++) {
            pages = indexRepository.findAllByLemmaAndPageIn(lemmas.get(i), pages)
                    .stream().map(IndexModel::getPage)
                    .collect(Collectors.toSet());
            if (pages.isEmpty()) {
                return pages;
            }
        }
        return pages;
    }

    private List<SearchInfo> searchByRelPages(Map<PageModel, Double> relevantPages, String query) {
        List<SearchInfo> collector = new ArrayList<>();
        for (Map.Entry<PageModel, Double> pageRelevanceEntry : relevantPages.entrySet()) {
            String title = networkService.getTitle(pageRelevanceEntry.getKey().getContent());
            String snippet = correctionSnippet(query, pageRelevanceEntry.getKey());
            if (!title.isEmpty() && !snippet.isEmpty()) {
                SearchInfo searchInfo = new SearchInfo();
                searchInfo.setSite(pageRelevanceEntry.getKey().getSite().getUrl());
                searchInfo.setSiteName(pageRelevanceEntry.getKey().getSite().getName());
                searchInfo.setUri(pageRelevanceEntry.getKey().getPath());
                searchInfo.setTitle(title);
                searchInfo.setSnippet(snippet);
                searchInfo.setRelevance(pageRelevanceEntry.getValue());
                collector.add(searchInfo);
            }
        }
        log.info("{} найдено результатов", collector.size());
        return collector;
    }

    private String correctionSnippet (String query, PageModel pageModel) {

        String text = networkService.htmlToText(pageModel.getContent());
        List<String> searchWords = getSearchWords(text, query);
        StringBuilder snippet = new StringBuilder();
        final int byOneValue = 100;
        final int byMoreValues = 40;
        int sideStep = searchWords.size() > 1 ? byMoreValues : byOneValue;

        for (String word : searchWords) {
            if (text.contains(word)) {
                int firstIndex = text.indexOf(word);
                int lastIndex = firstIndex + word.length();
                String before;
                String after;

                if (firstIndex - sideStep < 0) {
                    before = "..." + text.substring(0, firstIndex) + " <b>";
                } else {
                    before = "..." + text.substring(firstIndex - sideStep, firstIndex) + " <b>";
                }

                if (lastIndex + sideStep > text.length()) {
                    after = "</b> " + text.substring(lastIndex) + "...";
                } else {
                    after = "</b> " + text.substring(lastIndex, firstIndex + sideStep) + "...";
                }
                String keyWord = text.substring(firstIndex, lastIndex);
                snippet.append(before).append(keyWord).append(after);
            }
        }
        return snippet.toString();
    }

    private List<String> getSearchWords(String cleanText, String searchQuery) {
        Map<String, String> lemmasToWordsInText = lemmaFinder.collectLemmasAndWords(cleanText);
        Map<String, String> searchLemmasToWords = lemmaFinder.collectLemmasAndWords(searchQuery);
        List<String> searchWords = new ArrayList<>();
        for (Map.Entry<String, String> queryEntry : searchLemmasToWords.entrySet()) {
            for (Map.Entry<String, String> textEntry : lemmasToWordsInText.entrySet()) {
                if (textEntry.getKey().equals(queryEntry.getKey())) {
                    searchWords.add(textEntry.getValue());
                }
            }
        }
        return searchWords;
    }

    private Map<LemmaModel, Integer> getLemmaModelsAndFreq(String query, Map<String, Integer> lemmaModels) {
        Map<LemmaModel, Integer> newMap = new HashMap<>();
        if (!query.isEmpty()) {
            String siteUrl = "";
            try {
                URL gotUrl = new URL(query);
                siteUrl = gotUrl.getProtocol() + "://" + gotUrl.getHost() + "/";
            } catch (MalformedURLException e) {
                log.error("Ошибка парсинга по url, ", e);
                throw new SearchIndexingRuntimeException(e.getMessage());
            }

            Optional<SiteModel> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
            if (optional.isPresent()) {
                SiteModel siteToSearch = optional.get();
                for (String lemma : lemmaModels.keySet()) {
                    Optional<LemmaModel> optionalLemma = lemmaRepository.findBySiteAndLemma(siteToSearch, lemma);
                    optionalLemma.ifPresent(lemmaEntity -> newMap.put(lemmaEntity, lemmaEntity.getFrequency()));
                }
                return newMap;
            }
            return null;
        }

        for (String lemma : lemmaModels.keySet()) {
            List<Optional<LemmaModel>> optionalLemmas = lemmaRepository.findByLemma(lemma);
            if (!optionalLemmas.isEmpty()) {
                newMap.put(optionalLemmas.get(0).get(), optionalLemmas.get(0).get().getFrequency());
            }
        }
        return newMap;
    }

    private Map<LemmaModel, Integer> getSortedLemmasByFreq(Map<LemmaModel, Integer> mapToSort) {
        return mapToSort.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private Map<PageModel, Double> getRelevantPages(Set<PageModel> pageModels, Set<LemmaModel> sortedLemmas) {
        List<PageModel> pageList = pageModels.stream().toList();
        Map<PageModel, Set<Float>> ranksPerPage = new HashMap<>();
        for (int i = 1; i < pageList.size(); i++) {
            Set<Float> ranks = indexRepository.findAllByPageAndLemmaIn(pageList.get(i), sortedLemmas).stream()
                    .map(IndexModel::getRank)
                    .collect(Collectors.toSet());
            ranksPerPage.put(pageList.get(i), ranks);
        }

        Map<PageModel, Integer> absoluteRelevantPages = new HashMap<>();
        for (Map.Entry<PageModel, Set<Float>> ranksValue : ranksPerPage.entrySet()) {
            int sum = 0;
            for (Float rank : ranksValue.getValue()) {
                sum += rank;
            }
            absoluteRelevantPages.put(ranksValue.getKey(), sum);
        }
        Map<PageModel, Integer> sortReversedPagesByValues = getSortReversedPagesByValues(absoluteRelevantPages);
        Map<PageModel, Double> relevantPages = new HashMap<>();
        int maxRankValue = sortReversedPagesByValues.values().stream()
                .max(Comparator.comparing(Integer::intValue)).get();

        sortReversedPagesByValues.forEach((key, value) -> {
            double result = (double) value / maxRankValue;
            relevantPages.put(key, result);
        });
        return relevantPages;
    }

    private Map<PageModel, Integer> getSortReversedPagesByValues(Map<PageModel, Integer> absoluteRelevantPages) {
        return absoluteRelevantPages.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(100)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private List<SearchInfo> subList(List<SearchInfo> searchInfo, Integer offset, Integer limit) {
        int fromIndex = offset;
        int toIndex = fromIndex + limit;

        if (toIndex > searchInfo.size()) {
            toIndex = searchInfo.size();
        }
        if (fromIndex > toIndex) {
            return List.of();
        }
        return searchInfo.subList(fromIndex, toIndex);
    }
}
