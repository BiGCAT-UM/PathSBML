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

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.sbml.peer.PeerModel;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;

public class SbmlImportHelper
{
	private PeerModel br;
	
	
	public Pathway doImport(File file) throws ConverterException
	{
		try {
			SBMLDocument doc = new SBMLReader().readSBML(file.getAbsolutePath());

			br = PeerModel.createFromDoc(doc, file);			
			return br.getPathway();
		} 
		catch (IOException ex) 
		{
			throw new ConverterException (ex);
		}
		catch (XMLStreamException ex) 
		{
			throw new ConverterException (ex);
		}
	}

	public SBMLDocument getDocument()
	{
		return br.getDoc();
	}
	
}
