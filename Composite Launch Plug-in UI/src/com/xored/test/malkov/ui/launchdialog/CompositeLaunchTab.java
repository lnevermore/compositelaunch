package com.xored.test.malkov.ui.launchdialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.xored.test.malkov.ui.Activator;
import com.xored.test.malkov.ui.constants.LaunchTabUINames;
import com.xored.test.malkov.ui.model.LaunchMeta;

/**
 * @author malkov
 */

@SuppressWarnings("restriction")
public class CompositeLaunchTab extends AbstractLaunchConfigurationTab {
	
	public static final String LAUNCHES_SET_ATTRIBUTE_KEY = "launches_set_arrtibutes";
	public static final String BUILD_WITH_RUN_ATTRIBUTE = "asyncrun_checkbox_attribute";
	public static final String PANIC_RUN_ATTRIBUTE = "panic_run_checkbox_attribute";

	/**
	 * (non-javadoc)
	 * 
	 * It can be done in a form of a table, but I'd rather do it this way
	 * because it's more user-friendly than table Also enabling/disabling and
	 * launch order works clearer for user's understanding in this
	 * representation of UI
	 * 
	 */
	// maps for containing current state of a tab. Key is "..." button just for
	// comfortable using

	// associates ...-button and launchConfig
	private Map<Button, LaunchMeta> selectedLaunchesTextes;

	// associates ...-button and parent composite for comfortable disposing
	private Map<Button, Composite> parentTable;

	// associates ...-button and deletion button
	private Map<Button, Button> deletionButtonsTable;

	// checkbox for build or not before launch
	private Button buildWithRunCheckBox;

	// checkbox for termination all processes if someone fails
	private Button panicCheckBox;

	private Composite selectionComp;

	// indicates that something important were changes and can be saved
	private boolean savingEventOccurred = false;

	@Override
	public void createControl(Composite arg0) {

		Composite comp = SWTFactory.createComposite(arg0, 1, 1,
				GridData.FILL_HORIZONTAL);
		setControl(comp);

		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(getControl(), getHelpContextId());

		createRunConfigsControl(comp);

		final Group selectionGroup = SWTFactory.createGroup(comp,
				LaunchTabUINames.CompositeLaunchTab_13, 3, 2,
				GridData.FILL_HORIZONTAL);

		selectionComp = SWTFactory.createComposite(selectionGroup,
				selectionGroup.getFont(), 1, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL,
				0, 0);

		comp.setFont(arg0.getFont());

		selectedLaunchesTextes = new HashMap<>();
		parentTable = new HashMap<>();
		deletionButtonsTable = new HashMap<>();
	}

