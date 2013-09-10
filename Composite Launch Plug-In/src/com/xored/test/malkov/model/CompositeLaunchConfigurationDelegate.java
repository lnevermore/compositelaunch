package com.xored.test.malkov.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

import com.xored.test.malkov.ui.launchdialog.CompositeLaunchTab;

/**
 * 
 * @author malkov
 * 
 */
public class CompositeLaunchConfigurationDelegate extends
		AbstractJavaLaunchConfigurationDelegate {

	private boolean panicEnabled;
	private boolean buildEnabled;

	private Set<ILaunchConfiguration> launchSet;

	@Override
	@SuppressWarnings("unchecked")
	public void launch(ILaunchConfiguration configuration, final String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		System.out.println(mode);
		Map<String, String> preLaunchesSet = configuration.getAttribute(
				CompositeLaunchTab.LAUNCHES_SET_ATTRIBUTE_KEY,
				new HashMap<String, String>());
		panicEnabled = configuration.getAttribute(
				CompositeLaunchTab.PANIC_RUN_ATTRIBUTE, false);
		buildEnabled = configuration.getAttribute(
				CompositeLaunchTab.BUILD_WITH_RUN_ATTRIBUTE, false);

		this.launchSet = new HashSet<>();

		buildLaunchesForRun(preLaunchesSet);

		compositeLaunch(mode, launch, monitor);
	}

	private void compositeLaunch(String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		// launchConfigsOneByOne(mode, launch, monitor);
		launchConfigsSynchronusly(mode, launch, monitor);
	}

	private void buildLaunchesForRun(Map<String, String> preLaunchesSet)
			throws CoreException {

		final ILaunchConfiguration[] configurations = getLaunchManager()
				.getLaunchConfigurations();
		for (final Map.Entry<String, String> entry : preLaunchesSet.entrySet()) {

			if (Boolean.parseBoolean(entry.getValue())) {

				for (final ILaunchConfiguration config : configurations) {

					if (entry.getKey().equals(config.getName())) {

						this.launchSet.add(config);

					}
				}
			}
		}
	}

	private void launchConfigsSynchronusly(final String mode, ILaunch launch,
			IProgressMonitor monitor) {

		int i = 0;
		monitor.beginTask("Start launching synchronusly", launchSet.size());
		final List<ILaunch> alreadyLaunched = new ArrayList<ILaunch>();
		for (final ILaunchConfiguration configuration : launchSet) {
			try {
				if (monitor.isCanceled()) {
					break;
				}
				final SubProgressMonitor subMonitor = new SubProgressMonitor(
						monitor, 0);

				alreadyLaunched.add(configuration.launch(mode, subMonitor,
						buildEnabled, true));

				monitor.subTask("Launching " + configuration.getName()
						+ " is complete");

				monitor.worked(++i);
			} catch (CoreException core) {
				if (panicEnabled) {
					for (final ILaunch il : alreadyLaunched) {
						terminate(il);
					}
				}
			}
		}
		monitor.done();
	}

	// looks like there is no way to launch configurations in order
	// can't wait til some launch has been finished
	/*
	 * private void launchConfigsOneByOne(String mode, ILaunch launch,
	 * IProgressMonitor monitor) {
	 * 
	 * int i = 0; monitor.beginTask("Start launching one by one",
	 * launchSet.size()); final List<ILaunch> alreadyLaunched = new
	 * ArrayList<ILaunch>(); for (final ILaunchConfiguration configuration :
	 * launchSet) { try { if (monitor.isCanceled()) { break; } final
	 * SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 0);
	 * 
	 * configuration.launch(mode, subMonitor, true, true);
	 * monitor.subTask("Launching " + configuration.getName());
	 * 
	 * 
	 * 
	 * monitor.worked(++i); } catch (CoreException core) { if (panicEnabled) {
	 * for (final ILaunch il : alreadyLaunched) { terminate(il); } } } }
	 * monitor.done();
	 * 
	 * 
	 * }
	 */

	private void terminate(ILaunch launch) {
		try {
			IProcess[] processList = launch.getProcesses();
			for (IProcess process : processList) {
				if (!process.isTerminated()) {
					process.terminate();
				}
				launch.removeProcess(process);
			}

			IDebugTarget[] debugTargetList = launch.getDebugTargets();
			for (IDebugTarget debugTarget : debugTargetList) {
				launch.removeDebugTarget(debugTarget);
			}

			launch.terminate();

		} catch (Exception ex) {
			// Exception
		}
	}

}
