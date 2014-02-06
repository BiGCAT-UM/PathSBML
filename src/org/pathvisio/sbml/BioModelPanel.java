// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2014 BiGCaT Bioinformatics
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.gui.ProgressDialog;

import uk.ac.ebi.biomodels.ws.BioModelsWSClient;

/**
 * This class creates the search & browse panel for searching bio models and
 * content in the dialog of the Search. This class enables us to search the bio
 * models by various terms like bio model name,publication
 * title/id,person/encoder name,uniprot id, go id and taxonomy id.
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 */
@SuppressWarnings("serial")
public class BioModelPanel extends JPanel {

	private SBMLPlugin plugin;

	static Border etch = BorderFactory.createEtchedBorder();
	private JComboBox clientDropdown;
	private JTable resultTable;
	private JScrollPane resultspane;

	// biomodel browse options
	JRadioButton Browse = new JRadioButton("Browse Biomodels");
	JRadioButton All = new JRadioButton("All Models");
	JRadioButton Curated = new JRadioButton("Curated Models");
	JRadioButton NonCurated = new JRadioButton("Non-curated Models");
	JRadioButton Search = new JRadioButton("Search Biomodels");

	// biomodel search terms
	JTextField bioModelName = new JTextField();
	JTextField pubTitId = new JTextField();
	JTextField chebiId = new JTextField();
	private JTextField personName = new JTextField();
	private JTextField uniprotId = new JTextField();
	private JTextField goId = new JTextField();
	private JTextField taxonomyId = new JTextField();
	private JButton search = new JButton("search");

