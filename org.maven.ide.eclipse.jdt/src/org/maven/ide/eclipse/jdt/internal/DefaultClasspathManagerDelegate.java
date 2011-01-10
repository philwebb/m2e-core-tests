/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathManagerDelegate;
import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;


/**
 * DefaultClasspathManagerDelegate
 *
 * @author igor
 */
public class DefaultClasspathManagerDelegate implements IClasspathManagerDelegate {
  private final IProjectConfigurationManager configurationManager;

  private final MavenProjectManager projectManager;

  public DefaultClasspathManagerDelegate() {
    this.configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    this.projectManager = MavenPlugin.getDefault().getMavenProjectManager();
  }

  public void populateClasspath(final IClasspathDescriptor classpath, IMavenProjectFacade projectFacade,
      final int kind, final IProgressMonitor monitor) throws CoreException {

    final List<ArtifactFilter> classpathFilters = new ArrayList<ArtifactFilter>();
    for (IJavaProjectConfigurator configurator : getJavaProjectConfigurators(projectFacade, monitor)) {
      ArtifactFilter classpathFilter = (configurator == null ? null : configurator.getClasspathFilter(projectFacade, classpath, monitor));
      if(classpathFilter != null) {
        classpathFilters.add(classpathFilter);
      }
    }

    addClasspathEntries(classpath, projectFacade, kind, monitor, classpathFilters);

    for(IJavaProjectConfigurator configurator : getJavaProjectConfigurators(projectFacade, monitor)) {
      configurator.configureClasspath(projectFacade, classpath, monitor);
    }
  }

  private List<IJavaProjectConfigurator> getJavaProjectConfigurators(IMavenProjectFacade projectFacade,
      final IProgressMonitor monitor) throws CoreException {

    ArrayList<IJavaProjectConfigurator> configurators = new ArrayList<IJavaProjectConfigurator>();

    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, monitor);

    for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(projectFacade, monitor)) {
      if(configurator instanceof IJavaProjectConfigurator) {
        configurators.add((IJavaProjectConfigurator) configurator);
      }
    }

    return configurators;
  }

  void addClasspathEntries(IClasspathDescriptor classpath, IMavenProjectFacade facade, int kind,
      IProgressMonitor monitor, List<ArtifactFilter> classpathFilter) throws CoreException {

    //addClasspathEntries(classpath, facade, kind, monitor);
    //if (true) return;

    AndArtifactFilter filter = new AndArtifactFilter(
        classpathFilter == null ? null : new ArrayList<ArtifactFilter>(classpathFilter));

    if(BuildPathManager.CLASSPATH_RUNTIME == kind) {
      // ECLIPSE-33: runtime+provided scope
      // ECLIPSE-85: adding system scope
      filter.add(new ArtifactFilter() {
        public boolean include(Artifact artifact) {
          return BuildPathManager.SCOPE_FILTER_RUNTIME.include(artifact)
              || Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
              || Artifact.SCOPE_SYSTEM.equals(artifact.getScope());
        }
      });
    } else {
      // ECLIPSE-33: test scope (already includes provided)
      filter.add(BuildPathManager.SCOPE_FILTER_TEST);
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    Set<Artifact> artifacts = new LinkedHashSet<Artifact>(mavenProject.getArtifacts());

    for(IJavaProjectConfigurator configurator : getJavaProjectConfigurators(facade, monitor)) {
      Set<Artifact> additionalArtifacts = configurator.resolveAdditionalArtifacts(facade, monitor);
      if(additionalArtifacts != null) {
        artifacts.addAll(additionalArtifacts);
      }
    }

    for(Artifact a : artifacts) {
      if(!filter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      // project
      IMavenProjectFacade dependency = projectManager
          .getMavenProject(a.getGroupId(), a.getArtifactId(), a.getVersion());
      if(dependency != null && dependency.getProject().equals(facade.getProject())) {
        continue;
      }

      IClasspathEntryDescriptor entry = null;

      if(dependency != null && dependency.getFullPath(a.getFile()) != null) {
        entry = classpath.addProjectEntry(dependency.getFullPath());
      } else {
        File artifactFile = a.getFile();
        if(artifactFile != null /*&& artifactFile.canRead()*/) {
          entry = classpath.addLibraryEntry(Path.fromOSString(artifactFile.getAbsolutePath()));
        }
      }

      if(entry != null) {
        entry.setArtifactKey(new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier()));
        entry.setScope(a.getScope());
        entry.setType(a.getType());
        entry.setClassifier(a.getClassifier());
        entry.setOptionalDependency(a.isOptional());
      }
    }
  }

  void addClasspathEntries(IClasspathDescriptor classpath, IMavenProjectFacade facade, int kind,
      IProgressMonitor monitor) throws CoreException {
    ArtifactFilter scopeFilter;

    if(BuildPathManager.CLASSPATH_RUNTIME == kind) {
      // ECLIPSE-33: runtime+provided scope
      // ECLIPSE-85: adding system scope
      scopeFilter = new ArtifactFilter() {
        public boolean include(Artifact artifact) {
          return BuildPathManager.SCOPE_FILTER_RUNTIME.include(artifact)
              || Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
              || Artifact.SCOPE_SYSTEM.equals(artifact.getScope());
        }
      };
    } else {
      // ECLIPSE-33: test scope (already includes provided)
      scopeFilter = BuildPathManager.SCOPE_FILTER_TEST;
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact a : artifacts) {
      if(!scopeFilter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      // project
      IMavenProjectFacade dependency = projectManager
          .getMavenProject(a.getGroupId(), a.getArtifactId(), a.getVersion());
      if(dependency != null && dependency.getProject().equals(facade.getProject())) {
        continue;
      }

      IClasspathEntryDescriptor entry = null;

      if(dependency != null && dependency.getFullPath(a.getFile()) != null) {
        entry = classpath.addProjectEntry(dependency.getFullPath());
      } else {
        File artifactFile = a.getFile();
        if(artifactFile != null /*&& artifactFile.canRead()*/) {
          entry = classpath.addLibraryEntry(Path.fromOSString(artifactFile.getAbsolutePath()));
        }
      }

      if(entry != null) {
        entry.setArtifactKey(new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier()));
        entry.setScope(a.getScope());
        entry.setOptionalDependency(a.isOptional());
      }
    }
  }

}
