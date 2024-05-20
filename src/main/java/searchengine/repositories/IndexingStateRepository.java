package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexingState;

import java.util.Optional;
@Repository
public interface IndexingStateRepository extends JpaRepository<IndexingState, Integer> {
IndexingState findByIndexing(String indexing);
}
