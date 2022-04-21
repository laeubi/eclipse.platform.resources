/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.utils;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 */
@Component(service = DebugOptionsListener.class, property = DebugOptions.LISTENER_SYMBOLICNAME + "="
		+ ResourcesPlugin.PI_RESOURCES)
public class ResourcesPluginDebugOptionsListener implements DebugOptionsListener {

	// The workspace is not used here, but we don't want to be activated before it
	// is started...
	@Reference
	IWorkspace workspace;

	@Override
	public void optionsChanged(DebugOptions options) {
		Policy.DEBUG_TRACE = options.newDebugTrace(ResourcesPlugin.PI_RESOURCES);
		Policy.DEBUG = options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/debug", false); //$NON-NLS-1$

		Policy.DEBUG_AUTO_REFRESH = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/refresh", false); //$NON-NLS-1$

		Policy.DEBUG_BUILD_DELTA = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/delta", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_CYCLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/cycle", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_FAILURE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/failure", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_INTERRUPT = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/interrupt", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_INVOKING = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/invoking", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuild", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED_DELTA = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuilddelta", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED_STACK = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuildstack", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_STACK = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/stacktrace", false); //$NON-NLS-1$

		Policy.DEBUG_CONTENT_TYPE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/contenttype", false); //$NON-NLS-1$
		Policy.DEBUG_CONTENT_TYPE_CACHE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/contenttype/cache", false); //$NON-NLS-1$
		Policy.DEBUG_HISTORY = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/history", false); //$NON-NLS-1$
		Policy.DEBUG_NATURES = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/natures", false); //$NON-NLS-1$
		Policy.DEBUG_NOTIFICATIONS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/notifications", false); //$NON-NLS-1$
		Policy.DEBUG_PREFERENCES = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/preferences", false); //$NON-NLS-1$

		Policy.DEBUG_RESTORE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_MARKERS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/markers", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_MASTERTABLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/mastertable", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_METAINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/metainfo", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_SNAPSHOTS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/snapshots", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_SYNCINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/syncinfo", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_TREE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/tree", false); //$NON-NLS-1$

		Policy.DEBUG_SAVE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_MARKERS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/markers", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_MASTERTABLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/mastertable", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_METAINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/metainfo", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_SYNCINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/syncinfo", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_TREE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/tree", false); //$NON-NLS-1$

		Policy.DEBUG_STRINGS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/strings", false); //$NON-NLS-1$
	}
}