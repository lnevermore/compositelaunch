package com.xored.test.malkov.ui.model;

/**
 * 
 * @author malkov
 * 
 */
public class LaunchMeta {
	private boolean isEnabled;
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LaunchMeta() {
		isEnabled = false;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

}
