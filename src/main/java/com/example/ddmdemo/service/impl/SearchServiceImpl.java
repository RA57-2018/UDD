package com.example.ddmdemo.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.example.ddmdemo.dto.BooleanDTO;
import com.example.ddmdemo.exceptionhandling.exception.MalformedQueryException;
import com.example.ddmdemo.indexmodel.DummyIndex;
import com.example.ddmdemo.service.interfaces.SearchService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters.HighlightFieldParametersBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchTemplate;

    @Override
    public Page<DummyIndex> simple(List<String> keywords, Boolean isPhaseQuery, Pageable pageable) {
        HighlightFieldParametersBuilder highlightFieldParamBuilder = HighlightFieldParameters.builder()
            .withPreTags("<strong>").withPostTags("</strong>").withFragmentSize(150).withType("unified");
        
        HighlightFieldParameters highlightFieldParameters = highlightFieldParamBuilder.build();  

        List<HighlightField> highlightFields = new ArrayList<>();

        for (String field : Arrays.asList("title", "contentSr", "contentEn", "firstName", "lastName", "governmentName", "administrationLevel", "address")) {
            highlightFields.add(new HighlightField(field, highlightFieldParameters));
        }

        var searchQueryBuilder = new NativeQueryBuilder()
            .withQuery(buildSimpleSearchQuery(keywords, isPhaseQuery))
            .withHighlightQuery(new HighlightQuery(new Highlight(highlightFields), DummyIndex.class))
            .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }

    @Override
    public Page<DummyIndex> advancedSearch(List<String> expression, Pageable pageable) {
        if (expression.size() != 3) {
            throw new MalformedQueryException("Search query malformed.");
        }
        
        HighlightFieldParametersBuilder highlightFieldParamBuilder = HighlightFieldParameters.builder()
                .withPreTags("<strong>").withPostTags("</strong>").withFragmentSize(250).withType("unified");
        HighlightFieldParameters highlightFieldParameters = highlightFieldParamBuilder.build();

        List<HighlightField> highlightFields = new ArrayList<>();

        String field1 = expression.get(0).split(":")[0];
        String field2 = expression.get(2).split(":")[0];       

        highlightFields.add(new HighlightField(field1, highlightFieldParameters));
        highlightFields.add(new HighlightField(field2, highlightFieldParameters));

        String operation = expression.get(1);
        expression.remove(1);
        var searchQueryBuilder =
            new NativeQueryBuilder().withQuery(buildAdvancedSearchQuery(expression, operation))
            	.withHighlightQuery(new HighlightQuery(new Highlight(highlightFields), DummyIndex.class))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
       
    @Override
    public Page<DummyIndex> simpleSearch(String field, String text, Pageable pageable) {
    	HighlightFieldParametersBuilder highlightFieldParamBuilder = HighlightFieldParameters.builder()
    			.withPreTags("<strong>").withPostTags("</strong>").withFragmentSize(150).withType("unified");
    	HighlightFieldParameters highlightFieldParameters = highlightFieldParamBuilder.build();

        List<HighlightField> highlightFields = new ArrayList<>();
        highlightFields.add(new HighlightField(field, highlightFieldParameters));
        
    	var searchQueryBuilder =
                new NativeQueryBuilder().withQuery(buildSearchSimpleQuery(field, text))
                .withHighlightQuery(new HighlightQuery(new Highlight(highlightFields), DummyIndex.class))
            	.withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
    
    public Page<DummyIndex> booleanSearch(BooleanDTO booleanDTO, Pageable pageable) {
    	HighlightFieldParametersBuilder highlightFieldParamBuilder = HighlightFieldParameters.builder()
    			.withPreTags("<strong>").withPostTags("</strong>").withFragmentSize(150).withType("unified");
    	HighlightFieldParameters highlightFieldParameters = highlightFieldParamBuilder.build();

    	List<HighlightField> highlightFields = new ArrayList<>();

        if (booleanDTO.getField1() != null && !booleanDTO.getField1().isEmpty()) {
            highlightFields.add(new HighlightField(booleanDTO.getField1(), highlightFieldParameters));
        }

        if (booleanDTO.getField2() != null && !booleanDTO.getField2().isEmpty()) {
            highlightFields.add(new HighlightField(booleanDTO.getField2(), highlightFieldParameters));
        }
        
        var searchQueryBuilder =
            new NativeQueryBuilder().withQuery(buildBooleanSearchQuery(booleanDTO))
            	.withHighlightQuery(new HighlightQuery(new Highlight(highlightFields), DummyIndex.class))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
    
    private Query buildSimpleSearchQuery(List<String> tokens, Boolean isPhaseQuery) {
    	    if (isPhaseQuery) {
    	        String joinedTokens = String.join(" ", tokens);
    	        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> b.should(sb -> sb.matchPhrase(
    	                    m -> m.field("content_sr").query(joinedTokens)))
    	                .should(sb -> sb.matchPhrase(
    	                    m -> m.field("content_en").query(joinedTokens)))
    	        )))._toQuery();
    	    }
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.match(
                    m -> m.field("title").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(m -> m.field("content_sr").query(token)));
                b.should(sb -> sb.match(m -> m.field("content_en").query(token)));
            });
            return b;
        })))._toQuery();
    }

    private Query buildAdvancedSearchQuery(List<String> operands, String operation) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            var field1 = operands.get(0).split(":")[0];
            var value1 = operands.get(0).split(":")[1];
            var field2 = operands.get(1).split(":")[0];
            var value2 = operands.get(1).split(":")[1];

            switch (operation) {
                case "AND":
                    b.must(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.must(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
                case "OR":
                    b.should(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.should(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
                case "NOT":
                    b.must(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.mustNot(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
            }

            return b;
        })))._toQuery();
    }
    
    private Query buildSearchSimpleQuery(String field, String text) {
    	if (isPhrase(text)) {
    		return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> b.must(sb -> sb.matchPhrase(
                    m -> m.field(field).query(text)))
            )))._toQuery();
        } else {
        	return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> b.must(sb -> sb.match(
                    m -> m.field(field).fuzziness(Fuzziness.ONE.asString()).query(text)))
            )))._toQuery();
        }    	
    }
    
    
    private Query buildBooleanSearchQuery(BooleanDTO booleanDTO) {
    	String operation = booleanDTO.getOperation();
    	var field1 = booleanDTO.getField1();
        var value1 = booleanDTO.getValue1();
        var field2 = booleanDTO.getField2();
        var value2 = booleanDTO.getValue2();
        
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            switch (operation) {
                case "AND":
                    b.must(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.must(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
                case "OR":
                    b.should(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.should(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
                case "NOT":
                    b.must(sb -> sb.match(
                        m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1)));
                    b.mustNot(sb -> sb.match(m -> m.field(field2).query(value2)));
                    break;
            }

            return b;
        })))._toQuery();
    }
    
    private boolean isPhrase(String text) {
        return text.contains(" ");
    }

    private Page<DummyIndex> runQuery(NativeQuery searchQuery) {

        var searchHits = elasticsearchTemplate.search(searchQuery, DummyIndex.class,
            IndexCoordinates.of("dummy_index"));
        
        List<String> highlightFieldNames = searchQuery.getHighlightQuery()
                .map(HighlightQuery::getHighlight)
                .map(Highlight::getFields)
                .orElse(List.of())
                .stream()
                .map(HighlightField::getName)
                .collect(Collectors.toList());

        List<DummyIndex> results = searchHits.getSearchHits().stream().map(hit -> {
            DummyIndex dummyIndex = hit.getContent();
            String highlight = highlightFieldNames.stream()
                    .flatMap(name -> {
                        List<String> fragments = Optional.ofNullable(hit.getHighlightFields().get(name)).orElse(Collections.emptyList());
                        return fragments.stream();
                    })
                    .collect(Collectors.joining(" "));
            dummyIndex.setHighlight(highlight);
            return dummyIndex;
        }).collect(Collectors.toList());

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return new PageImpl<>(results, searchQuery.getPageable(), searchHits.getTotalHits());
    }
}
