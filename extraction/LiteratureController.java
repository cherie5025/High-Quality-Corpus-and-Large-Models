package shu.xai.literature_backened;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController  // 必须有这个注解
@RequestMapping("/api/literature")
public class LiteratureController {
    
    @Autowired
    private LiteratureService service;

    @GetMapping("/test")
    public String test() {
        return "OK";
    }
    
    @GetMapping("/all")
    public List<Literature> getAll() {
        return service.findAll();
    }
    
    @GetMapping("/search")
    public List<Literature> search(@RequestParam(required = false) String keyword) {
        return service.search(keyword);
    }
    
    @GetMapping("/{id}")
    public Literature getById(@PathVariable String id) {
        return service.findById(id).orElse(null);
    }
}