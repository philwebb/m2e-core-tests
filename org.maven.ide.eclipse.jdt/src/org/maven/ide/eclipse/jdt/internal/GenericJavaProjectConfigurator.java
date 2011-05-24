/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IJavaProjectSourceConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


public class GenericJavaProjectConfigurator extends AbstractJavaProjectConfigurator {

  @Override
  protected MavenProject getMavenProject(ProjectConfigurationRequest request, final IProgressMonitor monitor)
      throws CoreException {

    IProject project = request.getProject();
    IFile pomResource = request.getPom();
    ResolverConfiguration configuration = request.getResolverConfiguration();

    console.logMessage("Generating sources " + pomResource.getFullPath());

    monitor.subTask("reading " + pomResource.getFullPath());
    if(mavenConfiguration.isDebugOutput()) {
      console.logMessage("Reading " + pomResource.getFullPath());
    }

    String goalsToExecute = "";
    if(request.isProjectConfigure()) {
      goalsToExecute = mavenConfiguration.getGoalOnUpdate();
    } else if(request.isProjectImport()) {
      goalsToExecute = mavenConfiguration.getGoalOnImport();
    }

    if(goalsToExecute == null || goalsToExecute.trim().length() <= 0) {
      return request.getMavenProject();
    }

    MavenExecutionRequest executionRequest = projectManager.createExecutionRequest(pomResource, configuration, monitor);
    executionRequest.setGoals(Arrays.asList(goalsToExecute.split("[\\s,]+")));
    MavenExecutionResult result = maven.execute(executionRequest, monitor);

    if(result.hasExceptions()) {
      String msg = "Build error for " + pomResource.getFullPath();
      List<Throwable> exceptions = result.getExceptions();
      for(Throwable ex : exceptions) {
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
      markerManager.addMarkers(project, result);
    }

    // TODO optimize project refresh
    monitor.subTask("refreshing");
    // project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
    project.getFolder("target").refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

    List<MavenProject> mavenProjects = result.getTopologicallySortedProjects();

    if(mavenProjects == null) {
      return request.getMavenProject();
    }

    return mavenProjects.get(0);
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.jdt.internal.AbstractJavaProjectConfigurator#addProjectSourceFolders(org.maven.ide.eclipse.jdt.IClasspathDescriptor, org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest, org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void addProjectSourceFolders(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    super.addProjectSourceFolders(classpath, request, mavenProject, monitor);
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(facade, monitor);
    for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(facade, monitor)) {
      if(configurator instanceof IJavaProjectSourceConfigurator) {
        ((IJavaProjectSourceConfigurator) configurator).addProjectSourceFolders(request, classpath, monitor);
      }
    }
  }

  @Override
  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) {
  }

}
