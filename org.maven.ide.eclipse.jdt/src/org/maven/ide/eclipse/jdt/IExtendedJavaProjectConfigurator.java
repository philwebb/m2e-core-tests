/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * IExtendedJavaProjectConfigurator
 *
 * @author webb_p
 */
public interface IExtendedJavaProjectConfigurator extends IJavaProjectConfigurator {

  /**
   * Resolve any additional artifacts that should be included and processed.
   */
  public Set<Artifact> resolveAdditionalArtifacts(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException;

  /**
   * Return an option filter that will be considered when processing classpath artifacts.
   */
  public ArtifactFilter getClasspathFilter(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException;

}
