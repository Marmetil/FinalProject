package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "Pages", indexes = {@Index(name = "path_index", columnList = "site_id, path", unique = true)})
@RequiredArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id")
    private SiteEntity siteId;
    @Column(columnDefinition = "VARCHAR(255)")
    private String path;
    @Column(columnDefinition = "INT", nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

}
