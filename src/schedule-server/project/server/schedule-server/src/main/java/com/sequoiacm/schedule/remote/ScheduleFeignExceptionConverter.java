//package com.sequoiacm.schedule.remote;
//
//import com.sequoiacm.infrastructure.feign.ScmFeignException;
//import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;
//import com.sequoiacm.schedule.comm.model.ScheduleException;
//
//class ScheduleFeignExceptionConverter implements ScmFeignExceptionConverter<ScheduleException> {
//    @Override
//    public ScheduleException convert(ScmFeignException e) {
//        ScheduleException ex = new ScheduleException(e.getError(), e.getMessage(), e);
//        ex.setLocation(e.getPath());
//        return ex;
//    }
//}
