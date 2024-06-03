package searchengine.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Referrer;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
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
public class SiteMapBuilder  extends RecursiveAction {
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

        ConcurrentSkipListSet<String> links = linksExecutor(siteMap.getUrl());
        for (String link : links) {
            if (allLinks.contains(link)) {
                continue;
            }
            allLinks.add(link);
            siteMap.addChild(new SiteMap(link));

            Page page = new Page();
            page.setPath(pathFinder(link));
            page.setSiteId(siteRepository.findIdByUrl(urlRootFinder(link)));
            try {
                page.setCode(responseCodeGetter(link));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                page.setContent(htmlGetter(link));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            pageRepository.save(page);
            if(page.getCode() < 400) {
                LemmaWriter lemmaWriter = new LemmaWriter(siteRepository, pageRepository, sitesList, lemmaRepository, indexRepository, link);
                lemmaWriter.start();
                log.info("Запущен поток " + lemmaWriter.getName());
                try {
                    lemmaWriter.join();
                    log.info("состояние потока " + lemmaWriter.getState());
                    log.info("окончен поток " + lemmaWriter.getName());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("Леммы на странице " + page.getId() + " записаны " + link);
            }

            SiteEntity siteEntity1 = siteRepository.findIdByName(site.getName());
            siteEntity1.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity1);
            log.info("добавлена страница " + page.getId());
        }
        List<SiteMapBuilder> taskList = new ArrayList<>();

            for (SiteMap child : siteMap.getChildLinks()) {
                SiteMapBuilder task = new SiteMapBuilder(child, siteRepository, pageRepository, site, indexingStateRepository, sitesList, lemmaRepository, indexRepository);
                task.fork();
                taskList.add(task);
            }
            for (SiteMapBuilder task : taskList) {
                task.join();
            }

    }
    public static ConcurrentSkipListSet<String> linksExecutor (String url){
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();

        String regex = urlRootFinder(url) + "/[^#,\\s]*";
        try {
            Thread.sleep(500);
            Document document = Jsoup.connect(url)
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
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
    private static String urlRootFinder(String url){
        String[] partsUrl = url.split("/");
        return partsUrl[0] + "//" + partsUrl[2];
    }
    private static String pathFinder(String url){
        String[] partsUrl = url.split("/");

        int start = partsUrl[0].length() + partsUrl[2].length() + 3;
        return "/" + url.substring(start);
    }
    private int responseCodeGetter(String url) throws IOException {
        URL url1 = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)url1.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }

    private String htmlGetter(String url) throws IOException{
        Document document = Jsoup.connect(url).get();
        return document.html();
    }

    public CopyOnWriteArrayList<String> allLinksGetter(){
        return allLinks;
    }
    public void allLinksCleaner(){
        allLinks.clear();
    }

}
