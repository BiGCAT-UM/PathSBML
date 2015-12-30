// PathSBML Plugin
// SBML Plugin for PathVisio.
// Copyright 2013 developed for Google Summer of Code
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.pathvisio.core.Engine;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayIO;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.util.Utils;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.gui.ProgressDialog;
import org.sbml.jsbml.SBMLDocument;

import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;

/**
 * SBML importer and exporter
 * 
 * @author applecool
 * @author anwesha
 * 
 */
public class SBMLPlugin implements Plugin {
	private SBMLFormat sbmlformat;
	private PvDesktop desktop;
	JFileChooser modelChooser;
	private JMenu sbmlmenu;
	private JMenuItem sbmlImport;
	private JMenuItem sbmlExport;
	private JMenuItem biomodels;
	private JMenuItem layout;
	private JMenuItem validate;
	Component sbmlPanel;

//	final Set<PathwayIO> MODEL_FORMAT_ONLY = Utils
//			.setOf((PathwayIO) new SBMLFormat(this));

	final File tmpDir = new File(GlobalPreference.getPluginDir(),
			"models-cache");

	private final ValidateAction validateAction = new ValidateAction();

	private final FRLayoutAction layoutAction = new FRLayoutAction();

	private final ImportModelAction importmodelAction = new ImportModelAction();

	private final ExportModelAction exportmodelAction = new
	 ExportModelAction();

	private final BioModelsAction biomodelAction = new BioModelsAction();

	final Map<String, BioModelsWSClient> clients = new HashMap<String, BioModelsWSClient>();
	private String extension = ".xml";

