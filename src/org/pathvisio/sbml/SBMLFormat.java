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

import org.pathvisio.core.model.AbstractPathwayFormat;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.util.RootElementFinder;
import org.sbml.jsbml.SBMLDocument;

public class SBMLFormat extends AbstractPathwayFormat
{
	public static SBMLDocument doc;
	private final SBMLPlugin parent;
	
	/** @param parent may be null */
	public SBMLFormat (SBMLPlugin parent)
	{
		this.parent = parent;
	}
	
	public Pathway doImport(File file)
		throws ConverterException 
	{
		SbmlImportHelper helper = new SbmlImportHelper();
		Pathway result = helper.doImport(file);
		if (parent != null)
			{
			parent.setLastImported(helper.getDocument());
			//sets the doc to the current imported document.
			doc=helper.getDocument();
			}
		return result;
		
	}

	public void doExport(File file, Pathway pathway)
		throws ConverterException 
	{
		SbmlExportHelper helper = new SbmlExportHelper(pathway, file);
		helper.doExport();	
		
	}

	private static final String[] EXTENSIONS = new String[] { "sbml", "xml" };
	
	public String[] getExtensions() 
	{
		return EXTENSIONS;
	}

	public String getName() 
	{
		return "SBML (Systems Biology Markup Language)";
	}
	
	@Override
	public boolean isCorrectType(File f)
	{
		String uri;
		try
		{
			uri = "" + RootElementFinder.getRootUri(f);
			return uri.startsWith ("http://www.sbml.org");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