	/**
	 * @param plugin
	 */
	public BioModelPanel(final SBMLPlugin plugin) {

		this.setPlugin(plugin);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();








		ButtonGroup group1 = new ButtonGroup();
		group1.add(this.Browse);
		group1.add(this.Search);

		ButtonGroup group2 = new ButtonGroup();
		group2.add(this.All);
		group2.add(this.Curated);
		group2.add(this.NonCurated);



		//tooltips for browse buttons and search choice button
		this.All.setToolTipText("Browse all submitted models at Biomodels");
		this.Curated.setToolTipText("Browse all curated models at Biomodels");
		this.NonCurated
		.setToolTipText("Browse all non-curated models at Biomodels");
		this.Browse.setToolTipText("Choose to browse models in Biomodels");
		this.Search.setToolTipText("Choose to search for models in Biomodels");

		//tooltips for all the search boxes
		this.getBioModelName()
		.setToolTipText("Tip:Use Biomodel name (e.g.:'Tyson1991 - Cell Cycle 6 var')");
		this.getPubTitId().setToolTipText(
				"Tip:Use publication name(e.g.:'sbml')");
		this.getChebiId().setToolTipText("Tip:Use Chebi id (e.g.:'24996')");
		this.getPersonName()
		.setToolTipText("Tip:Use person/encoder name (e.g.:'Rainer','Nicolas')");
		this.getUniprotId()
		.setToolTipText("Tip:Use Uniprot id (e.g.:'P04637','P10113')");
		this.getGoId().setToolTipText("Tip:Use GO id (e.g.:'0006915')");
		this.getTaxonomyId().setToolTipText("Tip:Use Taxonomy id (e.g.:'9606')");

		//search biomodel action
		Action browseBioModelAction = new AbstractAction("browseBioModels") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					getResultspane().setBorder(BorderFactory.createTitledBorder(etch, "BioModels"));
					browse(e.getActionCommand());
				} catch (Exception ex) {
					JOptionPane
					.showMessageDialog(BioModelPanel.this,
							ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					Logger.log.error("Error while browsing Biomodels", ex);
				}
			}

		};

		//search biomodel action
		Action searchBioModelAction = new AbstractAction("searchBioModels") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					getResultspane().setBorder(BorderFactory.createTitledBorder(etch, "BioModels"));
					search();
				} catch (Exception ex) {
					JOptionPane
					.showMessageDialog(BioModelPanel.this,
							ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					Logger.log.error("Error while searching for Biomodels", ex);
				}
			}

		};

		//layout for the biomodel panel.
		JPanel biomodelBox = new JPanel(new GridBagLayout());
		GridBagConstraints ccf = new GridBagConstraints();

		biomodelBox.setBorder(BorderFactory.createTitledBorder(etch));

		//layout for the browse box in the biomodel panel.
		final JPanel browseOptBox = new JPanel(new GridBagLayout());
		GridBagConstraints cc1 = new GridBagConstraints();

		browseOptBox.setBorder(BorderFactory.createTitledBorder(etch,"Browse options"));

		//labels for all the search boxes.
		cc1.fill = GridBagConstraints.BOTH;
		cc1.gridx = 0;
		cc1.gridy = 2;
		cc1.weightx = 0.5;
		browseOptBox.add(this.All, cc1);
		cc1.fill = GridBagConstraints.BOTH;
		cc1.gridx = 0;
		cc1.gridy = 4;
		cc1.weightx = 0.5;
		browseOptBox.add(this.Curated, cc1);
		cc1.fill = GridBagConstraints.BOTH;
		cc1.gridx = 0;
		cc1.gridy = 6;
		cc1.weightx = 0.5;
		browseOptBox.add(this.NonCurated, cc1);

		final JPanel searchOptBox = new JPanel(new GridBagLayout());
		GridBagConstraints cc2 = new GridBagConstraints();

		searchOptBox.setBorder(BorderFactory.createTitledBorder(etch,"Search options"));

		//labels for all the search boxes.
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 0;
		searchOptBox.add(new JLabel("Biomodel Name:"), cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 1;
		searchOptBox.add(new JLabel("Publication Title/ID:"), cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 2;
		searchOptBox.add(new JLabel("Chebi ID:"),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 3;
		searchOptBox.add(new JLabel("Person Name:"),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 4;
		searchOptBox.add(new JLabel("Uniprot ID:"),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 5;
		searchOptBox.add(new JLabel("GO ID:"),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 0;
		cc2.gridy = 6;
		searchOptBox.add(new JLabel("Taxonomy ID:"),cc2);

		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 0;
		cc2.gridwidth =2;
		searchOptBox.add(getBioModelName(), cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 1;
		cc2.gridwidth =2;
		searchOptBox.add(getPubTitId(), cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 2;
		cc2.gridwidth =2;
		searchOptBox.add(getChebiId(), cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 3;
		cc2.gridwidth =2;
		searchOptBox.add(getPersonName(),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 4;
		cc2.gridwidth =2;
		searchOptBox.add(getUniprotId(),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 5;
		cc2.gridwidth =2;
		searchOptBox.add(getGoId(),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 6;
		cc2.gridwidth =2;
		searchOptBox.add(getTaxonomyId(),cc2);
		cc2.fill = GridBagConstraints.HORIZONTAL;
		cc2.gridx = 1;
		cc2.gridy = 7;
		cc2.weighty = 1.0;   //request any extra vertical space
		cc2.anchor = GridBagConstraints.PAGE_END; //bottom of space
		cc2.insets = new Insets(10,0,0,0);  //top padding
		searchOptBox.add(getSearch(),cc2);


		enableFrame(searchOptBox, false);

		//set action commands
		this.All.setActionCommand("all");
		this.Curated.setActionCommand("curated");
		this.NonCurated.setActionCommand("noncurated");

		//add action listeners
		this.Browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				enableFrame(browseOptBox, true);
				enableFrame(searchOptBox, false);
			}
		});
		this.Browse.setSelected(true);

		this.All.addActionListener(browseBioModelAction);
		this.Curated.addActionListener(browseBioModelAction);
		this.NonCurated.addActionListener(browseBioModelAction);

		this.Search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				enableFrame(searchOptBox, true);
				enableFrame(browseOptBox, false);
			}
		});

		getSearch().addActionListener(searchBioModelAction);


		Vector<String> clients = new Vector<String>(plugin.getClients().keySet());
		Collections.sort(clients);

		setClientDropdown(new JComboBox(clients));
		getClientDropdown().setSelectedIndex(0);
		getClientDropdown().setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				String strValue = SBMLPlugin.shortClientName(value.toString());
				return super.getListCellRendererComponent(list, strValue,
						index, isSelected, cellHasFocus);
			}
		});

		//		searchOptBox.add(clientDropdown, cc2.xy(6, 1));

		if (plugin.getClients().size() < 2) {
			getClientDropdown().setVisible(false);
		}

		ccf.fill = GridBagConstraints.BOTH;
		ccf.gridx = 0;
		ccf.gridy = 0;
		ccf.weightx = 0.2;
		ccf.anchor = GridBagConstraints.WEST;
		ccf.insets = new Insets(5,0,5,0);
		biomodelBox.add(this.Browse, ccf);
		ccf.fill = GridBagConstraints.BOTH;
		ccf.gridx = 0;
		ccf.gridy = 1;
		ccf.weightx = 0.2;
		biomodelBox.add(browseOptBox, ccf);
		ccf.fill = GridBagConstraints.BOTH;
		ccf.gridx = 1;
		ccf.gridy = 0;
		ccf.weightx = 0.8;
		ccf.anchor = GridBagConstraints.WEST;
		ccf.insets = new Insets(5,0,5,0);
		biomodelBox.add(this.Search, ccf);
		ccf.fill = GridBagConstraints.BOTH;
		ccf.gridx = 1;
		ccf.gridy = 1;
		ccf.weightx = 0.8;
		biomodelBox.add(searchOptBox, ccf);

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		add(biomodelBox, c);

		// Center contains table model for results
		setResultTable(new JTable());
		setResultspane(new JScrollPane(getResultTable()));
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		add(getResultspane(), c);

		//this enables us to import(open) the bio-models with two mouse clicks
		//on the results which appear in the result table.
		getResultTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//double click
				if (e.getClickCount() == 2) {
					JTable target = (JTable) e.getSource();
					int row = target.getSelectedRow();

					try {

						ResultTableModel model = (ResultTableModel) target
								.getModel();
						File tmpDir = new File(plugin.getTmpDir(), SBMLPlugin
								.shortClientName(model.clientName));
						tmpDir.mkdirs();
						plugin.openPathwayWithProgress(
								plugin.getClients().get(model.clientName),
								model.getValueAt(row, 0).toString(), 0, tmpDir);

					} catch (Exception ex) {
						JOptionPane.showMessageDialog(BioModelPanel.this,
								ex.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
						Logger.log.error("Error", ex);
					}
				}
			}
		});
	}

	/**
	 * Browse method for - searching bio-models by name,publication title/id,
	 * person/encoder name,uniprot id,go id and taxonomy id.
	 * @throws RemoteException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void browse(final String command) throws RemoteException,
	InterruptedException,
	ExecutionException {

		String clientName = getClientDropdown().getSelectedItem().toString();
		final BioModelsWSClient client = getPlugin().getClients().get(clientName);

		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(
				JOptionPane.getFrameForComponent(this), "", pk, true, true);
		final ArrayList<String> results = new ArrayList<String>();

		SwingWorker<String[], Void> sw = new SwingWorker<String[], Void>() {

			@Override
			protected String[] doInBackground() throws Exception {
				pk.setTaskName("Browse Biomodels");
				String[] results1 = null;
				String[] results2 = null;
				String[] results3 = null;

				try {
					if(command.equalsIgnoreCase("all")){

						results1 = client.getAllModelsId();

						if(results1!=null){
							for (String element : results1) {

								results.add(element);
							}
						}
					}
					if(command.equalsIgnoreCase("curated")){

						results2 = client.getAllCuratedModelsId();

						if(results2!=null){
							for (String element : results2) {

								results.add(element);
							}
						}
					}
					if(command.equalsIgnoreCase("noncurated")){

						results3 = client.getAllNonCuratedModelsId();

						if(results3!=null){
							for (String element : results3) {

								results.add(element);
							}
						}
					}

				} catch (Exception e) {
					throw e;
				} finally {
					pk.finished();
				}


				String[] finalresults = new String[results.size()];

				results.toArray(finalresults);

				return finalresults;


			}

			@Override
			protected void done() {
				if(!pk.isCancelled())
				{
					if(results.size()==0)
					{
						JOptionPane.showMessageDialog(null,"0 results found");
					}
				}
				else if(pk.isCancelled())
				{
					pk.finished();
				}
			}
		};

		sw.execute();
		d.setVisible(true);

		getResultTable().setModel(new ResultTableModel(sw.get(), clientName));
		getResultTable().setRowSorter(
				new TableRowSorter<TableModel>(getResultTable().getModel()));
	}


	/**
	 * Search method for - searching bio-models by name,publication title/id,
	 * person/encoder name,uniprot id,go id and taxonomy id.
	 * @throws RemoteException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void search() throws RemoteException, InterruptedException,
	ExecutionException {

		// search terms typed in the search boxes are trimmed and stored in the following respective variables.
		final String sbmlName = getBioModelName().getText().trim();
		final String sbmlPub = getPubTitId().getText().trim();
		final String sbmlChebi = getChebiId().getText().trim();
		final String sbmlPerson = this.getPersonName().getText().trim();
		final String sbmlUniprot = this.getUniprotId().getText().trim();
		final String sbmlGo = this.getGoId().getText().trim();
		final String sbmlTaxonomy = this.getTaxonomyId().getText().trim();

		if (!(sbmlPub.isEmpty()&&sbmlName.isEmpty()&&sbmlChebi.isEmpty()&&sbmlPerson.isEmpty()&&sbmlUniprot.isEmpty()&&sbmlGo.isEmpty()&&sbmlTaxonomy.isEmpty())) {
			String clientName = getClientDropdown().getSelectedItem().toString();
			final BioModelsWSClient client = getPlugin().getClients().get(clientName);

			final ProgressKeeper pk = new ProgressKeeper();
			final ProgressDialog d = new ProgressDialog(
					JOptionPane.getFrameForComponent(this), "", pk, true, true);
			final ArrayList<String> results = new ArrayList<String>();

			SwingWorker<String[], Void> sw = new SwingWorker<String[], Void>() {

				@Override
				protected String[] doInBackground() throws Exception {
					pk.setTaskName("Searching Biomodels");
					String[] results1 = null;
					String[] results2 = null;
					String[] results3 = null;
					String[] results4 = null;
					String[] results5 = null;
					String[] results6 = null;
					String[] results7 = null;
					try {
						// getting the models id by name.
						if (!getBioModelName().getText().equalsIgnoreCase(""))
						{
							results1 = client.getModelsIdByName(sbmlName);

							if(results1!=null){
								for (String element : results1) {

									results.add(element);
								}
							}
						}

						//getting the models id by publication title or id.
						if (!getPubTitId().getText().equalsIgnoreCase(""))
						{
							results2= client.getModelsIdByPublication(sbmlPub);
							if(results2!=null){
								for (String element : results2) {

									results.add(element);
								}
							}
						}

						//getting models id by chebi id.
						if (!getChebiId().getText().equalsIgnoreCase(""))
						{
							results3= client.getModelsIdByChEBIId(sbmlChebi);
							if(results3!=null){
								for (String element : results3) {

									results.add(element);
								}
							}
						}

						//getting models id by person or encoder or author name.
						if(!getPersonName().getText().equalsIgnoreCase(""))
						{
							results4= client.getModelsIdByPerson(sbmlPerson);
							if(results4!=null){
								for (String element : results4) {

									results.add(element);
								}
							}
						}

						//getting models id by uniprot id.
						if(!getUniprotId().getText().equalsIgnoreCase(""))
						{
							results5= client.getModelsIdByUniprot(sbmlUniprot);
							if(results5!=null){
								for (String element : results5) {

									results.add(element);
								}
							}
						}

						//getting models id by go id.
						if(!getGoId().getText().equalsIgnoreCase(""))
						{
							results6 = client.getModelsIdByGOId(sbmlGo);

							if(results6!=null){
								for (String element : results6) {

									results.add(element);
								}
							}
						}

						//getting models id by taxonomy id.
						if(!getTaxonomyId().getText().equalsIgnoreCase(""))
						{
							results7 = client.getModelsIdByTaxonomyId(sbmlTaxonomy);

							if(results7!=null){
								for (String element : results7) {

									results.add(element);
								}
							}
						}

					} catch (Exception e) {
						throw e;
					} finally {
						pk.finished();
					}


					String[] finalresults = new String[results.size()];

					results.toArray(finalresults);

					return finalresults;


				}

				@Override
				protected void done() {
					if(!pk.isCancelled())
					{
						if(results.size()==0)
						{
							JOptionPane.showMessageDialog(null,"0 results found");
						}
					}
					else if(pk.isCancelled())
					{
						pk.finished();
					}
				}
			};

			sw.execute();
			d.setVisible(true);

			getResultTable().setModel(
					new ResultTableModel(sw.get(), clientName));
			getResultTable()
			.setRowSorter(
					new TableRowSorter<TableModel>(getResultTable()
							.getModel()));
		} else {
			JOptionPane.showMessageDialog(null, "Please Enter a Search Query",
					"ERROR", JOptionPane.ERROR_MESSAGE);
		}
	}

	//enable search
	void enableFrame(JPanel container, boolean enable) {
		Component[] components = container.getComponents();
		for (Component component : components) {
			component.setEnabled(enable);
			if (component instanceof JPanel) {
				enableFrame((JPanel)component, enable);
			}
		}
	}
	/**
	 * @return the resultspane
	 */
	public JScrollPane getResultspane() {
		return this.resultspane;
	}

	/**
	 * @param resultspane the resultspane to set
	 */
	public void setResultspane(JScrollPane resultspane) {
		this.resultspane = resultspane;
	}
	/**
	 * @return the clientDropdown
	 */
	public JComboBox getClientDropdown() {
		return this.clientDropdown;
	}

	/**
	 * @param clientDropdown the clientDropdown to set
	 */
	public void setClientDropdown(JComboBox clientDropdown) {
		this.clientDropdown = clientDropdown;
	}
	/**
	 * @return the plugin
	 */
	public SBMLPlugin getPlugin() {
		return this.plugin;
	}

	/**
	 * @param plugin the plugin to set
	 */
	public void setPlugin(SBMLPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * @return the bioModelName
	 */
	public JTextField getBioModelName() {
		return this.bioModelName;
	}

	/**
	 * @param bioModelName
	 *            the bioModelName to set
	 */
	public void setBioModelName(JTextField bioModelName) {
		this.bioModelName = bioModelName;
	}

	/**
	 * @return the pubTitId
	 */
	public JTextField getPubTitId() {
		return this.pubTitId;
	}

	/**
	 * @param pubTitId
	 *            the pubTitId to set
	 */
	public void setPubTitId(JTextField pubTitId) {
		this.pubTitId = pubTitId;
	}

	/**
	 * @return the chebiId
	 */
	public JTextField getChebiId() {
		return this.chebiId;
	}

	/**
	 * @param chebiId
	 *            the chebiId to set
	 */
	public void setChebiId(JTextField chebiId) {
		this.chebiId = chebiId;
	}

	/**
	 * @return the uniprotId
	 */
	public JTextField getUniprotId() {
		return this.uniprotId;
	}

	/**
	 * @param uniprotId the uniprotId to set
	 */
	public void setUniprotId(JTextField uniprotId) {
		this.uniprotId = uniprotId;
	}

	/**
	 * @return the personName
	 */
	public JTextField getPersonName() {
		return this.personName;
	}

	/**
	 * @param personName the personName to set
	 */
	public void setPersonName(JTextField personName) {
		this.personName = personName;
	}

	/**
	 * @return the goId
	 */
	public JTextField getGoId() {
		return this.goId;
	}

	/**
	 * @param goId the goId to set
	 */
	public void setGoId(JTextField goId) {
		this.goId = goId;
	}

	/**
	 * @return the taxonomyId
	 */
	public JTextField getTaxonomyId() {
		return this.taxonomyId;
	}

	/**
	 * @param taxonomyId the taxonomyId to set
	 */
	public void setTaxonomyId(JTextField taxonomyId) {
		this.taxonomyId = taxonomyId;
	}

	/**
	 * @return the search
	 */
	public JButton getSearch() {
		return this.search;
	}

	/**
	 * @param search the search to set
	 */
	public void setSearch(JButton search) {
		this.search = search;
	}

	/**
	 * @return the resultTable
	 */
	public JTable getResultTable() {
		return this.resultTable;
	}

	/**
	 * @param resultTable
	 *            the resultTable to set
	 */
	public void setResultTable(JTable resultTable) {
		this.resultTable = resultTable;
	}

	/**
	 * This class creates the result table model based on the results.
	 * 
	 * @author applecool
	 * 
	 */
	private class ResultTableModel extends AbstractTableModel {
		String[] results;
		String[] columnNames = new String[] { "Name" };
		String clientName;

		public ResultTableModel(String[] results, String clientName) {
			this.clientName = clientName;
			this.results = results;

		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public int getRowCount() {
			return this.results.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String r = this.results[rowIndex];

			return r;
		}

		@Override
		public String getColumnName(int column) {
			return this.columnNames[column];
		}
	}

}
