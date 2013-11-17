/*
 * The MIT License
 *
 * Copyright (c) 2009-2010, Vincent Sellier, Manufacture Française des Pneumatiques Michelin, Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jvnet.hudson.plugins.reports;

import java.net.URI;

import hudson.Extension;

import hudson.model.Hudson;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Label;
//import hudson.model.UnprotectedRootAction;
import hudson.model.Job;
import hudson.model.ListView;
/*<<*/
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
/*>>*/

import hudson.Plugin;

import hudson.matrix.MatrixProject;

import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import hudson.scm.SCM;

import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import static java.util.logging.Level.SEVERE;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public class ReportsLink extends ListView {
	@DataBoundConstructor
	public ReportsLink(String name) {
		super(name);
	}

	private final static Logger LOGGER = Logger.getLogger(ReportsLink.class.getName());

/*<<*/

	@Override
	public void submit(org.kohsuke.stapler.StaplerRequest req) {
	}
	public List<TopLevelItem> getItems() {
		List<TopLevelItem> base = super.getItems(),
				result = new ArrayList<TopLevelItem>(base.size());
		for (TopLevelItem item : base) {
			if (item instanceof Job) {
//				SCMTrigger trigger = ((Job)item).getTrigger(SCMTrigger.class);
//				if (trigger!=null) {
//					result.put(item.getName(),trigger.getSpec());
//				}
			}
		}
		return result;
	}
	public Map getSCMPolling() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<AbstractProject<?, ?>> items = (List)Hudson.getInstance().getItems(AbstractProject.class);
		for (AbstractProject<?, ?> item: items) {
			SCMTrigger trigger = item.getTrigger(SCMTrigger.class);
			if (trigger!=null) {
				map.put(item.getName(),trigger.getSpec());
			}
		}
		return map;
	}
	public Map getCron() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<AbstractProject<?, ?>> items = (List)Hudson.getInstance().getItems(AbstractProject.class);
		for (AbstractProject<?, ?> item: items) {
			TimerTrigger trigger = item.getTrigger(TimerTrigger.class);
			if (trigger!=null) {
				map.put(item.getName(),trigger.getSpec());
			}
		}
		return map;
	}
	public Map getFarmNode() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<MatrixProject> items = (List)Hudson.getInstance().getItems(MatrixProject.class);
		for (MatrixProject item: items) {
			int counter = 0;
			Set<Label> labels = item.getLabels();
			if (labels != null) {
				LOGGER.log(SEVERE,"labels.size()="+labels.size());
				for (Label label: labels) {
					map.put(item.getName()+" "+(counter++),label.getName());
				}
			}
		}
		return map;
	}
	public Map getAssignedNode() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<AbstractProject<?, ?>> items = (List)Hudson.getInstance().getItems(AbstractProject.class);
		for (AbstractProject<?, ?> item: items) {
			Label label = item.getAssignedLabel();
			if (label!=null) {
				map.put(item.getName(),label.getName());
			}
		}
		return map;
	}
	public Map getNode() {
		Map<String,String> map = getFarmNode();
		map.putAll(getAssignedNode());
		return map;
	}
	private final void process_scm(TreeMap<String,String> map, String job_name, SCM scm) 
	throws Exception,Error
	{
		process_scm2(map, job_name, 0, "", scm);
	}
	private final void process_scm2(TreeMap<String,String> map, String job_name, int counter, String scm_prefix, SCM scm) 
	throws Exception,Error
	{
		String scm_name=scm.getClass().getName();
LOGGER.log(SEVERE,"scm_name="+scm_name);
		String scm_simple_name=scm_name;
		if (scm_name.endsWith("hudson.scm.NullSCM")) { // Works
			// No SCM for this job.
		} else if (scm_name.endsWith("hudson.scm.SubversionSCM")) { // Works
			scm_simple_name="svn";
			Method get_locations=scm.getClass().getMethod("getLocations");
			LOGGER.log(SEVERE,"svn:getLocations="+get_locations);

			for (Object location: (Object[])(get_locations.invoke(scm))) {
				map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+location.toString());
			}
		} else if (scm_name.endsWith("hudson.plugins.bazaar.BazaarSCM")) {
			scm_simple_name="bzr";
			Method get_source=scm.getClass().getMethod("getSource");
			LOGGER.log(SEVERE,"bazaar:getSource="+get_source);

			map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+get_source.invoke(scm).toString());
		} else if (scm_name.endsWith("hudson.plugins.git.GitSCM")) { // Works
			scm_simple_name="git";
			Method git_repositories=scm.getClass().getMethod("getRepositories");
			for (RemoteConfig config: (ArrayList<RemoteConfig>)git_repositories.invoke(scm)) {
				for (URIish uri: (List<URIish>)config.getURIs()) {
					LOGGER.log(SEVERE,"git:getRemoteConfigs[x][y]="+uri);
					map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+uri.toString());
				}
			}
		} else if (scm_name.endsWith("org.jenkinsci.plugins.multiplescms.MultiSCM")) {
			scm_simple_name="M:";
			Method get_scms=scm.getClass().getMethod("getConfiguredSCMs");
			LOGGER.log(SEVERE,"multiplescms:getConfiguredSCMs="+get_scms);
			for (SCM scm2: (List<SCM>)get_scms.invoke(scm)) {
				process_scm2(map,job_name,counter++,scm_simple_name,scm2);
			}
		} else if (scm_name.endsWith("hudson.plugins.filesystem_scm.FSSCM")) {
			scm_simple_name="fs";
			Method get_path=scm.getClass().getMethod("getPath");
			LOGGER.log(SEVERE,"FSSCM:getPath="+get_path);

			map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+get_path.invoke(scm).toString());
		} else if (scm_name.endsWith("hudson.plugins.cloneworkspace.CloneWorkspaceSCM")) {
			scm_simple_name="cloneWS";
			Method get_wsname=scm.getClass().getMethod("getParamParentJobName",new Class[]{AbstractBuild.class});
			LOGGER.log(SEVERE,"cloneworkspaceSCM:getParamParentJobName="+get_wsname);

			map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+get_wsname.invoke(scm,new Object[]{null}).toString());
		} else if (scm_name.endsWith("hudson.plugins.mercurial.MercurialSCM")) {
			scm_simple_name="hg";
			Method get_source=scm.getClass().getMethod("getSource");
			LOGGER.log(SEVERE,"mercurial:getSource="+get_source);

			map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+get_source.invoke(scm).toString());
		} else if (scm_name.endsWith("hudson.plugins.perforce.PerforceSCM")) {
			scm_simple_name="p4";
			Method get_project_path=scm.getClass().getMethod("getProjectPath");
			LOGGER.log(SEVERE,"perforce:getProjectPath="+get_project_path);

			map.put(job_name+" "+(counter++), scm_prefix+scm_simple_name+":"+get_project_path.invoke(scm).toString());
		} else {
			map.put(job_name+" "+(counter++), scm_prefix+scm_name+":???");
		}
	}
	public Map getSCM() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<AbstractProject<?, ?>> items = (List)Hudson.getInstance().getItems(AbstractProject.class);
		for (AbstractProject<?, ?> item: items) {
			try {
				process_scm(map, item.getName(), item.getScm());
			} catch (Throwable t) {
				LOGGER.log(SEVERE,"caught:"+t);
			}
		}
		return map;
	}
	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public String getDisplayName() {
return "Reports";
//		    return Messages.DisplayName();
		}
	}
}
/* *** <<
/ *
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Alan Harder
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * /
package hudson.plugins.status_view;

import hudson.model.Descriptor.FormException;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/ **
 * List view type filters jobs by status of the latest build.
 *
 * @author Alan Harder <mindless@dev.java.net>
 * /
public class StatusView extends ListView {

    private boolean stable, unstable, failed, aborted, running;

    public boolean isStable() { return stable; }
    public boolean isUnstable() { return unstable; }
    public boolean isFailed() { return failed; }
    public boolean isAborted() { return aborted; }
    public boolean isRunning() { return running; }

    @Override
    public synchronized List<TopLevelItem> getItems() {
        List<TopLevelItem> base = super.getItems(),
                           result = new ArrayList<TopLevelItem>(base.size());
        for (TopLevelItem item : base) {
            if (item instanceof Job) {
                if (running && !((Job)item).isBuilding()) continue;
                Run lastBuild = ((Job)item).getLastCompletedBuild();
                Result status = lastBuild!=null ? lastBuild.getResult() : null;
                if ( (stable && status == Result.SUCCESS)
                  || (unstable && status == Result.UNSTABLE)
                  || (failed && status == Result.FAILURE)
                  || (aborted && status == Result.ABORTED)
                  || (running && status == null) 
                    result.add(item);
                }
            }
        }
        return result;
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException, FormException {
        stable = req.hasParameter("status_view.stable");
        unstable = req.hasParameter("status_view.unstable");
        failed = req.hasParameter("status_view.failed");
        aborted = req.hasParameter("status_view.aborted");
        running = req.hasParameter("status_view.running");
        super.submit(req);
    }

    @Extension
    public static final class DescriptorImpl extends ViewDescriptor {
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }
}
*** >> */
