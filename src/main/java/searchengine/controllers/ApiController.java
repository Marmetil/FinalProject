package searchengine.controllers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.Message;
import searchengine.dto.seach.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.IndexingState;
import searchengine.repositories.IndexingStateRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final IndexingStateRepository indexingStateRepository;
    private final SitesList siteList;
    private final SearchingService searchingService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public Message startIndexing() {
        if(indexingStateRepository.findByIndexing("YES") == null)  {
            CompletableFuture<Void> asyncIndexing = CompletableFuture.runAsync(indexingService::startIndexing);
            return new Message(true);
        } else {
            return new Message(false, "Индексация уже запущена");
        }
    }
    @GetMapping("/stopIndexing")

    public Message stopIndexing(){
        if(indexingStateRepository.findByIndexing("YES") == null){
            return new Message(false, "Индексация не запущена");
        } else {
            indexingService.stopIndexing();
        }
        return new Message(true);
    }
    @PostMapping("/indexPage")
    public Message indexPage(String url) throws IOException {
        ArrayList<String> siteUrls = new ArrayList<>();
        for (Site site : siteList.getSites()) {
            siteUrls.add(site.getUrl());
        }
        if(!siteUrls.contains(indexingService.urlRootFinder(url))){
            return new Message(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        } else {
            indexingService.IndexPage(url);
        } return new Message(true);

    }
@GetMapping("/search")
    public ResponseEntity<SearchingResponse> Searching(@RequestParam String query, @RequestParam(required = false) Site site,
                                                       @RequestParam(required = false, defaultValue = "0") int offset,
                                                       @RequestParam(required = false, defaultValue = "20") int limit) throws IOException {
        if(site == null){
            return ResponseEntity.ok(searchingService.getSearchingResponse(query, siteList.getSites(), 2, 3));
            }

        else {
            List<Site> newSiteList = new ArrayList<>();
            newSiteList.add(site);
            return ResponseEntity.ok(searchingService.getSearchingResponse(query, newSiteList, 2, 3));
        }

}
}


