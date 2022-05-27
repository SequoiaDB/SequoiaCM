package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.authorization")
public class AuthorizationConfig {

    private int maxTimeOffset = -1;
    // S3文档中对签名算法描述是将 signedHeaders 和 headers 排序后进行计算。
    // 实际验证：按照顺序计算好签名后，打乱请求中的 signedHeaders 顺序，然后发送到 amz 的服务器，
    // 服务器判定签名校验失败，可见 amz 的服务器没有排序再计算
    // 为兼容S3文档和amz S3实际实现的差异，通过 sortHeaders 参数来控制 SCM S3 是否对 signedHeaders 和 headers进行排序
    // SCM S3 默认配置为 false, 与 amz 实际实现保持一致，不进行排序
    private boolean sortHeaders = false;

    public int getMaxTimeOffset() {
        return maxTimeOffset;
    }

    public void setMaxTimeOffset(int maxTimeOffset) {
        this.maxTimeOffset = maxTimeOffset;
    }

    public boolean isSortHeaders() {
        return sortHeaders;
    }

    public void setSortHeaders(boolean sortHeaders) {
        this.sortHeaders = sortHeaders;
    }
}
