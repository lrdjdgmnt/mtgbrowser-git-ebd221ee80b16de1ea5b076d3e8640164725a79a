package com.reflexit.magiccards_rcp;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "com.reflexit.magiccards_rcp";
	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void log(String message) {
		Activator pl = getDefault();
		if (pl == null) {
			System.err.println("Log: " + message);
		} else
			pl.getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), message));
	}

	public static void log(Throwable e) {
		Activator pl = getDefault();
		if (pl == null) {
			e.printStackTrace();
		} else
			pl.getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), 1, e.getMessage(), e));
	}

	public static void log(Status s) {
		Activator pl = getDefault();
		if (pl == null) {
			System.err.println("Log: " + s.getMessage());
		} else
			pl.getLog().log(s);
	}
}
