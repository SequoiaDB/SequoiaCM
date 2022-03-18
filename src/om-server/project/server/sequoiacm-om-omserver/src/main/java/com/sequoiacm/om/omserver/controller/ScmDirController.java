package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmDirectoryInfoWithSubDir;
import com.sequoiacm.om.omserver.service.ScmDirService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScmDirController {

    @Autowired
    private ScmDirService dirService;

    @GetMapping(value = "/directory/id/{dir_id}", params = "action=list_sub_dir")
    public List<OmDirectoryInfoWithSubDir> listSubDir(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") int skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false, defaultValue = "{}") BSONObject orderBy,
            @PathVariable(value = "dir_id") String dirId)
            throws ScmInternalException, ScmOmServerException {
        return dirService.listSubDir(session, wsName, dirId, orderBy, skip, limit);
    }
}
