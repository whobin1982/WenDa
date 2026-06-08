package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

/**
 * 文件存储 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-006）。
 */
public interface FileStorageAdapter {

    /** 初始化分片上传会话，返回 uploadId。 */
    String initUpload(InitUploadRequest req);

    /** 上传分片。 */
    void uploadPart(UploadPartRequest req);

    /** 完成上传，返回对象 key。 */
    String completeUpload(CompleteUploadRequest req);

    /** 取消上传。 */
    void abortUpload(String uploadId);

    /** 获取对象元数据。 */
    ObjectMetadata getMetadata(String key);

    /** 生成临时签名下载 URL。 */
    String generateDownloadUrl(String key, Duration ttl);

    /** 生成临时签名预览 URL。 */
    String generatePreviewUrl(String key, Duration ttl);

    /** 删除对象（仅非关键业务；关键文件走归档）。 */
    void delete(String key);

    record InitUploadRequest(String bucket, String key, long totalSize, String contentType,
                             Map<String, String> userMetadata) {}

    record UploadPartRequest(String uploadId, int partNumber, InputStream data, long size) {}

    record CompleteUploadRequest(String uploadId, String key, String contentType) {}

    record ObjectMetadata(String key, long size, String contentType, String etag) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "对象存储 " + op + " 失败。");
    }
}
