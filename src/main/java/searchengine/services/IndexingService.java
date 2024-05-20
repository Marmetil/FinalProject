package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.SiteMapBuilder;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
//@NoArgsConstructor
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexingStateRepository indexingStateRepository;
    private final SitesList siteList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();


    public void startIndexing(){
        List<SiteEntity> siteEntities = new ArrayList<>();
        if(indexingStateRepository.findById(1).isEmpty()){
            IndexingState newState = new IndexingState();
            newState.setIndexing("YES");
            indexingStateRepository.save(newState);
        }
        for (Site site : siteList.getSites()){
            SiteEntity siteEntity = siteRepository.findIdByName(site.getName());
            if(!(siteEntity == null)) {
                List<Index> indexesToDelete = indexRepository.findIndexBySiteId(siteEntity);
                for (Index index : indexesToDelete){
                    indexRepository.delete(index);
                }
                lemmaRepository.deleteLemmaBySiteId(siteEntity);
                pageRepository.deletePage(siteEntity);
                siteRepository.delete(siteEntity);
            }
            SiteEntity newSiteEntity = new SiteEntity();
            newSiteEntity.setStatusTime(LocalDateTime.now());
                newSiteEntity.setLastError(null);
                newSiteEntity.setUrl(site.getUrl());
                newSiteEntity.setName(site.getName());
                newSiteEntity.setStatus(Status.INDEXING);
                siteRepository.save(newSiteEntity);
                siteEntities.add(newSiteEntity);
        }
        for (SiteEntity site : siteEntities){
            SiteMap siteMap = new SiteMap(site.getUrl());
            Site newSite = makeSite(site);
            SiteMapBuilder task = new SiteMapBuilder(siteMap, siteRepository, pageRepository, newSite, indexingStateRepository, siteList, lemmaRepository, indexRepository);
                   ForkJoinPool forkJoinPool = new ForkJoinPool();
                   forkJoinPool.invoke(task);
                   forkJoinPoolList.add(forkJoinPool);

                   SiteEntity siteEntity = siteRepository.findIdByName(site.getName());
                   if(siteEntity.getLastError() == null){
                       siteEntity.setStatus(Status.INDEXED);
                   } else {
                       siteEntity.setStatus(Status.FAILED);
                   }
                   siteRepository.save(siteEntity);
                   log.info("Индексация окончена " + site.getName());
                   task.allLinksCleaner();
        }
    }
    public void stopIndexing(){
  indexingStateRepository.deleteAll();
        for (ForkJoinPool forkJoinPool : forkJoinPoolList){
            forkJoinPool.shutdownNow();
        }
        forkJoinPoolList.clear();
        for (Site site : siteList.getSites()) {
            SiteEntity siteEntity = siteRepository.findIdByName(site.getName());

            if(siteEntity.getStatus().equals(Status.INDEXING)){
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Индексация остановлена пользователем");
                siteRepository.save(siteEntity);
            }
        }

    }

    public void IndexPage(String url) throws IOException {
        LemmaCounter lemmaCounter = new LemmaCounter(siteRepository, pageRepository, siteList, lemmaRepository,indexRepository);
        lemmaCounter.IndexPage(url);
    }

    public String urlRootFinder(String url){
        String[] partsUrl = url.split("/");
        return partsUrl[0] + "//" + partsUrl[2];
    }

    private Site makeSite(SiteEntity siteEntity){
        Site site = new Site();
        site.setName(siteEntity.getName());
        site.setUrl(siteEntity.getUrl());
        return site;
    }

}
