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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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

/**
 * SBML importer and exporter
 */
public class SBMLPlugin implements Plugin {
	private class BioModelsAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		FRLayoutAction() {
			putValue(NAME, "ForceDirectedLayout");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// new FruchtRein(desktop.getSwingEngine());
			new Prefuse(desktop.getSwingEngine(), false);
		}

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

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		ValidateToolBarAction() {
			putValue(NAME, "Validate");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ValidatePanel vp = new ValidatePanel();
			JDialog d = new JDialog(desktop.getFrame(), "Validate");
			d.getContentPane().add(vp);
			d.pack();
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

	private PvDesktop desktop;
	private JMenu sbmlmenu;
	private JMenuItem biomodels;
	private JMenuItem layout;

	private JMenuItem validate;
	private SBMLDocument lastImported = null;
	Component sbmlPanel;

	private File tmpDir = new File(GlobalPreference.getPluginDir(), "models-cache");

	private final ValidateToolBarAction validateAction = new ValidateToolBarAction();

	private final FRLayoutAction layoutAction = new FRLayoutAction();

	private final BioModelsAction biomodelAction = new BioModelsAction();

	private Map<String, BioModelsWSClient> clients = new HashMap<String, BioModelsWSClient>();

	public void createSbmlMenu() {

		sbmlmenu = new JMenu("SBML Plugin");

		biomodels = new JMenuItem("Biomodel Import");
		layout = new JMenuItem("Force Directed Layout");
		validate = new JMenuItem("Validate Model");

		biomodels.addActionListener(biomodelAction);
		layout.addActionListener(layoutAction);
		validate.addActionListener(validateAction);

		sbmlmenu.add(biomodels);
		sbmlmenu.add(layout);
		sbmlmenu.add(validate);

		desktop.registerSubMenu("Plugins", sbmlmenu);
	}

	@Override
	public void done() {
		desktop.getSideBarTabbedPane().remove(sbmlPanel);
		
		desktop.unregisterSubMenu("Plugins", sbmlmenu);
        if(tmpDir.exists()) {
                tmpDir.delete();
        }
	}

	public Map<String, BioModelsWSClient> getClients() {
		return clients;
	}

	public File getTmpDir() {
		return tmpDir;
	}

	@Override
	public void init(PvDesktop desktop) {
		try {
			tmpDir.mkdirs();
			loadClient();
			
			// save the desktop reference so we can use it later
			this.desktop = desktop;

			// register importer / exporter
			SBMLFormat sbmlFormat = new SBMLFormat(this);
			desktop.getSwingEngine().getEngine().addPathwayExporter(sbmlFormat);
			desktop.getSwingEngine().getEngine().addPathwayImporter(sbmlFormat);

			// register menu items
			createSbmlMenu();
			
			// add new SBML side pane
			DocumentPanel pane = new DocumentPanel(desktop.getSwingEngine());
			JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
			sbmlPanel = sidebarTabbedPane.add("SBML", pane);

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

	private void loadClient() throws MalformedURLException, ServiceException,
			BioModelsWSException {
		BioModelsWSClient client = new BioModelsWSClient();
		clients.put(
				"http://www.ebi.ac.uk/biomodels-main/services/BioModelsWebServices?wsdl",
				client);

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

			File tmp2 = new File(tmpDir, id + ".xml");
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

	public void openPathwayWithProgress(final BioModelsWSClient client,
			final String id, final int rev, final File tmpDir)
			throws InterruptedException, ExecutionException {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(
				JOptionPane.getFrameForComponent(desktop.getSwingEngine()
						.getApplicationPanel()), "", pk, false, true);

		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				pk.setTaskName("Opening Model");
				try {
					openPathway(client, id, rev, tmpDir);

				} catch (Exception e) {
					Logger.log.error("The Model is not found", e);
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

	/**
	 * This method is called in the SBMLFormat.java This method sets the
	 * imported document to the lastImported variable.
	 * 
	 * @param document
	 */
	public void setLastImported(SBMLDocument document) {
		lastImported = document;

	}

}
