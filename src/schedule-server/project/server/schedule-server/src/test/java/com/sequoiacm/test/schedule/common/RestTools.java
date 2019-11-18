package com.sequoiacm.test.schedule.common;

import org.bson.BSONObject;

public abstract class RestTools {
    private String rootUrl;

    public RestTools(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getRootUrl() {
        return this.rootUrl;
    }

    public abstract String login(String user, String passwd);

    public abstract String getName(String string);

    public abstract void createSchedule(String name, String desc, String type, BSONObject content,
            String workspace, String cron);

    public abstract void getSchedule(String string);

    public abstract void deleteSchedule(String string);

    public abstract void listSchedule();

    public abstract int getVersion();

    public abstract void listPrivileges();

    public abstract void listUsers();
}
