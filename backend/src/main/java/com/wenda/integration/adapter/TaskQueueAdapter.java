package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

import java.util.Map;

/**
 * 异步任务队列 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-007）。
 */
public interface TaskQueueAdapter {

    String enqueue(TaskDefinition def);

    TaskStatus getStatus(String taskId);

    void cancel(String taskId);

    enum TaskType { AI_RUN, FILE_PARSE, REPORT_RENDER, IMPORT, ABILITY_MAP, NOTIFY }

    enum TaskState { PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED, TIMEOUT }

    record TaskDefinition(TaskType type, String businessKey, Map<String, Object> payload,
                          int maxRetries) {}

    record TaskStatus(String taskId, TaskState state, int progress, String errorCode,
                      String errorMessage, Map<String, Object> result) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "任务队列 " + op + " 失败。");
    }
}
