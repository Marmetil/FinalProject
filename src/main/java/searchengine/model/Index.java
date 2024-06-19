package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "index_page", indexes = {@jakarta.persistence.Index(name = "lemma_index", columnList = "page_id, lemma_id", unique = true)})
@RequiredArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id")
    private Page pageId;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id")
    private Lemma lemmaId;
    @Column(name = "rating", columnDefinition = "INT", nullable = false)
    private int rank;
}
