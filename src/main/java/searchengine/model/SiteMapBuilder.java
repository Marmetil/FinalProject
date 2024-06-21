package searchengine.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteMapBuilder extends RecursiveAction {
    static CopyOnWriteArrayList<String> allLinks = new CopyOnWriteArrayList<>();
    private final SiteMap siteMap;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Site site;
    private final IndexingStateRepository indexingStateRepository;
    private final SitesList sitesList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    protected void compute() {
        if(indexingStateRepository.findByIndexing("YES") == null) {
            return;
        }
        ConcurrentSkipListSet<String> links = null;
        try {
            links = linksExecutor(siteMap.getUrl());
        } catch (IOException | InterruptedException e) {
            log.info("Возникла ошибка получения ссылок");
        }
        for (String link : links) {
            if (allLinks.contains(link)) {
                continue;
            }
            allLinks.add(link);
            siteMap.addChild(new SiteMap(link));
            Page page = new Page();
            savePage(page, link);
            if(page.getCode() < 400) {
               startLemmaWriter(link);
            }
            SiteEntity siteEntity1 = siteRepository.findIdByName(site.getName());
            siteEntity1.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity1);
        }
        List<SiteMapBuilder> taskList = new ArrayList<>();
        for (SiteMap child : siteMap.getChildLinks()) {
            SiteMapBuilder task = new SiteMapBuilder(child, siteRepository, pageRepository, site,
                    indexingStateRepository, sitesList, lemmaRepository, indexRepository);
            task.fork();
            taskList.add(task);
        }
        for (SiteMapBuilder task : taskList) {
            task.join();
        }
    }

    public ConcurrentSkipListSet<String> linksExecutor(String url) throws IOException, InterruptedException {
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
        String regex = urlRootFinder(url) + "/[^#,\\s]*";
        Thread.sleep(500);
        Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(false).get();
        Elements elements = document.select("a");
        for (Element element : elements) {
            String link = element.absUrl("href");
            if (link.matches(regex) && (!isFile(link))) {
                links.add(link);
                }
        }
        return links;
    }

    private static boolean isFile(String link) {
        link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".JPG")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                || link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains("?_ga");
    }

    private static String urlRootFinder(String url) {
        String[] partsUrl = url.split("/");
        return partsUrl[0] + "//" + partsUrl[2];
    }

    private static String pathFinder(String url) {
        String[] partsUrl = url.split("/");

        int start = partsUrl[0].length() + partsUrl[2].length() + 3;
        return "/" + url.substring(start);
    }

    private int responseCodeGetter(String url) throws IOException {
        URL url1 = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }

    private String htmlGetter(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        return document.html();
    }

    public CopyOnWriteArrayList<String> allLinksGetter() {
        return allLinks;
    }

    public void allLinksCleaner() {
        allLinks.clear();
    }
    private void savePage(Page page, String link){
        page.setPath(pathFinder(link));
        page.setSiteId(siteRepository.findIdByUrl(urlRootFinder(link)));
        try {
            page.setCode(responseCodeGetter(link));
        } catch (IOException e) {
            page.setCode(HttpStatus.BAD_REQUEST.value());
        }
        try {
            page.setContent(htmlGetter(link));
        } catch (IOException e) {
            page.setContent("Ошибка  получения контента");
        }
        pageRepository.save(page);
    }

    private void startLemmaWriter(String link){
        LemmaWriter lemmaWriter = new LemmaWriter(siteRepository, pageRepository,
                sitesList, lemmaRepository, indexRepository, link);
        lemmaWriter.start();
        try {
            lemmaWriter.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
