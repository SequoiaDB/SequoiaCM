package com.sequoiacm.schedule.client;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.sequoiacm.schedule.client.config.ScheduleWorkerConfig;
import com.sequoiacm.schedule.client.controller.ScheduleWorkerController;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerMgr;

@EnableScheduleClient
public class ScheduleWorkerAutoConfig {

    @Bean
    public ScheduleWorkerMgr workerMgr(@Value("${server.port}") int serverPort,
            ScheduleWorkerConfig config) throws UnknownHostException {
        return new ScheduleWorkerMgr(serverPort, config);
    }

    @Bean
    public ScheduleWorkerConfig scheClientConfig() {
        return new ScheduleWorkerConfig();
    }

    @Bean
    public ScheduleWorkerController controller() {
        return new ScheduleWorkerController();
    }
}
