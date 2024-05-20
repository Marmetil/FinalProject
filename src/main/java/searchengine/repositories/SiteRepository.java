package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findIdByUrl(String url);
    SiteEntity findIdByName(String name);
    @Modifying
    @Query("update SiteEntity s set s.status = ?1 where s.name = ?2")
    String setStatusFor(Status status, String siteName);
}
