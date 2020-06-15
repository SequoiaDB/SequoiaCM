package com.sequoiadb.infrastructure.map.client.core;

import com.sequoiadb.infrastructure.map.client.model.ScmMapGroup;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;

public class ScmMapFactory {
    public static ScmMapGroup getGroupMap(MapFeignClient client, String groupName) {
        return new ScmMapGroupImpl(client, groupName);
    }
}
