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

import hudson.Extension;

import hudson.model.Hudson;
import hudson.model.AbstractProject;
import hudson.model.Label;
import hudson.model.ManagementLink;
import hudson.model.RootAction;

import hudson.matrix.Combination;
import hudson.matrix.MatrixProject;

import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;

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

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.LargeText;

@Extension
public class ReportsLink implements RootAction {
	private final static Logger LOGGER = Logger.getLogger(ReportsLink.class.getName());

	private Boolean backupRunning = Boolean.FALSE;

	@Override
	public String getIconFileName() {
		return "/plugin/reports/images/reports.gif";
	}

	@Override
	public String getUrlName() {
		return "reports";
	}

	public String getDisplayName() {
		return Messages.display_name();
	}

	public String getDescription() {
		return Messages.description();
	}

	public List<String> getExtensions() {
		List<String> extensions = new ArrayList();
		return extensions;
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
	public Map getSubversion() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Hudson hudson=Hudson.getInstance();
		List<AbstractProject<?, ?>> items = (List)Hudson.getInstance().getItems(AbstractProject.class);
		for (AbstractProject<?, ?> item: items) {
			int counter = 0;
try {
			if (item.getScm().getClass().getName().indexOf("SubversionSCM")<0) continue;
			Method get_locations=item.getScm().getClass().getMethod("getLocations");
			LOGGER.log(SEVERE,"get_locations="+get_locations);

				for (Object location: (Object[])(get_locations.invoke(item.getScm()))) {
					map.put(item.getName()+" "+(counter++),location.toString());
				}
} catch (Throwable t) {
LOGGER.log(SEVERE,"caught:"+t);
}
		}
		return map;
	}
}
