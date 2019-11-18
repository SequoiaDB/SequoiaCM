/**
 * 
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.testng.SkipException;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;


/**
 * @Description BreakpointUtil.java
 * @author luweikang
 * @date 2018年5月16日
 */
public class BreakpointUtil extends TestScmBase {
	public static void checkDBDataSource(){
		List<SiteWrapper> sites = ScmInfo.getAllSites();
		for (SiteWrapper site : sites) {			
			DatasourceType dsType = site.getDataType();
			if (!dsType.equals(DatasourceType.SEQUOIADB) ) {				
				throw new SkipException("breakpoint file only support sequoiadb datasourse, skip!");
			}
		}
	}
}

class BreakpointStream extends InputStream{

	private FileInputStream in = null;
	private int finishByteNum = 0;
	private int breakNum;
	
	public BreakpointStream(String filePath, int breakNum ) throws FileNotFoundException{
		this.in = new FileInputStream( filePath );
		this.breakNum = breakNum;
	}
	
	@Override
	public int available() throws IOException {
        return in.available();
		//return 0;
    }
	
	@Override
	public int read() throws IOException {
		int rs = in.read();	
		
		if( finishByteNum >= breakNum ){			
			rs = -1;
		}
		++finishByteNum;		
		return rs;
	}
	
}

