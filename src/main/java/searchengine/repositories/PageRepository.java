package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findIdByPath(String path);

  Integer countBySiteId(SiteEntity siteEntity);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM Page p WHERE p.siteId = :site_id" )
    void deletePage(@Param("site_id") SiteEntity siteId);

    Page findBySiteId(SiteEntity siteId);

    @Modifying
//    @Transactional
    @Query(value = "SELECT p FROM Page p WHERE p.siteId = :site_id")
    List<Page> findAllPageBySiteId(@Param("site_id") SiteEntity siteEntity);

//    @Transactional
//    void deleteBySiteId(SiteEntity siteId);

}
