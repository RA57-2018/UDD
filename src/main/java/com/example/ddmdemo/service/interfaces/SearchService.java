package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.BooleanDTO;
import com.example.ddmdemo.indexmodel.DummyIndex;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {

    Page<DummyIndex> simple(List<String> keywords, Boolean isPhaseQuery, Pageable pageable);

    Page<DummyIndex> advancedSearch(List<String> expression, Pageable pageable);
    
    Page<DummyIndex> simpleSearch(String field, String text, Pageable pageable);   
    
    Page<DummyIndex> booleanSearch(BooleanDTO booleanDTO, Pageable pageable);
}
