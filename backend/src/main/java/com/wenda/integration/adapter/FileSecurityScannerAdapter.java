package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

/**
 * 文件安全扫描 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-011）。
 */
public interface FileSecurityScannerAdapter {

    /** 触发扫描；返回扫描任务 ID。 */
    String scan(ScanRequest req);

    /** 查询扫描结果。 */
    ScanResult getResult(String scanId);

    enum Verdict { CLEAN, INFECTED, SUSPICIOUS, FAILED, TIMEOUT }

    record ScanRequest(String fileId, String storageKey, long sizeBytes, String contentType,
                       String checksum) {}

    record ScanResult(String scanId, String fileId, Verdict verdict, String engineVersion,
                      String report) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "文件扫描 " + op + " 失败。");
    }
}
