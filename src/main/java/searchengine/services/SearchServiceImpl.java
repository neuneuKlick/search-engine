package searchengine.services;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchInfo;
import searchengine.dto.statistics.SearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

import static searchengine.services.UrlFormatter.getShortUrl;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final NetworkService networkService;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lemmaFinder;
    private String rarestWords;
    private  Map<String, LemmaModel> necessaryLemmaModels = new HashMap<>();
    private int totalCountPages;
    private final Map<PageModel, Float> relevancePages = new HashMap<>();
    private final SnippetGenerator snippetGenerator;

    /**
     *
     * Перебрать сайты, найти релевантные страницы
     * Вернуть релеввантные страницы в списке
     *
     *
     * вернуть объект DTO с необходимой информацией
     */
    @Override
    public SearchResponse searchResults(String query, String urlSite, int offset, int limit) {
        if (query.isEmpty()) {
            return new SearchResponse(false, "Задан пустой поисковый запрос");
        }
        log.info("Поиск по запросу слов: " + query);

        /**
         * lemmas - это список уникальных слов, обработанные в лематизаторе и представленные в сущ. виде ед. числе
         */
        Set<String> lemmas = lemmaFinder.getLemmaSet(query); //шейнкман

        /**
         * Получаем модели сайтов из таблицы в БД по адресу и складываем их в список.
         */
        List<SiteModel> sites = getSites(urlSite);

        for (SiteModel siteModel : sites) {
            /**
             * Найдём нужные слова, в БД
             */
            //TODO заменить мой getLemmasFromDatabase на getLemmaEntitiesFromTable
            Map<String, LemmaModel> lemmaModels = getLemmasFromDatabase(lemmas, siteModel);

            /**
             * Исключить лишние часто встречаемые леммы
             */
            necessaryLemmaModels = getMostNecessaryFreqOccurringLemmaModels(lemmaModels);

            /**
             * Отсортировать
             */

            List<Map.Entry<String, LemmaModel>> sortedLemmasInOrderOfFrequency;
            sortedLemmasInOrderOfFrequency = new ArrayList<>(necessaryLemmaModels.entrySet());
            sortedLemmasInOrderOfFrequency.sort(Comparator.comparingInt(r -> r.getValue().getFrequency()));
            necessaryLemmaModels = sortedLemmasInOrderOfFrequency.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));


            //TODO По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.
            // Далее искать соответствия следующей леммы из этого списка страниц, а затем повторять операцию
            // по каждой следующей лемме. Список страниц при этом на каждой итерации должен уменьшаться.
            rarestWords = necessaryLemmaModels.keySet().stream()
                    .findFirst().orElse(null);


            //TODO Если страницы найдены, рассчитывать по каждой из них релевантность (и выводить её потом, см. ниже)
            // и возвращать.
            List<PageModel> pagesWithRarestLemmas = null;
            if (rarestWords != null) {
                List<Integer> listOFPagesById = getPageModelsId(siteModel);
                pagesWithRarestLemmas = pageRepository.findAllByIdIn(listOFPagesById);
            }

            List<PageModel> listNecessaryPages = getNecessaryPages(siteModel, pagesWithRarestLemmas);

            //TODO Рассчёт абсолютной релевантности
            Map<PageModel, Float> resultCalculationOfAbsoluteRelevance = new HashMap<>();
            for (PageModel pageModel : listNecessaryPages) {
                resultCalculationOfAbsoluteRelevance.put(pageModel, getSumOfLemmasRank(necessaryLemmaModels, pageModel));
                relevancePages.putAll(resultCalculationOfAbsoluteRelevance);
            }

        }


        //TODO Получаем максимальный ранк
        Float aFloat = relevancePages.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(null);
        float maxValue = aFloat == null ? 1 : aFloat;

        //TODO Расчитываем релевантность
        Map<PageModel, Float> resultsComparativeRelevance = relevancePages.keySet().stream()
                .collect(Collectors.toMap(pageModel -> pageModel,
                        p -> relevancePages.get(p) / maxValue,
                        (a, b) -> b));

        Map<PageModel, Float> resultsComparing = relevancePages.entrySet().stream()
                .sorted(Map.Entry.<PageModel, Float>comparingByValue()
                        .reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        //TODO Отсортируем  в порядке убывания значений
        List<Map.Entry<PageModel, Float>> sortedDesc = new ArrayList<>(resultsComparing.entrySet());
        sortedDesc.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        //TODO Выберем подмножество элементов
        int lastIndex = Math.min(offset + limit, resultsComparing.size());
        List<Map.Entry<PageModel, Float>> subList = sortedDesc.subList(offset, lastIndex);

        //TODO Создадим коллекцию с выбранными элементами
        Map<PageModel, Float> resultEntrySubList = new LinkedHashMap<>();
        for (Map.Entry<PageModel, Float> entry : subList) {
            resultEntrySubList.put(entry.getKey(), entry.getValue());
        }

        Map<PageModel, Float> sortedSubList = resultEntrySubList;


        //TODO getSearchData
        List<SearchInfo> infoList = new ArrayList<>();
        for (PageModel pageModel : sortedSubList.keySet()) {
            SearchInfo searchInfo = getSearchInfo(lemmas, sortedSubList, pageModel);
            infoList.add(searchInfo);
        }


        List<SearchInfo> resultsSearchInfo = new ArrayList<>(infoList);

        return SearchResponse.builder()
                .results(true)
                .error("")
                .count(6)
                .listSearchInfo(resultsSearchInfo)
                .build();
    }

    private Map<String, LemmaModel> getLemmasFromDatabase(Set<String> lemmas, SiteModel siteModel) {
        return lemmas.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, siteModel))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(LemmaModel::getLemma, currentLemmaModel -> currentLemmaModel));
    }

    private Map<String, LemmaModel> getMostNecessaryFreqOccurringLemmaModels(Map<String, LemmaModel> lemmaModels) {
        float limitOfFreq = 98.5F;
        return lemmaModels.entrySet().stream()
                .filter(value -> value.getValue().getFrequency() < limitOfFreq)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @NotNull
    private List<Integer> getPageModelsId(SiteModel siteModel) {
        return Objects.requireNonNull(getRarestIndexSet(siteModel, rarestWords)).stream()
                .map(IndexModel::getPage)
                .map(PageModel::getId)
                .collect(Collectors.toList());
    }


    private List<SiteModel> getSites(String urlAddressSite) {
        if (urlAddressSite == null) {
            return siteRepository.findAll();
        } else {
            return Collections.singletonList(siteRepository.findByUrl(urlAddressSite));
        }
    }

    private @Nullable Set<IndexModel> getRarestIndexSet(SiteModel siteModel, String rarestWords) {
        LemmaModel rarestLemmaModel = lemmaRepository.findByLemmaAndSite(rarestWords, siteModel);
        if (rarestLemmaModel != null) {
            return indexRepository.findAllByLemma(rarestLemmaModel);
        }
        return null;
    }

    @NotNull
    private List<PageModel> getNecessaryPages(SiteModel siteModel, List<PageModel> pagesWithRarestLemmas) {

        Map<String, LemmaModel> lemmaModelMaps = necessaryLemmaModels.entrySet().stream()
                .filter(r -> !r.getValue().equals(rarestWords))
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(LemmaModel::getFrequency)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        List<PageModel> listNecessaryPages = new ArrayList<>(pagesWithRarestLemmas);

        for (String necessaryLemma : lemmaModelMaps.keySet()) {
            LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSite(necessaryLemma, siteModel);
            Set<IndexModel> indexModels = indexRepository.findAllByLemma(lemmaModel);

            listNecessaryPages.stream()
                    .filter(pageModel -> indexModels.stream()
                            .anyMatch(i -> Objects.equals(i.getPage().getId(), pageModel.getId())))
                    .toList();

            List<PageModel> list = listNecessaryPages;
            listNecessaryPages.clear();
            listNecessaryPages.addAll(list);
        }
        totalCountPages += listNecessaryPages.size();

        return listNecessaryPages;
    }

    private float getSumOfLemmasRank(Map<String, LemmaModel> lemmasProcessed, PageModel pageModel) {
        float result = 0.0F;
        for (LemmaModel lemmaModel : lemmasProcessed.values()) {
            IndexModel indexModel = indexRepository.findByLemmaAndPage(lemmaModel, pageModel);
            if (indexModel != null) {
                result += indexModel.getRank();
            }
        }
        return result;
    }

    private SearchInfo getSearchInfo(Set<String> queryLemmas, Map<PageModel, Float> subsetMap, PageModel pageModel) {
        return SearchInfo.builder()
                .url(pageModel.getPath())
                .siteName(pageModel.getSite().getName())
                .site(getShortUrl(pageModel.getSite().getUrl()).replaceFirst("/$", ""))
                .snippet(getSnippet(pageModel, new ArrayList<>(queryLemmas)))
                .relevance(subsetMap.get(pageModel))
                .title(getTitle(pageModel.getContent())).build();
    }

    private String getSnippet(PageModel pageModel, List<String> lemmas) {
        snippetGenerator.setText(pageModel.getContent());
        snippetGenerator.setQueryWords(lemmas);
        return snippetGenerator.generateSnippets();
    }

    private String getTitle(String sourceHtml) {
        Document doc = Jsoup.parse(sourceHtml);
        return doc.title();
    }
}
