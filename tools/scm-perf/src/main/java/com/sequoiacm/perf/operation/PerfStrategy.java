package com.sequoiacm.perf.operation;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;

public class PerfStrategy {


    public static void run(Config config) throws ScmException, InterruptedException {

        switch (config.getApiType()) {
            case REST:
                RestOperationV2 restOperation = new RestOperationV2(config);
                restOperation.run();
                break;
            case DRIVER:
                DriverOperationV2 driverOperation = new DriverOperationV2(config);
                driverOperation.run();
                break;
            default:
                break;
        }
    }
}
