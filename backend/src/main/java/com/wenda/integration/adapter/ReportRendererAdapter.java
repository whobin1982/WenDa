package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

import java.util.Map;

/**
 * 报告渲染 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-012）。
 */
public interface ReportRendererAdapter {

    /** 渲染报告为字节流（PDF / Word / Excel）。 */
    byte[] render(RenderRequest req);

    enum Format { PDF, DOCX, XLSX }

    record RenderRequest(String reportType, String templateVersion, Format format,
                         Map<String, Object> data, Map<String, String> options) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "报告渲染 " + op + " 失败。");
    }
}
