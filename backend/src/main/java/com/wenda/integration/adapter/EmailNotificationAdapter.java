package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

/**
 * 邮件通知 Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 NFR-009）。
 */
public interface EmailNotificationAdapter {

    void send(EmailMessage msg);

    record EmailMessage(String to, String subject, String bodyHtml, String bodyText) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "邮件发送 " + op + " 失败。");
    }
}
