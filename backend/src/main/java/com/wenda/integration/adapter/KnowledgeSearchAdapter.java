package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 知识库检索 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-009）。
 */
public interface KnowledgeSearchAdapter {

    SearchResult search(SearchRequest req);

    record SearchRequest(String keyword, List<String> filters, int topK, String schoolId) {}

    record SearchResult(List<Hit> hits) {
        public record Hit(String chunkId, String documentId, String content, double score,
                          Map<String, Object> metadata) {}
    }

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "知识库检索 " + op + " 失败。");
    }
}
