package com.sequoiacm.common;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.sequoiacm.contentserver.common.ScmSystemUtils;

public class TestScmSystemUtil {
    @Test
    public void testEqualsStringList() {
        Assert.assertFalse(ScmSystemUtils.equals(new ArrayList<String>(), null));
        Assert.assertFalse(ScmSystemUtils.equals(null, new ArrayList<String>()));
        Assert.assertTrue(ScmSystemUtils.equals(null, null));

        Assert.assertTrue(ScmSystemUtils.equals(Arrays.asList("1", "3", "2"),
                Arrays.asList("3", "2", "1")));

        Assert.assertFalse(ScmSystemUtils.equals(Arrays.asList("1", "2"),
                Arrays.asList("2", "1", "3")));
    }
}
