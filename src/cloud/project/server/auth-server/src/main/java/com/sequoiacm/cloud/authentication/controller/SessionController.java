package com.sequoiacm.cloud.authentication.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.session.data.sequoiadb.SequoiadbSession;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.ForbiddenException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;

@RequestMapping("/api/v1")
@RestController
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SequoiadbSessionRepository sessionRepository;

    @Autowired
    private ScmUserRoleRepository userRoleRepository;

    @GetMapping("/sessions")
    public List<SequoiadbSession> getAllSessions(@RequestParam(value = "username", required = false) String username) {
        if (StringUtils.hasText(username)) {
            ScmUser user = userRoleRepository.findUserByName(username);
            if (user == null) {
                throw new NotFoundException("User does not exist: " + username);
            }
        }
        return sessionRepository.getAllSessions(username, false);
    }

    @GetMapping("/sessions/{sessionId}")
    public SequoiadbSession getSession(@PathVariable(value = "sessionId") String sessionId,
                                       @RequestParam(value = "user_details", required = false) Boolean userDetails) {
        SequoiadbSession session = sessionRepository.getSession(sessionId, false);
        if (session == null) {
            throw new NotFoundException("Session is not found: " + sessionId);
        }
        if (userDetails != null && userDetails) {
            String username = session.getPrincipal();
            if (StringUtils.hasText(username)) {
                ScmUser user = userRoleRepository.findUserByName(session.getPrincipal());
                session.setAttribute("user_details", user);
            }
        }
        return session;
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable(value = "sessionId") String sessionId,
                              HttpSession currentSession) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BadRequestException("Invalid session id");
        }
        if (sessionId.equals(currentSession.getId())) {
            throw new ForbiddenException("Cannot delete current session");
        }
        SequoiadbSession session = sessionRepository.getSession(sessionId, true);
        if (session == null) {
            throw new NotFoundException("Session is not found: " + sessionId);
        }
        sessionRepository.delete(sessionId);
    }
    
    @RequestMapping(value = "/sessions", method = RequestMethod.HEAD)
    public ResponseEntity<String> countSessions(HttpServletResponse response) {
    	long count = sessionRepository.countSessions();
    	response.setHeader("X-SCM-Count", String.valueOf(count));
    	return ResponseEntity.ok("");
    }
}