	/**
	 * creates run configuration with two checkboxes
	 * 
	 * @param comp
	 *            parent component
	 */
	private void createRunConfigsControl(final Composite comp) {
		final Group runConfigGroup = SWTFactory.createGroup(comp,
				LaunchTabUINames.CompositeLaunchTab_13, 3, 2,
				GridData.FILL_HORIZONTAL);

		final Composite runConfigComp = SWTFactory.createComposite(
				runConfigGroup, runConfigGroup.getFont(), 1, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL,
				0, 0);

		buildWithRunCheckBox = SWTFactory.createCheckButton(runConfigComp,
				LaunchTabUINames.CompositeLaunchTab_checkbox_1, null, false, 1);
		panicCheckBox = SWTFactory.createCheckButton(runConfigComp,
				LaunchTabUINames.CompositeLaunchTab_checkbox_2, null, false, 1);

		buildWithRunCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				savingEventOccurred = true;
				updateLaunchConfigurationDialog();
			}
		});

		panicCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				savingEventOccurred = true;
				updateLaunchConfigurationDialog();
			}
		});
	}

	/**
	 * creates new field for new launch selection
	 * 
	 * @param meta
	 *            container with meta info about selected launch config. if
	 *            null, creates empty container, otherwise creates container
	 *            with exists launch config
	 */
	private void addNewLaunchSelectionField(LaunchMeta meta) {

		// ui components creation

		final Composite localComp = SWTFactory.createComposite(selectionComp,
				selectionComp.getFont(), 4, 1, GridData.FILL_HORIZONTAL, 0, 0);
		final Text launchText = SWTFactory.createSingleText(localComp, 1);

		final String textPreset = meta == null ? "" //$NON-NLS-N$
				: meta.getName();

		final String enablingPreset = meta == null ? LaunchTabUINames.CompositeLaunchTab_1
				: meta.isEnabled() ? LaunchTabUINames.CompositeLaunchTab_2
						: LaunchTabUINames.CompositeLaunchTab_3;

		final Button launchSelection = createPushButton(localComp,
				LaunchTabUINames.CompositeLaunchTab_4, null);

		final Button delete = createPushButton(localComp,
				LaunchTabUINames.CompositeLaunchTab_5, null);

		final Button disable = createPushButton(localComp, enablingPreset, null);

		parentTable.put(launchSelection, localComp);

		disable.setEnabled(meta != null);
		delete.setEnabled(meta != null);
		launchText.setText(textPreset);

		final boolean isNeedToEnable = meta == null ? true : meta.isEnabled();
		launchSelection.setEnabled(isNeedToEnable);
		launchText.setEnabled(isNeedToEnable);

		// listeners
		launchSelection.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				createLaunchesSelectionDialog(launchSelection, launchText,
						disable);
			}
		});

		delete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				savingEventOccurred = true;
				parentTable.get(launchSelection).dispose();
				parentTable.remove(launchSelection);
				selectedLaunchesTextes.remove(launchSelection);
				deletionButtonsTable.remove(launchSelection);
				updateLaunchConfigurationDialog();
			}
		});

		disable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				savingEventOccurred = true;
				if (launchText.isEnabled()) {

					launchSelection.setEnabled(false);
					launchText.setEnabled(false);
					disable.setText(LaunchTabUINames.CompositeLaunchTab_6);
					selectedLaunchesTextes.get(launchSelection).setEnabled(
							false);
				} else {

					launchText.setEnabled(true);
					launchSelection.setEnabled(true);
					disable.setText(LaunchTabUINames.CompositeLaunchTab_7);

					selectedLaunchesTextes.get(launchSelection)
							.setEnabled(true);
				}

				updateLaunchConfigurationDialog();
			}
		});

		launchText.setEditable(false);

		deletionButtonsTable.put(launchSelection, delete);

		if (meta != null) {
			selectedLaunchesTextes.put(launchSelection, meta);
		}

	}

	/**
	 * creates launches selection dialog.
	 * 
	 * @param self
	 *            button "..."
	 * @param launch
	 *            text components with launch name
	 * @param disable
	 *            button for disabling launch
	 */
	protected void createLaunchesSelectionDialog(Button self, Text launch,
			Button disable) {
		final String currentContainerString = launch.getText();
		try {
			final ILaunchConfiguration[] allConfigs = getLaunchManager()
					.getLaunchConfigurations();
			final List<ILaunchConfiguration> list = new ArrayList<>();

			ILaunchConfiguration selectedConfig = null;
			if (currentContainerString != null) {

				for (final ILaunchConfiguration config : allConfigs) {

					if (!LaunchTabUINames.CompositeLaunchTab_8.equals(config
							.getType().getName())) {
						if (!alreasyExistsInSelected(config,
								currentContainerString)) {
							list.add(config);
						}

						if (currentContainerString.equals(config.getName())) {

							selectedConfig = config;
						}
					}
				}
			}

			final ElementListSelectionDialog dialog = new ElementListSelectionDialog(
					getShell(), launchesProvider);

			dialog.setTitle(LaunchTabUINames.CompositeLaunchTab_9);
			dialog.setMessage(LaunchTabUINames.CompositeLaunchTab_10);
			dialog.setElements(list.toArray());
			dialog.setInitialSelections(new Object[] { selectedConfig });
			dialog.setEmptySelectionMessage(LaunchTabUINames.CompositeLaunchTab_11);
			dialog.setMultipleSelection(false);
			dialog.open();

			final Object result = dialog.getFirstResult();

			// update ui if something were selected
			if (result != null) {
				launch.setText(((ILaunchConfiguration) result).getName());
				selectedLaunchesTextes.put(self, new LaunchMeta());
				selectedLaunchesTextes.get(self).setName(
						((ILaunchConfiguration) result).getName());
				selectedLaunchesTextes.get(self).setEnabled(true);
				disable.setEnabled(true);
				savingEventOccurred = true;

				showAnotherSeletion(currentContainerString == null
						|| LaunchTabUINames.CompositeLaunchTab_12
								.equals(currentContainerString));

			}

		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}

	}

	/**
	 * check whatever some config already exists in selected
	 * 
	 * @param config
	 *            some config
	 * @param current
	 *            current config name, which is in text
	 * @return true if this config exists in selected, false otherwise
	 */
	private boolean alreasyExistsInSelected(ILaunchConfiguration config,
			String current) {
		if (config.getName() == current) {
			return false;
		}
		for (final LaunchMeta value : selectedLaunchesTextes.values()) {
			if ((config.getName().equals(value.getName()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * sets all deletion buttons enabled and create one empty line
	 * 
	 * @param isNeedToShowNewString
	 *            if empty line were updated, shows new line, of already exists
	 *            line updated, forbid to show new lines
	 * @see CompositeLaunchTab::createLaunchesSelectionDialog
	 */
	private void showAnotherSeletion(boolean isNeedToShowNewString) {

		for (final Button deleteButton : deletionButtonsTable.values()) {
			deleteButton.setEnabled(true);
		}
		if (isNeedToShowNewString) {
			addNewLaunchSelectionField(null);
		}

		updateLaunchConfigurationDialog();
	}

	@Override
	public String getName() {
		return LaunchTabUINames.CompositeLaunchTab_13;
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		clearTabState();
		try {
			// create all existed launches
			initLaucnhesMeta(configuration);
			initCheckboxes(configuration);
			// create one more empty
			addNewLaunchSelectionField(null);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
	}

	private void initCheckboxes(ILaunchConfiguration configuration)
			throws CoreException {
		buildWithRunCheckBox.setSelection(configuration.getAttribute(
				BUILD_WITH_RUN_ATTRIBUTE, false));
		panicCheckBox.setSelection(configuration.getAttribute(
				PANIC_RUN_ATTRIBUTE, false));

	}

	@SuppressWarnings("unchecked")
	private void initLaucnhesMeta(ILaunchConfiguration configuration)
			throws CoreException {

		Map<String, String> map = configuration.getAttribute(
				LAUNCHES_SET_ATTRIBUTE_KEY,
				new HashMap<String, String>());

		for (final Map.Entry<String, String> entry : map.entrySet()) {

			LaunchMeta meta = new LaunchMeta();

			meta.setEnabled(Boolean.parseBoolean(entry.getValue()));
			meta.setName(entry.getKey());

			addNewLaunchSelectionField(meta);
		}

	}

	private void clearTabState() {
		// disposing and clear all all
		for (final Composite comp : parentTable.values()) {
			comp.dispose();
		}
		selectedLaunchesTextes.clear();
		parentTable.clear();
		deletionButtonsTable.clear();
		savingEventOccurred = false;
		panicCheckBox.setSelection(false);
		buildWithRunCheckBox.setSelection(false);

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {

		final Map<String, String> metaSetNames = new HashMap<String, String>();
		for (final LaunchMeta meta : selectedLaunchesTextes.values()) {
			metaSetNames.put(meta.getName(), String.valueOf(meta.isEnabled()));
		}
		configuration.setAttribute(
				LAUNCHES_SET_ATTRIBUTE_KEY,
				metaSetNames);
		configuration.setAttribute(
				BUILD_WITH_RUN_ATTRIBUTE,
				buildWithRunCheckBox.getSelection());
		configuration.setAttribute(
				PANIC_RUN_ATTRIBUTE,
				panicCheckBox.getSelection());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {

	}

	@Override
	public boolean canSave() {
		return savingEventOccurred;
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		final String badLaunchName = validateLaunchNames();

		if (badLaunchName != null) {
			if (badLaunchName.length() != 0) {
				setErrorMessage(String.format(
						LaunchTabUINames.CompositeLaunchTab_17, badLaunchName));
			} else {
				setErrorMessage(LaunchTabUINames.CompositeLaunchTab_18);
			}
			return false;
		}

		if (!validateExsistion()) {
			setErrorMessage(LaunchTabUINames.CompositeLaunchTab_19);
			return false;
		}

		return true;
	}

	/**
	 * validate whatever one or more launches enabled and can starts
	 * 
	 * @return true if one or more launches enabled
	 */
	private boolean validateExsistion() {
		for (final LaunchMeta meta : selectedLaunchesTextes.values()) {
			if (meta.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * validation for launch names
	 * 
	 * @return name of the first launch in launches set which doesn't exists or
	 *         null, of all launches are ok
	 */
	private String validateLaunchNames() {
		ILaunchConfiguration[] configurations;
		try {
			configurations = getLaunchManager().getLaunchConfigurations();
		} catch (CoreException e) {
			// can't get launch configs
			return ""; //$NON-NLS-N$
		}

		main: for (final LaunchMeta meta : selectedLaunchesTextes.values()) {
			for (ILaunchConfiguration config : configurations) {
				if (config.getName().equals(meta.getName())) {
					continue main;
				}
			}
			return meta.getName();
		}
		return null;
	}

	@Override
	public String getId() {
		return LaunchTabUINames.CompositeLaunchTab_16;
	}

	@Override
	public Image getImage() {
		final AbstractUIPlugin plugin = Activator.getDefault();
		final ImageRegistry reg = plugin.getImageRegistry();

		return reg.get(Activator.COMPOSITE_TAB_IMAGE_ID);
	}

	private ILabelProvider launchesProvider = new ILabelProvider() {

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public String getText(Object element) {
			return ((ILaunchConfiguration) element).getName();
		}

		@Override
		public Image getImage(Object element) {
			try {
				return DebugUITools
						.getImage(((ILaunchConfigurationType) ((ILaunchConfiguration) element)
								.getType()).getIdentifier());
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
				return null;
			}
		}
	};
}
