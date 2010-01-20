package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.embedder.MavenImpl;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;

public class ProjectConfigurationManagerTest extends AsbtractMavenProjectTestCase {

  public void testBasedirRenameRequired() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_REQUIRED);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("maven-project");
    assertNotNull(project);
    IMavenProjectFacade facade = plugin.getMavenProjectManager().getMavenProject("MNGECLIPSE-1793_basedirRename", "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  public void testBasedirRenameNo() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_NO);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("mavenNNNNNNN");
    assertNotNull(project);
    IMavenProjectFacade facade = plugin.getMavenProjectManager().getMavenProject("MNGECLIPSE-1793_basedirRename", "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  private void testBasedirRename(int renameRequired) throws IOException, CoreException {
    IWorkspaceRoot root = workspace.getRoot();
    final MavenPlugin plugin = MavenPlugin.getDefault();

    String pathname = "projects/MNGECLIPSE-1793_basedirRename/mavenNNNNNNN";
    File src = new File(pathname);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    projectInfos.add(new MavenProjectInfo("label", new File(dst, "pom.xml"), null, null));
    projectInfos.get(0).setBasedirRename(renameRequired);

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(new ResolverConfiguration());

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        plugin.getProjectConfigurationManager().importProjects(projectInfos, importConfiguration, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
  }

  public void testWorkspaceResolutionOfInterModuleDependenciesDuringImport() throws Exception {
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      PlexusContainer container = ((MavenImpl) MavenPlugin.getDefault().getMaven()).getPlexusContainer();
      ComponentDescriptor<Wagon> descriptor = new ComponentDescriptor<Wagon>();
      descriptor.setRealm(container.getContainerRealm());
      descriptor.setRoleClass(Wagon.class);
      descriptor.setImplementationClass(FilexWagon.class);
      descriptor.setRoleHint("mngeclipse1990");
      descriptor.setInstantiationStrategy("singleton");
      container.addComponentDescriptor(descriptor);
      FilexWagon.setRequestFilterPattern("test/.*", true);
      mavenConfiguration.setUserSettingsFile(new File("projects/MNGECLIPSE-1990/settings.xml").getAbsolutePath());
      importProjects("projects/MNGECLIPSE-1990", new String[] {"pom.xml", "dependent/pom.xml", "dependency/pom.xml",
          "parent/pom.xml"}, new ResolverConfiguration());
      List<String> requests = FilexWagon.getRequests();
      assertTrue("Dependency resolution was attempted from remote repository: " + requests, requests.isEmpty());
    } finally {
      mavenConfiguration.setUserSettingsFile(oldSettings);
    }
  }

}
