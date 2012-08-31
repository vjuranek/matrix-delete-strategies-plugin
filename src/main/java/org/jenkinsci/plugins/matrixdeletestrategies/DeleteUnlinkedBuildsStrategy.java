package org.jenkinsci.plugins.matrixdeletestrategies;

import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixDeleteStrategy;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Deletes all sub-builds, which are not linked by other builds. If the sub-builds are not linked, <strong>deletes also sub-builds
 * marked as keep forever</strong>. The only criteria here is if the build is linked or not. 
 * 
 * @author vjuranek
 *
 */
public class DeleteUnlinkedBuildsStrategy extends MatrixDeleteStrategy {
    
    @DataBoundConstructor
    public DeleteUnlinkedBuildsStrategy() {
        
    }
    
    public void doDelete(MatrixBuild b) throws MatrixDeleteException, IOException {
        b.checkPermission(b.DELETE);
        
        List<MatrixRun> linkedRuns = getLinkedRuns(b);
        List<MatrixRun> runs = b.getExactRuns();
        runs.removeAll(linkedRuns);
        for(MatrixRun run : runs)
            //if(((AbstractBuild<?,?>)run).getWhyKeepLog() == null)
                run.delete();
        
        if (b.getWhyKeepLog()==null)
            b.delete();
    }
    
    /**
     * 
     * @return {@List<MatrixRun>} of runs, which are linked by younger builds. Can be empty, but never null.
     */
    public List<MatrixRun> getLinkedRuns(MatrixBuild b) {
        List<MatrixRun> linkedRuns = new ArrayList<MatrixRun>();
        MatrixBuild nb = b.getNextBuild();
        if(null == nb) 
            return linkedRuns;  // this is the latest build, cannot be linked
        
        for(MatrixConfiguration c : b.getParent().getActiveConfigurations()) {
            MatrixRun r = c.getNearestOldBuild(nb.getNumber());
            if (r != null && r.getNumber()==b.getNumber()) // linked builds has to be linked by next builds 
                linkedRuns.add(r);
        }
        return linkedRuns;
    }
    
    @Extension
    public static class DescriptorImpl extends MatrixDeleteStrategyDescriptor {
        @Override
        public String getDisplayName() {
            return "Delete unlinked sub-builds";
        }
    }

}
