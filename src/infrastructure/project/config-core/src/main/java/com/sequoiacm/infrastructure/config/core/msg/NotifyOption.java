package com.sequoiacm.infrastructure.config.core.msg;

public interface NotifyOption {
    String getBusinessName();

    // 通知客户端心跳模块的版本缓存，若本次通知不引起版本变化，则返回null
    Version getBusinessVersion();
}
