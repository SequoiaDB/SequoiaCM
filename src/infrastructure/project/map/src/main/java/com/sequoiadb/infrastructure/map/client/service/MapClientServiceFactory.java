package com.sequoiadb.infrastructure.map.client.service;

public class MapClientServiceFactory {
    private static MapClientServiceFactory factory = new MapClientServiceFactory();

    public static MapClientServiceFactory getInstance() {
        return factory;
    }

    public IMapClientService createClientService(MapFeignClient client, String groupName) {
        return new MapClientServiceImpel(client, groupName);
    }
}
