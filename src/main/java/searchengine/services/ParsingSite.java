package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class ParsingSite extends RecursiveTask<String> {

    private String url;
    private final static CopyOnWriteArrayList<String> urlList = new CopyOnWriteArrayList<>();
    private final SiteModel siteModel;
    private final PageRepository pageRepository;


    public ParsingSite(String url, SiteModel siteModel, PageRepository pageRepository) {
        this.url = url;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
    }

    @Override
    protected String compute() {

        String tab = StringUtils.repeat("\t",
                url.lastIndexOf("/") != url.length() - 1 ? StringUtils.countMatches(url, "/") - 2 : StringUtils.countMatches(url, "/") - 3);
        StringBuffer stringBuffer = new StringBuffer(tab + url + "\n");
        List<ParsingSite> taskList = new CopyOnWriteArrayList<>();

        try {
            log.info("log test");
            System.out.println("Connection: " + url);
            Thread.sleep(150);
            Document document = Jsoup.connect(url).ignoreContentType(true).get();

            PageModel pageModel = getContentPage(url, document);

            Elements elements = document.select("a[href]");
            for (Element element : elements) {
                String attrUrl = element.absUrl("href");
                if (!attrUrl.isEmpty() && attrUrl.startsWith(url) && !urlList.contains(attrUrl) && !attrUrl.contains("#") && attrUrl.endsWith("/") ) {
                    urlList.add(attrUrl);
                    ParsingSite task = new ParsingSite(attrUrl, siteModel, pageRepository);
                    taskList.add(task);
                    task.fork();
                }
            }
            for (ParsingSite task : taskList) {
                stringBuffer.append(task.join());
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return stringBuffer.toString();
    }

    public PageModel getContentPage(String url, Document document) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteModel(siteModel);
        pageModel.setPath(url.substring(siteModel.getUrl().length()));
        pageModel.setCodeResponse(200);
        pageModel.setContent(document.outerHtml());

        pageRepository.saveAndFlush(pageModel);

        return pageModel;
    }


}
