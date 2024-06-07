package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM Index i WHERE i.pageId = :page_id" )
    void deleteIndex(@Param("page_id") Page page);

    @Modifying
    @Query("SELECT  i FROM Index i WHERE i.id IN " +
            "(SELECT i.id FROM Index i INNER JOIN Page p ON p.id = i.pageId.id " +
            "INNER JOIN SiteEntity s ON s.id = p.siteId.id WHERE p.siteId.id = :site_id)")
    List<Index> findIndexBySiteId(@Param("site_id") Integer  siteId);

    @Modifying
    @Query("SELECT i FROM Index i WHERE i.lemmaId.id = :lemma_id")
    List<Index> findByLemma(@Param("lemma_id") Lemma lemma);

//    @Modifying
//    @Query("SELECT i FROM Index i WHERE i.pageId = :page_id AND i.lemmaId = :lemma_id")
//    Index findByLemmaAndPage(@Param("lemma_id") Lemma lemma, @Param("page_id") Page page);
    Index findByLemmaIdAndPageId(Lemma lemmaId, Page pageId);
    @Modifying
    @Query("SELECT  i FROM Index i WHERE i.id IN " +
            "(SELECT i.id FROM Index i INNER JOIN Page p ON p.id = i.pageId.id " +
            " INNER JOIN SiteEntity s ON s.id = p.siteId.id WHERE i.lemmaId.id = :lemma_id AND p.siteId.id = :site_id)")
    List<Index> findIndexByLemmaAndSiteId(@Param("site_id") Integer  siteId, @Param("lemma_id") Integer lemmaId);

}
