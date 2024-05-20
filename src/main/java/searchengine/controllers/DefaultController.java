package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }

//    @PostMapping
//    public void startIndexing(){
//        String url = "https://laserbeauty.kz";
//        SiteMap siteMap = new SiteMap(url);
//        IndexingService task = new IndexingService(siteMap);
//        new ForkJoinPool().invoke(task);
//
//    }
}
