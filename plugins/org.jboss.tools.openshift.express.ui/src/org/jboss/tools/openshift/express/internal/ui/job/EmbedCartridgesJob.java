/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.selector.LatestVersionOf;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgesJob extends AbstractDelegatingMonitorJob {

	private List<IEmbeddableCartridge> selectedCartridges;
	private IApplication application;
	private List<IEmbeddedCartridge> addedCartridges;

	public EmbedCartridgesJob(List<IEmbeddableCartridge> selectedCartridges, IApplication application) {
		super(NLS.bind(OpenShiftExpressUIMessages.ADDING_REMOVING_CARTRIDGES, application.getName()));
		this.selectedCartridges = selectedCartridges;
		this.application = application;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		try {
			removeEmbeddedCartridges(
					getRemovedCartridges(selectedCartridges, application.getEmbeddedCartridges()),
					application, monitor);
			this.addedCartridges = addEmbeddedCartridges(
					getAddedCartridges(selectedCartridges, application.getEmbeddedCartridges()), 
					application, monitor);
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return OpenShiftUIActivator.createErrorStatus("Could not embed cartridges for application {0}", e,
					application.getName());
		}
	}

	public List<IEmbeddedCartridge> getAddedCartridges() {
		return addedCartridges;
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToRemove,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToRemove.isEmpty()) {
			return;
		}
		Collections.sort(cartridgesToRemove, new CartridgeAddRemovePriorityComparator());
		for (IEmbeddableCartridge cartridgeToRemove : cartridgesToRemove) {
			if (monitor.isCanceled()) {
				return;
			}
			final IEmbeddedCartridge embeddedCartridge = application.getEmbeddedCartridge(cartridgeToRemove);
			if (embeddedCartridge != null) {
				embeddedCartridge.destroy();
			}
		}
	}

	private List<IEmbeddedCartridge> addEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToAdd,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToAdd.isEmpty()
				|| monitor.isCanceled()) {
			return Collections.emptyList();
		}
		Collections.sort(cartridgesToAdd, new CartridgeAddRemovePriorityComparator());
		return application.addEmbeddableCartridges(cartridgesToAdd);
	}

	private List<IEmbeddableCartridge> getAddedCartridges(List<IEmbeddableCartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<IEmbeddableCartridge> cartridgesToAdd = new ArrayList<IEmbeddableCartridge>();
		for (IEmbeddableCartridge cartridge : selectedCartridges) {
			if (!embeddedCartridges.contains(cartridge)) {
				cartridgesToAdd.add(cartridge);
			}
		}
		return cartridgesToAdd;
	}

	private List<IEmbeddableCartridge> getRemovedCartridges(List<IEmbeddableCartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<IEmbeddableCartridge> cartridgesToRemove = new ArrayList<IEmbeddableCartridge>();
		for (IEmbeddableCartridge cartridge : embeddedCartridges) {
			if (!selectedCartridges.contains(cartridge)) {
				cartridgesToRemove.add(cartridge);
			}
		}
		return cartridgesToRemove;
	}

	private class CartridgeAddRemovePriorityComparator implements Comparator<IEmbeddableCartridge> {

		@Override
		public int compare(IEmbeddableCartridge thisCartridge, IEmbeddableCartridge thatCartridge) {
			// mysql has to be added/removed before phpmyadmin
			if (thisCartridge.equals(LatestVersionOf.mySQL().matches(thisCartridge))) {
				return -1;
			} else if (LatestVersionOf.mySQL().matches(thatCartridge)) {
				return 1;
			} else if (LatestVersionOf.postgreSQL().matches(thisCartridge)) {
				return -1;
			} else if (LatestVersionOf.postgreSQL().matches(thatCartridge)) {
				return 1;
			} else if (LatestVersionOf.mongoDB().matches(thisCartridge)) {
				return -1;
			} else if (LatestVersionOf.mongoDB().matches(thatCartridge)) {
				return 1;
			}
			return 0;
		}

		
	
	}
}
