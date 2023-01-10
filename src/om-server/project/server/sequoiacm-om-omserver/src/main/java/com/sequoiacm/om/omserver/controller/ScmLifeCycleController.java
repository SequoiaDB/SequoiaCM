package com.sequoiacm.om.omserver.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.lifecycle.OmStageTagDetail;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionBasic;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionFilter;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionSchedule;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionWithWs;
import com.sequoiacm.om.omserver.service.ScmLifeCycleService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1/lifeCycle")
public class ScmLifeCycleController {

    @Autowired
    private ScmLifeCycleService lifeCycleService;

    @GetMapping("/stageTag")
    public List<OmStageTagDetail> listStageTag(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            HttpServletResponse response) throws ScmOmServerException, ScmInternalException {
        long stageTagCount = lifeCycleService.countStageTag(session, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(stageTagCount));
        if (stageTagCount <= 0) {
            return Collections.emptyList();
        }
        return lifeCycleService.listStageTag(session, filter, skip, limit);
    }

    @PostMapping("/stageTag")
    public void createStageTag(ScmOmSession session,
            @RequestParam(value = RestParamDefine.STAGE_TAG_NAME) String name,
            @RequestParam(value = RestParamDefine.STAGE_TAG_DESCRIPTION, required = false) String description)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.crateStageTag(session, name, description);
    }

    @DeleteMapping("/stageTag/{name:.+}")
    public void deleteStageTag(@PathVariable("name") String name, ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.deleteStageTag(session, name);
    }

    @PutMapping(value = "/site", params = "action=addStageTag")
    public void addSiteStageTag(@RequestParam(value = RestParamDefine.SITE_NAME) String siteName,
            @RequestParam(value = RestParamDefine.STAGE_TAG) String stageTag, ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.addSiteStageTag(session, siteName, stageTag);
    }

    @PutMapping(value = "/site", params = "action=removeStageTag")
    public void removeSiteStageTag(@RequestParam(value = RestParamDefine.SITE_NAME) String siteName,
            ScmOmSession session) throws ScmOmServerException, ScmInternalException {
        lifeCycleService.removeStageTag(session, siteName);
    }

    @GetMapping("/transition")
    public List<OmTransitionWithWs> listTransition(ScmOmSession session,
            @RequestParam(value = "transition_filter", required = false, defaultValue = "{}") BSONObject transitionFilter,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            HttpServletResponse response) throws ScmOmServerException, ScmInternalException {
        OmTransitionFilter omTransitionFilter = new OmTransitionFilter(transitionFilter);
        long stageTagCount = lifeCycleService.countTransition(session, omTransitionFilter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(stageTagCount));
        if (stageTagCount <= 0) {
            return Collections.emptyList();
        }
        return lifeCycleService.listTransition(session, omTransitionFilter, skip, limit);
    }

    @PostMapping("/transition")
    public void createTransition(ScmOmSession session,
            @RequestBody OmTransitionBasic omTransitionBasic)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.createTransition(session, omTransitionBasic);
    }

    @PutMapping("/transition")
    public void updateTransition(ScmOmSession session,
            @RequestParam(value = RestParamDefine.OLD_TRANSITION) String oldTransition,
            @RequestBody OmTransitionBasic omTransitionBasic)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.updateTransition(session, oldTransition, omTransitionBasic);
    }

    @PutMapping(value = "/transition/{name:.+}", params = "action=addApply")
    public List<OmBatchOpResult> addTransitionApply(ScmOmSession session,
            @PathVariable("name") String transition, @RequestBody List<String> wsList)
            throws ScmOmServerException, ScmInternalException {
        return lifeCycleService.addTransitionApply(session, transition, wsList);
    }

    @PutMapping(value = "/transition/{name:.+}", params = "action=removeApply")
    public List<OmBatchOpResult> removeTransitionApply(ScmOmSession session,
            @PathVariable("name") String transition,
            @RequestBody List<String> wsList) throws ScmOmServerException, ScmInternalException {
        return lifeCycleService.removeTransitionApply(session, transition, wsList);
    }

    @DeleteMapping("/transition/{name:.+}")
    public void deleteTransition(ScmOmSession session, @PathVariable("name") String name)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.deleteTransition(session, name);
    }

    @GetMapping("/workspace/transition/{workspace_name:.+}")
    public List<OmTransitionSchedule> listTransitionByWs(ScmOmSession session,
            @PathVariable("workspace_name") String workspaceName)
            throws ScmOmServerException, ScmInternalException {
        return lifeCycleService.listTransitionByWs(session, workspaceName);
    }

    @PostMapping("/workspace/transition/{workspace_name:.+}")
    public void addWsTransition(ScmOmSession session,
            @RequestBody OmTransitionBasic omTransitionBasic,
            @PathVariable("workspace_name") String workspaceName)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.addTransition(session, omTransitionBasic, workspaceName);
    }

    @PutMapping("/workspace/transition/{workspace_name:.+}")
    public void updateWsTransition(ScmOmSession session,
            @RequestParam(value = RestParamDefine.OLD_TRANSITION) String oldTransition,
            @RequestBody OmTransitionBasic omTransitionBasic,
            @PathVariable("workspace_name") String workspaceName)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.updateWsTransition(session, oldTransition, omTransitionBasic,
                workspaceName);
    }

    @PutMapping(value = "/workspace/transition/{workspace_name:.+}", params = "action=changeState")
    public void changeTransitionState(ScmOmSession session,
            @PathVariable("workspace_name") String workspaceName,
            @RequestParam(value = RestParamDefine.TRANSITION) String transition,
            @RequestParam(value = RestParamDefine.TRANSITION_ENABLED) boolean isEnabled)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.changeTransitionState(session, workspaceName, transition, isEnabled);
    }

    @DeleteMapping("/workspace/transition/{workspace_name:.+}")
    public void removeTransition(ScmOmSession session,
            @PathVariable("workspace_name") String workspaceName,
            @RequestParam(value = RestParamDefine.TRANSITION) String transition)
            throws ScmOmServerException, ScmInternalException {
        lifeCycleService.removeTransition(session, workspaceName, transition);
    }
}
