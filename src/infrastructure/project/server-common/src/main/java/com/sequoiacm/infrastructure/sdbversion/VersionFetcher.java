package com.sequoiacm.infrastructure.sdbversion;

public interface VersionFetcher {
    Version fetchVersion() throws Exception;
}
