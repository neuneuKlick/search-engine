package searchengine.services;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.Morphology;
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
    private final Morphology morphology;
    private final LemmaFinder lemmaFinder;

    @Override
    public SearchResponse searchResults(String query, String urlSite, int offset, int limit) {
        Map<String, Set<String>> lemmas = getLemmaSet(query);
        List<SiteModel> siteModels = getSiteModels(urlSite);
        for (SiteModel siteModel : siteModels) {
            List<LemmaModel> lemmaModels = getLemmaModels(siteModel, lemmas);
            HashMap<PageModel, Float> pageModels = getPageModels(siteModel, lemmaModels, limit);
        }


        return SearchResponse.builder().build();
    }

    private HashMap<PageModel, Float> getPageModels(SiteModel siteModel, Map<String, LemmaModel> lemmaModels,
                                                    int limit) {
        HashMap<PageModel, Float> pageModels = new HashMap<>();
        List<LemmaModel> list = new ArrayList<>();
        for (Map.Entry<String, LemmaModel> entry : lemmaModels.entrySet()) {
//            list.add();
        }
        String firstLemma;
//        pageRepository.findByLemma()

        return pageModels;
    }

    private Map<String,Set<String>> getLemmaSet(String searchText) {
        String[] splitText = searchText.split("\\s+");
        Map<String, Set<String>> result = new HashMap<>();
        for (String word : splitText) {
            List<String> lemma = lemmaFinder.g(word);
            Set<String> stringHashSet = new HashSet<>();
            stringHashSet.addAll(lemma);
            result.put(word, stringHashSet);
        }

        return result;
    }

    private List<LemmaModel> getLemmaModels(SiteModel siteModel, Map<String, Set<String>> lemmas) {
        Integer count = 0;
        List<LemmaModel> list = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : lemmas.entrySet()) {
            List<LemmaModel> lemmasModels = lemmaRepository.selectLemmasBySite(entry.getValue(), siteModel);
            if(!lemmasModels.isEmpty()){
                count++;
                list.addAll(lemmasModels);
            }
            if(count != lemmas.size()){
                return new ArrayList<>();
            }else {
                list.sort(Comparable::compareTo);
                return list;
            }
        }
    }

    private List<SiteModel> getSiteModels(String urlSite) {
        if (urlSite == null) {
            return siteRepository.findAll();
        } else {
            return Collections.singletonList(siteRepository.findByUrl(urlSite));
        }
    }
}
