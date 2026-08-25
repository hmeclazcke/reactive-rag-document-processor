package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

record WordCountAggregationResult(
        String word,
        long count
) {
}