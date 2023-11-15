package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;



@Slf4j
@RequiredArgsConstructor
@Service
public class LemmaServiceImpl implements LemmaService {
    private final NetworkService networkService;
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Override
    public void findAndSave(PageModel page) {

        String clearText = networkService.htmlToText(page.getContent());
        Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(clearText);

        Set<LemmaModel> lemmaSetToSave = new HashSet<>();
        Set<IndexModel> indices = new HashSet<>();
        synchronized (lemmaRepository) {
            lemmaMap.forEach((name, count) -> {
                Optional<LemmaModel> optionalLemma = lemmaRepository.findBySiteAndLemma(page.getSite(), name);
                LemmaModel lemma;
                if (optionalLemma.isPresent()) {
                    lemma = optionalLemma.get();
                } else {
                    lemma = LemmaModel.builder()
                            .frequency(1)
                            .lemma(name)
                            .site(page.getSite())
                            .build();
                    lemmaSetToSave.add(lemma);
                }

                indices.add(IndexModel.builder()
                        .page(page)
                        .lemma(lemma)
                        .rank((float) count)
                        .build());
            });
            lemmaRepository.saveAll(lemmaSetToSave);
        }
        indexRepository.saveAll(indices);
    }

    @Override
    public void updateLemmasFrequency(Integer siteId) {
        SiteModel siteModel = siteRepository.findById(siteId).orElseThrow(() -> new IllegalStateException("Site not found"));
        Set<LemmaModel> lemmaToSave = new HashSet<>();
        Set<LemmaModel> lemmaToDelete = new HashSet<>();
        log.info("Start lemmas frequency calculation for site: {}", siteModel);
        for (LemmaModel lemmaModel : lemmaRepository.findAllBySite(siteModel)) {
            int frequency = indexRepository.countByLemma(lemmaModel);
            if (frequency == 0) {
                lemmaToDelete.add(lemmaModel);
            } else if (lemmaModel.getFrequency() != frequency) {
                lemmaModel.setFrequency(frequency);
                lemmaToSave.add(lemmaModel);
            }
        }
        lemmaRepository.deleteAll(lemmaToDelete);
        log.info("Update lemmas: " + lemmaToSave.size());
        lemmaRepository.saveAll(lemmaToSave);
    }
}
