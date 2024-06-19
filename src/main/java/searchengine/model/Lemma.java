package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Entity
@Table(indexes = {@Index(name = "lemma_index", columnList = "lemma, site_id", unique = true)})
@RequiredArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id")
    private SiteEntity siteId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;
}
