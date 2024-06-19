package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByLemma(String lemma);
    Lemma findByLemmaAndSiteId(String lemma, SiteEntity siteEntity);
    Integer countBySiteId(SiteEntity siteEntity);

    @Modifying
    @Query("SELECT  l FROM Lemma l WHERE l.id IN " +
            "(SELECT l.id FROM Lemma l INNER JOIN Index i ON l.id = i.lemmaId.id " +
            " INNER JOIN Page p ON p.id = i.pageId.id WHERE p.siteId.id = l.siteId.id AND i.pageId.id = :page_id)")
    List<Lemma> lemmaToUpdate(@Param("page_id") Integer page_id);

    @Modifying
    @Transactional
    @Query("DELETE  FROM Lemma l WHERE l.frequency = 0")
   void deleteLemma();

    @Modifying
    @Transactional
    @Query("DELETE  FROM Lemma l WHERE l.siteId = :site_id")
    void deleteLemmaBySiteId(@Param("site_id") SiteEntity siteEntity);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "INSERT INTO lemma (site_id, lemma, frequency)" +
            "VALUES (:site_id, :lemma, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ")
    void fillInLemma(@Param("site_id") Integer site_id, @Param("lemma") String lemma);
}
