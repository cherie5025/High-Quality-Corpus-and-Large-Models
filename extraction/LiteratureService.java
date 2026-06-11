package shu.xai.literature_backened;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional  // 类级别启用事务
public class LiteratureService {
    
    @Autowired
    private LiteratureRepository repository;
    
    @Transactional(readOnly = true)  // 查询事务
    public List<Literature> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return repository.findAll();
        }
        return repository.searchByKeyword(keyword);
    }
    
    @Transactional(readOnly = true)
    public Optional<Literature> findById(String id) {
        return repository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Literature> findAll() {
        return repository.findAll();
    }
}