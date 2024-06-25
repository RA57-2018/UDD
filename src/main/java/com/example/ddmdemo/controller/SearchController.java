package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.BooleanDTO;
import com.example.ddmdemo.dto.SearchQueryDTO;
import com.example.ddmdemo.indexmodel.DummyIndex;
import com.example.ddmdemo.service.interfaces.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/simple")
    public Page<DummyIndex> simpleSearch(@RequestBody SearchQueryDTO simpleSearchQuery,
                                         Pageable pageable) {
        return searchService.simple(simpleSearchQuery.keywords(), simpleSearchQuery.isPhaseQuery(), pageable);
    }

    @PostMapping("/advanced")
    public Page<DummyIndex> advancedSearch(@RequestBody SearchQueryDTO advancedSearchQuery,
                                           Pageable pageable) {
        return searchService.advancedSearch(advancedSearchQuery.keywords(), pageable);
    }
    
    @GetMapping("/simpleSearch")
    public Page<DummyIndex> search(@RequestParam String field, @RequestParam String text, Pageable pageable) {
    	return searchService.simpleSearch(field, text, pageable);
    }
    
    @PostMapping("/boolean")
    public Page<DummyIndex> search(@RequestBody BooleanDTO booleanDTO, Pageable pageable) {
    	return searchService.booleanSearch(booleanDTO, pageable);
    }
}
