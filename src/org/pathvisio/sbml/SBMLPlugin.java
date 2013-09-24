// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.sbml;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.xml.rpc.ServiceException;
import javax.xml.stream.XMLStreamException;

import org.pathvisio.core.Engine;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.sbml.peer.PeerModel;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;

import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;
import uk.ac.ebi.biomodels.ws.SimpleModel;

/**
 * SBML importer and exporter
 */
public class SBMLPlugin implements Plugin {
	private PvDesktop desktop;

	private SBMLDocument lastImported = null;
	JPanel sbmlPanel;
	File tmpDir = new File(GlobalPreference.getApplicationDir(), "models-cache");
	File tmpDir2 = new File(GlobalPreference.getApplicationDir(),
			"sbml-models-cache");

	public void init(PvDesktop desktop) {
		try {
			tmpDir.mkdirs();
			tmpDir2.mkdirs();
			loadClient();
			// save the desktop reference so we can use it later
			this.desktop = desktop;

			// register importer / exporter
			SBMLFormat sbmlFormat = new SBMLFormat(this);
			desktop.getSwingEngine().getEngine().addPathwayExporter(sbmlFormat);
			desktop.getSwingEngine().getEngine().addPathwayImporter(sbmlFormat);

			validateButton = desktop.getSwingEngine().getApplicationPanel()
					.addToToolbar(validateAction);
			layoutButton = desktop.getSwingEngine().getApplicationPanel()
					.addToToolbar(layoutAction);
			biomodelButton = desktop.getSwingEngine().getApplicationPanel()
					.addToToolbar(biomdelAction);

			// add new SBML side pane
			DocumentPanel pane = new DocumentPanel(desktop.getSwingEngine());
			JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
			sidebarTabbedPane.add("SBML", pane);

			// add functionality to the pane
			desktop.getSwingEngine().getEngine()
					.addApplicationEventListener(pane);
		} catch (Exception e) {
			Logger.log.error("Error while initializing ", e);
			JOptionPane.showMessageDialog(desktop.getSwingEngine()
					.getApplicationPanel(), e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private final ValidateToolBarAction validateAction = new ValidateToolBarAction();
	private final FRLayoutAction layoutAction = new FRLayoutAction();
	private final BioModelsAction biomdelAction = new BioModelsAction();

	private Map<String, BioModelsWSClient> clients = new HashMap<String, BioModelsWSClient>();

	private JButton validateButton;

	private JButton layoutButton;

	private JButton biomodelButton;;

	/**
	 * This class adds action to the Validate button.
	 * 
	 * When the button is clicked, a dialog box is opened where an SBML file can
	 * be chosen to validate.
	 * 
	 * @author applecool
	 * 
	 */
	private class ValidateToolBarAction extends AbstractAction {

		ValidateToolBarAction() {
			putValue(NAME, "Validate");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			ValidatePanel vp = new ValidatePanel();
			JDialog d = new JDialog(desktop.getFrame(), "Validate");
			d.getContentPane().add(vp);
			d.pack();
			d.setVisible(true);

		}

	}

	/**
	 * This class adds the action to the Force Directed Layout button.
	 * 
	 * Works properly only with three data nodes. Doesn't work with process
	 * nodes. This method is added just to experiment with FR Layout algorithm.
	 * 
	 * @author applecool
	 * 
	 */
	private class FRLayoutAction extends AbstractAction {

		FRLayoutAction() {
			putValue(NAME, "ForceDirectedLayout");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			// new FruchtRein(desktop.getSwingEngine());
			new Prefuse(desktop.getSwingEngine(), false);
		}

	}

	private class BioModelsAction extends AbstractAction {

		BioModelsAction() {
			putValue(NAME, "Biomodels");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			BioModelPanel p = new BioModelPanel(SBMLPlugin.this);
			JDialog d = new JDialog(desktop.getFrame(), "Searching Biomodels",
					false);

			d.getContentPane().add(p);
			d.pack();
			d.setVisible(true);
			d.setResizable(false);
			// loading dialog at the centre of the frame
			d.setLocationRelativeTo(desktop.getSwingEngine().getFrame());
			d.setVisible(true);
		}

	}

	public static String shortClientName(String clientName) {
		Pattern pattern = Pattern.compile("http://(.*?)/");
		Matcher matcher = pattern.matcher(clientName);

		if (matcher.find()) {
			clientName = matcher.group(1);
		}

		return clientName;
	}

	public Map<String, BioModelsWSClient> getClients() {
		return clients;
	}

	public void openPathwayWithProgress(final BioModelsWSClient client,
			final String id, final int rev, final File tmpDir)
			throws InterruptedException, ExecutionException {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(
				JOptionPane.getFrameForComponent(desktop.getSwingEngine()
						.getApplicationPanel()), "", pk, false, true);

		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
			protected Boolean doInBackground() throws Exception {
				pk.setTaskName("Opening pathway");
				try {
					openPathway(client, id, rev, tmpDir);
				} catch (Exception e) {
					Logger.log.error("The Pathway is not found", e);
					JOptionPane.showMessageDialog(null,
							"The Pathway is not found", "ERROR",
							JOptionPane.ERROR_MESSAGE);
				} finally {
					pk.finished();
				}
				return true;
			}
		};

		sw.execute();
		d.setVisible(true);
		sw.get();
	}

	protected void openPathway(BioModelsWSClient client, String id, int rev,
			File tmpDir) throws ConverterException, BioModelsWSException,
			IOException {

		String p = client.getModelSBMLById(id);
		File tmp = new File(tmpDir, id + ".xml");

		BufferedWriter output = new BufferedWriter(new FileWriter(tmp));
		output.write(p.toString());
		output.close();
		SBMLDocument doc;
		try {
			doc = new SBMLReader().readSBML(tmp.getAbsolutePath());

			PeerModel br = PeerModel.createFromDoc(doc, tmp);
			Pathway pw = br.getPathway();

			File tmp2 = new File(tmpDir2, id + ".xml");
			pw.writeToXml(tmp2, true);

			Engine engine = desktop.getSwingEngine().getEngine();
			engine.setWrapper(desktop.getSwingEngine().createWrapper());
			SBMLFormat.doc = doc;
			engine.openPathway(tmp2);

		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void loadClient() throws MalformedURLException, ServiceException,
			BioModelsWSException {
		BioModelsWSClient client = new BioModelsWSClient();
		clients.put(
				"http://www.ebi.ac.uk/biomodels-main/services/BioModelsWebServices?wsdl",
				client);

	}

	public static enum PlPreference implements Preference {
		PL_LAYOUT_FR_ATTRACTION("0.5"), PL_LAYOUT_FR_REPULSION("1"), PL_LAYOUT_SPRING_FORCE(
				"0.33"), PL_LAYOUT_SPRING_REPULSION("100"), PL_LAYOUT_SPRING_STRETCH(
				"0.7");

		private final String defaultVal;

		PlPreference(String _defaultVal) {
			defaultVal = _defaultVal;
		}

		@Override
		public String getDefault() {
			return defaultVal;
		}
	}

	public void done() {
		desktop.getSideBarTabbedPane().remove(sbmlPanel);
//		// remove our action (defined below) from the toolbar
//		desktop.getSwingEngine().getApplicationPanel().removeFromToolbar(validateButton);
//		desktop.getSwingEngine().getApplicationPanel().removeFromToolbar(layoutButton);
//		desktop.getSwingEngine().getApplicationPanel().removeFromToolbar(biomodelButton);
	}

	/**
	 * This method is called in the SBMLFormat.java This method sets the
	 * imported document to the lastImported variable.
	 * 
	 * @param document
	 */
	public void setLastImported(SBMLDocument document) {
		lastImported = document;

	}

	public File getTmpDir() {
		// TODO Auto-generated method stub
		return tmpDir;
	}

}
