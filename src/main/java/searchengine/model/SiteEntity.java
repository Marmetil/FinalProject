package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Setter
@Getter
@Table(name = "Sites")
@RequiredArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(length = 45, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(columnDefinition = "VARCHAR(255)", name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(columnDefinition = "TEXT", name = "last_error", nullable = true)
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

}