	@SuppressWarnings("serial")
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
			d.setLocationRelativeTo(desktop.getFrame());
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
			putValue(NAME, "Force Directed Layout");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Prefuse(desktop.getSwingEngine(), false);
		}

	}

	/**
	 * 
	 *
	 */
	public static enum PlPreference implements Preference {
		/**
		 * 
		 */
		PL_LAYOUT_FR_ATTRACTION("0.5"), /**
		 * 
		 */
		PL_LAYOUT_FR_REPULSION("1"), /**
		 * 
		 */
		PL_LAYOUT_SPRING_FORCE(
				"0.33"), /**
				 * 
				 */
				PL_LAYOUT_SPRING_REPULSION("100"),
				/**
				 * 
				 */
				PL_LAYOUT_SPRING_STRETCH(
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
	@SuppressWarnings("serial")
	private class ValidateAction extends AbstractAction {

		ValidateAction() {
			putValue(NAME, "Validate");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {

			ValidatePanel vp = new ValidatePanel(SBMLPlugin.this);
			JDialog d = new JDialog(desktop.getFrame(), "Validate");
			d.getContentPane().add(vp);
			d.pack();
			d.setLocationRelativeTo(desktop.getFrame());
			d.setVisible(true);

		}

	}

	@SuppressWarnings("serial")
	private class ImportModelAction extends AbstractAction {

		ImportModelAction() {
			putValue(NAME, "Import");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			modelChooser = new JFileChooser(
					GlobalPreference.DIR_LAST_USED_IMPORT.toString());
			modelChooser.setVisible(true);
			// filtering the files based on their extensions.
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"SBML(Systems Biology Markup Language) (.sbml,.xml)",
					"sbml", "xml");
			modelChooser.setFileFilter(filter);
			int returnVal = modelChooser.showOpenDialog(desktop
					.getSwingEngine().getApplicationPanel());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = modelChooser.getSelectedFile();
				desktop.getSwingEngine().importPathway(file);
			}
		}
	}

	@SuppressWarnings("serial")
	private class ExportModelAction extends AbstractAction {

		ExportModelAction() {
			putValue(NAME, "Export");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			modelChooser = new JFileChooser(
					GlobalPreference.DIR_LAST_USED_IMPORT.toString());
			modelChooser.setVisible(true);
			// filtering the files based on their extensions.
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"SBML(Systems Biology Markup Language) (.sbml,.xml)",
					"sbml", "xml");
			modelChooser.setFileFilter(filter);
			int returnVal = modelChooser.showOpenDialog(desktop
					.getSwingEngine().getApplicationPanel());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = modelChooser.getSelectedFile();
				desktop.getSwingEngine().exportPathway(file);
			}
		}
	}
	protected static String shortClientName(String clientName) {
		Pattern pattern = Pattern.compile("http://(.*?)/");
		String clientName2 = clientName;
		Matcher matcher = pattern.matcher(clientName2);

		if (matcher.find()) {
			clientName2 = matcher.group(1);
		}

		return clientName2;
	}



	/**
	 * @author anwesha
	 */
	public void createSbmlMenu() {
		sbmlmenu = new JMenu("SBML Plugin");

		// egModel = new JMenuItem("Open Example Model");
		sbmlImport = new JMenuItem("Import local model file");
		biomodels = new JMenuItem("Import model directly from BioModels");
		layout = new JMenuItem("Apply force directed layout");
		validate = new JMenuItem("Validate selected model");
		sbmlExport= new JMenuItem("Export pathway as SBML model");



		sbmlImport.addActionListener(importmodelAction);
		biomodels.addActionListener(biomodelAction);
		layout.addActionListener(layoutAction);
		validate.addActionListener(validateAction);
		sbmlExport.addActionListener(exportmodelAction);

		// sbmlmenu.add(egModel);
		sbmlmenu.add(sbmlImport);
		sbmlmenu.add(biomodels);
		sbmlmenu.add(layout);
		sbmlmenu.add(validate);
		sbmlmenu.add(sbmlExport);

		desktop.registerSubMenu("Plugins", sbmlmenu);
	}


	protected Map<String, BioModelsWSClient> getClients() {
		return clients;
	}

	protected File getTmpDir() {
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
			sbmlformat = new SBMLFormat(this);
			desktop.getSwingEngine().getEngine().addPathwayExporter(sbmlformat);
			desktop.getSwingEngine().getEngine().addPathwayImporter(sbmlformat);

			// register menu items
			createSbmlMenu();

			// add new SBML side pane
			DocumentPanel pane = new DocumentPanel(desktop.getSwingEngine()
					.getEngine());
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

	private void loadClient() {
		BioModelsWSClient client = new BioModelsWSClient();
		clients.put(
				"http://www.ebi.ac.uk/biomodels-main/services/BioModelsWebServices?wsdl",
				client);

	}

	private void openPathway(BioModelsWSClient client, String id, File tmpDir)
			throws
			BioModelsWSException,
			IOException {
		String p = client.getModelSBMLById(id);
		File tmp = new File(tmpDir, id + extension);

		BufferedWriter output = new BufferedWriter(new FileWriter(tmp));
		output.write(p.toString());
		output.close();
		try {

			Pathway pw = sbmlformat.doImport(tmp);

			File tmp2 = new File(tmpDir, id + extension );
			pw.writeToXml(tmp2, true);

			Engine engine = desktop.getSwingEngine().getEngine();
			engine.setWrapper(desktop.getSwingEngine().createWrapper());
			engine.openPathway(tmp2);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"The model file can't be imported!",
					"Unsupported model file",
					JOptionPane.ERROR_MESSAGE);
		}

	}

	/**
	 * @param client
	 * @param id
	 * @param rev
	 * @param tmpDir
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
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
					openPathway(client, id, tmpDir);

				} catch (Exception e) {
					Logger.log.error("The Model is not found", e);
					JOptionPane.showMessageDialog(null,
							"The Model is not found", "ERROR",
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

//	 /**
//	 * This method is called in the SBMLFormat.java This method sets the
//	 * imported document to the lastImported variable.
//	 *
//	 * @param sbmlDocument
//	 */
//	 public void setLastImported(SBMLDocument sbmlDocument) {
//	 last_model_imported = sbmlDocument;
//	 }



	@Override
	public void done() {
		/*
		 * Remove sidebar
		 */
		desktop.getSideBarTabbedPane().remove(sbmlPanel);
		/*
		 * Unregister menu
		 */
		desktop.unregisterSubMenu("Plugins", sbmlmenu);
		/*
		 * Delete cache even if not empty and contains non-empty sub folders
		 */
		if(tmpDir.exists()) {
			for (File file : tmpDir.listFiles()) {
				if (file.isDirectory()) {
					for (File file2 : file.listFiles()) {
						file2.delete();
					}
				}
				file.delete();
			}
			tmpDir.delete();
		}
	}
}

