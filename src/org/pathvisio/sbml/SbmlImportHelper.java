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

import java.io.File;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.sbml.peer.PeerModel;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;

public class SbmlImportHelper {
	private PeerModel br;
	SBMLDocument doc;


	public Pathway doImport(File file) {
		try {
			doc = new SBMLReader()
			.readSBML(file.getAbsolutePath());

			br = PeerModel.createFromDoc(doc);
		}
		catch (Exception ex) {
			System.out.println("WARNING :" + doc.getModel().getName()
					+ "could not be converted");
		}
		return br.getPathway();
	}

	public SBMLDocument getDocument() {
		return br.getDoc();
	}

}