package searchengine.services;

import searchengine.model.PageModel;

public interface LemmaService {
    void findAndSave(PageModel page);
    void updateLemmasFrequency(Integer siteId);
}
