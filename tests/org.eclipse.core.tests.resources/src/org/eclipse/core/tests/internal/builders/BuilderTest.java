/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class tests public API related to building and to build specifications.
 * Specifically, the following methods are tested:
 * 
 * IWorkspace#build IProject#build IProjectDescription#getBuildSpec
 * IProjectDescription#setBuildSpec
 */
public class BuilderTest extends AbstractBuilderTest {
	public static Test suite() {
		return new TestSuite(BuilderTest.class);
//		TestSuite suite = new TestSuite();
//		suite.addTest(new BuilderTest("testChangeDynamicBuildOrder"));
//		return suite;
	}

	public BuilderTest() {
		super(null);
	}

	/**
	 * BuilderTest constructor comment.
	 * 
	 * @param name
	 *                  java.lang.String
	 */
	public BuilderTest(String name) {
		super(name);
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		getWorkspace().getRoot().delete(true, null);
		TestBuilder builder = SortBuilder.getInstance();
		if (builder != null)
			builder.reset();
		builder = DeltaVerifierBuilder.getInstance();
		if (builder != null)
			builder.reset();
	}

	/**
	 * Tests the lifecycle of a builder.
	 * 
	 * @see SortBuilder
	 */
	public void testAutoBuildPR() {
		//REF: 1FUQUJ4
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject project1 = workspace.getRoot().getProject("PROJECT" + 1);
		IFolder folder = project1.getFolder("FOLDER");
		IFolder sub = folder.getFolder("sub");
		IFile fileA = folder.getFile("A");
		IFile fileB = sub.getFile("B");
		// Create some resources
		try {
			// Turn auto-building on
			setAutoBuilding(true);
			project1.create(getMonitor());
			project1.open(getMonitor());
			// Set build spec
			IProjectDescription desc = project1.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			command.getArguments().put(TestBuilder.BUILD_ID, "Project1Build1");
			desc.setBuildSpec(new ICommand[] {command});
			project1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Create folders and files
		try {
			folder.create(true, true, getMonitor());
			fileA.create(getRandomContents(), true, getMonitor());
			sub.create(true, true, getMonitor());
			fileB.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Tests installing and running a builder that always fails during
	 * instantation.
	 */
	public void testBrokenBuilder() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		try {
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			ICommand command1 = desc.newCommand();
			command1.setBuilderName(BrokenBuilder.BUILDER_NAME);
			ICommand command2 = desc.newCommand();
			command2.setBuilderName(SortBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command1, command2});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		//do an incremental build -- build should fail, but second builder
		// should run
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			fail("3.0");
		} catch (CoreException e) {
			//expected
		}
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents("3.1");
		//build again -- it should suceed this time
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	public void testBuildClean() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2")});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		//start with a clean build
		try {
			getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}
		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		assertTrue("3.2", verifier.wasCleanBuild());
		// Now do an incremental build - since delta was null it should appear as a clean build
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.3", e);
		}
		assertTrue("3.4", verifier.wasFullBuild());
		// next time it will appear as an incremental build
		try {
			project.touch(getMonitor());
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.5", e);
		}
		assertTrue("3.6", verifier.wasIncrementalBuild());
		//do another clean
		try {
			getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.7", e);
		}
		assertTrue("3.8", verifier.wasCleanBuild());
		//doing a full build should still look like a full build
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.9", e);
		}
		assertTrue("3.10", verifier.wasFullBuild());
	}

	/**
	 * Tests the lifecycle of a builder.
	 * 
	 * @see SortBuilder
	 */
	public void testBuildCommands() {
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject project1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject project2 = workspace.getRoot().getProject("PROJECT" + 2);
		IFile file1 = project1.getFile("FILE1");
		IFile file2 = project2.getFile("FILE2");
		//set the build order
		try {
			IWorkspaceDescription workspaceDesc = workspace.getDescription();
			workspaceDesc.setBuildOrder(new String[] {project1.getName(), project2.getName()});
			workspace.setDescription(workspaceDesc);
		} catch (CoreException e) {
			fail("0.0", e);
		}
		TestBuilder verifier = null;
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			project1.create(getMonitor());
			project1.open(getMonitor());
			project2.create(getMonitor());
			project2.open(getMonitor());
			file1.create(getRandomContents(), true, getMonitor());
			file2.create(getRandomContents(), true, getMonitor());
			// Do an initial build to get the builder instance
			IProjectDescription desc = project1.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Project1Build1")});
			project1.setDescription(desc, getMonitor());
			project1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier = SortBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Project1Build1");
			verifier.assertLifecycleEvents("1.0");
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Build spec with no commands
		try {
			IProjectDescription desc = project1.getDescription();
			desc.setBuildSpec(new ICommand[] {});
			project1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		// Build the project -- should do nothing
		try {
			verifier.reset();
			dirty(file1);
			project1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.assertLifecycleEvents("3.1");
		} catch (CoreException e) {
			fail("3.99", e);
		}
		// Build command with no arguments -- will use default build ID
		try {
			IProjectDescription desc = project1.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}
		// Build the project
		// Note that since the arguments have changed, the identity of the build
		// command is different so a new builder will be instantiated
		try {
			dirty(file1);
			project1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("5.2");
		} catch (CoreException e) {
			fail("5.99", e);
		}
		// Create and set a build specs for project one
		try {
			IProjectDescription desc = project1.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Project1Build1")});
			project1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("6.99", e);
		}
		// Create and set a build spec for project two
		try {
			IProjectDescription desc = project2.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, SortBuilder.BUILDER_NAME, "Project2Build1"), createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2")});
			project2.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("7.99", e);
		}
		// Build
		try {
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Project1Build1");
			//second builder is touched for the first time
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Project2Build1");
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Project2Build2");
			dirty(file1);
			dirty(file2);
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.assertLifecycleEvents("8.0");
			verifier.addExpectedLifecycleEvent("Project1Build1");
			dirty(file1);
			project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.assertLifecycleEvents("8.2");
			dirty(file2);
			project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Project2Build1");
			verifier.addExpectedLifecycleEvent("Project2Build2");
			verifier.assertLifecycleEvents("8.3");
		} catch (CoreException e) {
			fail("8.99", e);
		}
		// Change order of build commands
		try {
			IProjectDescription desc = project2.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2"), createCommand(desc, SortBuilder.BUILDER_NAME, "Project2Build1")});
			project2.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("10.99", e);
		}
		// Build
		try {
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Project1Build1");
			verifier.addExpectedLifecycleEvent("Project2Build2");
			verifier.addExpectedLifecycleEvent("Project2Build1");
			verifier.assertLifecycleEvents("11.0");
			dirty(file1);
			dirty(file2);
			project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Project1Build1");
			verifier.assertLifecycleEvents("11.2");
			dirty(file1);
			dirty(file2);
			project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Project2Build2");
			verifier.addExpectedLifecycleEvent("Project2Build1");
			verifier.assertLifecycleEvents("11.3 ");
		} catch (CoreException e) {
			fail("11.99", e);
		}
	}

	/**
	 * Tests the lifecycle of a builder.
	 * 
	 * @see SortBuilder
	 */
	public void testBuildOrder() {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			proj1.create(getMonitor());
			proj1.open(getMonitor());
			proj2.create(getMonitor());
			proj2.open(getMonitor());
			//set the build order
			setBuildOrder(proj1, proj2);
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Create and set a build specs for project one
		try {
			IProjectDescription desc = proj1.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
			proj1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		// Create and set a build spec for project two
		try {
			IProjectDescription desc = proj2.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build1"), createCommand(desc, "Build2")});
			proj2.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = null;
		// Build the workspace
		try {
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier = SortBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.assertLifecycleEvents("4.0 ");
		} catch (CoreException e) {
			fail("4.99", e);
		}
		//build in reverse order
		try {
			setBuildOrder(proj2, proj1);
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.assertLifecycleEvents("5.0");
		} catch (CoreException e) {
			fail("5.99");
		}
		//only specify build order for project1
		try {
			setBuildOrder(proj1);
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.assertLifecycleEvents("6.0");
		} catch (CoreException e) {
			fail("6.99");
		}
		//only specify build order for project2
		try {
			setBuildOrder(proj2, proj1);
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.assertLifecycleEvents("7.0");
		} catch (CoreException e) {
			fail("7.99");
		}
	}

	/**
	 * Ensure that build order is preserved when project is closed/opened.
	 */
	public void testCloseOpenProject() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		try {
			// Create some resources
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Create and set a build spec
		try {
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build1"), createCommand(desc, "Build2")});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		try {
			project.close(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		//ensure the build spec hasn't changed
		try {
			IProjectDescription desc = project.getDescription();
			ICommand[] commands = desc.getBuildSpec();
			assertEquals("4.0", 2, commands.length);
			assertEquals("4.1", commands[0].getBuilderName(), SortBuilder.BUILDER_NAME);
			assertEquals("4.2", commands[1].getBuilderName(), SortBuilder.BUILDER_NAME);
			Map args = commands[0].getArguments();
			assertEquals("4.3", "Build1", (String) args.get(TestBuilder.BUILD_ID));
			args = commands[1].getArguments();
			assertEquals("4.4", "Build2", (String) args.get(TestBuilder.BUILD_ID));
		} catch (CoreException e) {
			fail("4.99", e);
		}
	}

	/**
	 * Tests an implicit workspace build order created by setting dynamic
	 * project references.
	 */
	public void testDynamicBuildOrder() {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			proj1.create(getMonitor());
			proj1.open(getMonitor());
			proj2.create(getMonitor());
			proj2.open(getMonitor());
			//establish a build order by adding a dynamic reference from
			// proj2->proj1
			IProjectDescription description = proj2.getDescription();
			description.setDynamicReferences(new IProject[] {proj1});
			proj2.setDescription(description, IResource.NONE, null);
			IWorkspaceDescription wsDescription = getWorkspace().getDescription();
			wsDescription.setBuildOrder(null);
			getWorkspace().setDescription(wsDescription);
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Create and set a build specs for project one
		try {
			IProjectDescription desc = proj1.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
			proj1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		// Create and set a build spec for project two
		try {
			IProjectDescription desc = proj2.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build1"), createCommand(desc, "Build2")});
			proj2.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = null;
		// Build the workspace
		try {
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier = SortBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.assertLifecycleEvents("4.0 ");
		} catch (CoreException e) {
			fail("4.99", e);
		}
		//build in reverse order
		try {
			//reverse the order by adding a dynamic reference from proj1->proj2
			IProjectDescription description = proj2.getDescription();
			description.setDynamicReferences(new IProject[0]);
			proj2.setDescription(description, IResource.NONE, null);
			description = proj1.getDescription();
			description.setDynamicReferences(new IProject[] {proj2});
			proj1.setDescription(description, IResource.NONE, null);
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build2");
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.assertLifecycleEvents("5.0");
		} catch (CoreException e) {
			fail("5.99");
		}
	}
	/**
	 * Tests that changing the dynamic build order will induce an autobuild on a project.
	 * This is a regression test for bug 60653.
	 */
	public void testChangeDynamicBuildOrder() {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		final IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		final IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);
		try {
			// Turn auto-building on and make sure there is no explicit build order
			setAutoBuilding(true);
			IWorkspaceDescription wsDescription = getWorkspace().getDescription();
			wsDescription.setBuildOrder(null);
			getWorkspace().setDescription(wsDescription);
			// Create and set a build spec for project two
			getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					proj2.create(getMonitor());
					proj2.open(getMonitor());
					IProjectDescription desc = proj2.getDescription();
					desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build1")});
					proj2.setDescription(desc, getMonitor());
				}
			}, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.reset();
		//create project two and establish a build order by adding a dynamic 
		//reference from proj2->proj1 in the same operation
		try {
			getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					// Create and set a build specs for project one
					proj1.create(getMonitor());
					proj1.open(getMonitor());
					IProjectDescription desc = proj1.getDescription();
					desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
					proj1.setDescription(desc, getMonitor());

					//add the dynamic reference to project two
					IProjectDescription description = proj2.getDescription();
					description.setDynamicReferences(new IProject[] {proj1});
					proj2.setDescription(description, IResource.NONE, null);
				}
			}, getMonitor());
		} catch (CoreException e1) {
			fail("2.99", e1);
		}
		waitForBuild();
		//ensure the build happened in the correct order, and that both projects were built
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.assertLifecycleEvents("3.0");
	}

	/**
	 * Tests the method IncrementProjectBuilder.forgetLastBuiltState
	 */
	public void testForgetLastBuiltState() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		SortBuilder verifier = null;
		//do an initial build
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, getMonitor());
			verifier = SortBuilder.getInstance();
		} catch (CoreException e) {
			fail("3.2", e);
		}
		//forget last built state
		verifier.forgetLastBuiltState();
		// Now do another incremental build. Delta should be null
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, getMonitor());
			assertTrue("4.0", verifier.wasDeltaNull());
		} catch (CoreException e) {
			fail("4.99", e);
		}
		// Do another incremental build, requesting a null build state. Delta
		// should not be null
		verifier.requestForgetLastBuildState();
		try {
			project.touch(getMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, getMonitor());
			assertTrue("5.0", !verifier.wasDeltaNull());
		} catch (CoreException e) {
			fail("5.99", e);
		}
		//try a snapshot when a builder has a null tree
		try {
			getWorkspace().save(false, getMonitor());
		} catch (CoreException e) {
			fail("6.99");
		}
		// Do another incremental build. Delta should be null
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, getMonitor());
			assertTrue("7.0", verifier.wasDeltaNull());
		} catch (CoreException e) {
			fail("7.99", e);
		}
		// Delete the project
		try {
			project.delete(false, getMonitor());
		} catch (CoreException e) {
			fail("99.99", e);
		}
	}

	/**
	 * Tests the lifecycle of a builder.
	 */
	public void testLifecycleEvents() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = null;
		//try to do an incremental build when there has never
		//been a batch build
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier = SortBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("3.1");
		} catch (CoreException e) {
			fail("3.2", e);
		}
		// Now do another incremental build. Since we just did one, nothing
		// should happen in this one.
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.assertLifecycleEvents("3.4");
		} catch (CoreException e) {
			fail("3.5", e);
		}
		// Now do a batch build
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("3.6");
		} catch (CoreException e) {
			fail("3.8", e);
		}
		// Close the project
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("4.1", e);
		}
		// Open the project, build it, and delete it
		try {
			project.open(getMonitor());
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
			project.delete(false, getMonitor());
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("5.0");
		} catch (CoreException e) {
			fail("5.1", e);
		}
	}

	/**
	 * Tests the lifecycle of a builder.
	 * 
	 * @see SortBuilder
	 */
	public void testMoveProject() {
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("Destination");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			proj1.create(getMonitor());
			proj1.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build specs for project one
		try {
			IProjectDescription desc = proj1.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			command.getArguments().put(TestBuilder.BUILD_ID, "Build0");
			desc.setBuildSpec(new ICommand[] {command});
			proj1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// build project1
		try {
			proj1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
		// move proj1 to proj2
		try {
			proj1.move(proj2.getFullPath(), false, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		// build proj2
		try {
			proj2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
	}

	/**
	 * Tests that turning autobuild on will invoke a build in the next
	 * operation.
	 */
	public void testTurnOnAutobuild() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		final IFile file = project.getFile("File.txt");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = null;
		//try to do an incremental build when there has never
		//been a batch build
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier = SortBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("3.1");
		} catch (CoreException e) {
			fail("3.2", e);
		}
		// Now make a change and then turn autobuild on. Turning it on should
		// cause a build.
		IWorkspaceRunnable r = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				file.setContents(getRandomContents(), IResource.NONE, getMonitor());
				IWorkspaceDescription desc = getWorkspace().getDescription();
				desc.setAutoBuilding(true);
				getWorkspace().setDescription(desc);
			}
		};
		waitForBuild();
		getWorkspace().run(r, getMonitor());
		waitForBuild();
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents("4.0");
	}
}