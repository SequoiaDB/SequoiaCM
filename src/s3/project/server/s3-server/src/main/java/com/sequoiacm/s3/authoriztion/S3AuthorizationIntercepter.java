//package com.sequoiacm.s3.authoriztion;
//
//import java.util.Date;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import com.sequoiacm.s3.config.AuthorizationConfig;
//import com.sequoiacm.s3.exception.S3Error;
//import com.sequoiacm.s3.exception.S3ServerException;
//
//@Component
//public class S3AuthorizationIntercepter implements HandlerInterceptor {
//    // private static final Logger logger =
//    // LoggerFactory.getLogger(S3AuthorizationIntercepter.class);
//    @Autowired
//    private ScmSessionMgr sessionMgr;
//
//    @Autowired
//    private AuthorizationConfig authConfig;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
//            Object handler) throws Exception {
//        if (!authConfig.isCheck()) {
//            return true;
//        }
//        if (request.getAttribute(ScmSession.class.getName()) != null) {
//            return true;
//        }
//        S3Authorization auth = S3AuthorizationFactory.createAuthentication(request);
//        if (auth == null) {
//            throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
//                    "missing authorization header");
//        }
//        Date now = new Date();
//        if (authConfig.getMaxTimeOffset() > 0
//                && Math.abs(auth.getSignDate().getTime() - now.getTime()) > authConfig
//                        .getMaxTimeOffset()) {
//            throw new S3ServerException(S3Error.REQUEST_TIME_TOO_SKEWED,
//                    "request time too skewed:requestTime=" + auth.getSignDate() + ", serverTime="
//                            + now);
//        }
//
//        ScmSession session = sessionMgr.getSession(auth);
//        request.setAttribute(ScmSession.class.getName(), session);
//        return true;
//    }
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
//            ModelAndView modelAndView) throws Exception {
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
//            Object handler, Exception ex) throws Exception {
//    }
//
//}
