package shu.xai.literature_backened;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository  // 确保有这个注解
public interface LiteratureRepository extends JpaRepository<Literature, String> {
    
    List<Literature> findByTitleContainingIgnoreCase(String keyword);
    
    @Query("SELECT l FROM Literature l WHERE " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.authors) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Literature> searchByKeyword(@Param("keyword") String keyword);
}