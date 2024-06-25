package com.example.ddmdemo.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.example.ddmdemo.dto.BooleanDTO;
import com.example.ddmdemo.exceptionhandling.exception.MalformedQueryException;
import com.example.ddmdemo.indexmodel.DummyIndex;
import com.example.ddmdemo.service.interfaces.SearchService;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchTemplate;

    @Override
    public Page<DummyIndex> simple(List<String> keywords, Boolean isPhaseQuery, Pageable pageable) {
        var searchQueryBuilder =
            new NativeQueryBuilder().withQuery(buildSimpleSearchQuery(keywords, isPhaseQuery))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }

    @Override
    public Page<DummyIndex> advancedSearch(List<String> expression, Pageable pageable) {
        if (expression.size() != 3) {
            throw new MalformedQueryException("Search query malformed.");
        }

        String operation = expression.get(1);
        expression.remove(1);
        var searchQueryBuilder =
            new NativeQueryBuilder().withQuery(buildAdvancedSearchQuery(expression, operation))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
       
    @Override
    public Page<DummyIndex> simpleSearch(String field, String text, Pageable pageable) {
    	var searchQueryBuilder =
                new NativeQueryBuilder().withQuery(buildSearchSimpleQuery(field, text))
                    .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
    
    public Page<DummyIndex> booleanSearch(BooleanDTO booleanDTO, Pageable pageable) {
        var searchQueryBuilder =
            new NativeQueryBuilder().withQuery(buildBooleanSearchQuery(booleanDTO))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }
    
    private Query buildSimpleSearchQuery(List<String> tokens, Boolean isPhaseQuery) {
    	if (isPhaseQuery) {
    		String joinedTokens = String.join(" ", tokens);
    		return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> b.must(sb -> sb.matchPhrase(
                    m -> m.field("content_sr").query(joinedTokens)))
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

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<DummyIndex>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }
}
