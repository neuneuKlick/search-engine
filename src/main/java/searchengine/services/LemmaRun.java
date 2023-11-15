package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.Morphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class LemmaRun implements Runnable {
    private final SiteModel siteModel;
    private final PageModel pageModel;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lemmaFinder;


    public LemmaRun(SiteModel siteModel, PageModel pageModel, IndexRepository indexRepository,
                    LemmaRepository lemmaRepository, LemmaFinder lemmaFinder) {
        this.siteModel = siteModel;
        this.pageModel = pageModel;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.lemmaFinder = lemmaFinder;
    }
    @Override
    public void run() {
        String title = clearHtml(pageModel.getContent(), "title");
        String body = clearHtml(pageModel.getContent(), "body");
        String concatContent = title.concat(" " + body);
        Map<String, Integer> lemmaList = lemmaFinder.collectLemmas(concatContent);
        Set<String> words = new HashSet<>(lemmaList.keySet());
        for (String lemma : words) {
            Integer countLemma = lemmaList.get(lemma);
            synchronized (siteModel) {
                LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSite(lemma, siteModel);
                if (lemmaModel == null) {
                    LemmaModel newLemmaModel = new LemmaModel();
                    newLemmaModel.setSite(siteModel);
                    newLemmaModel.setLemma(lemma);
                    newLemmaModel.setFrequency(1);
                    lemmaModel = lemmaRepository.saveAndFlush(newLemmaModel);
                } else {
                    lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
                    lemmaRepository.saveAndFlush(lemmaModel);
                }
                IndexModel newIndexModel = new IndexModel();
                newIndexModel.setPage(pageModel);
                newIndexModel.setLemma(lemmaModel);
                newIndexModel.setRank(countLemma);
                indexRepository.saveAndFlush(newIndexModel);
            }
        }


    }

    private String clearHtml(String content, String cssQuery) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select(cssQuery);
        StringBuilder html = new StringBuilder();
        for (Element element : elements) {
            html.append(element.html());
        }
        return Jsoup.parse(html.toString()).text();
    }
}
