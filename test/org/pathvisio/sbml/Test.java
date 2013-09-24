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

import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;

import junit.framework.TestCase;

public class Test extends TestCase 
{
	public void testImport() throws ConverterException
	{
		File fExample = new File ("SBML/testdata/layout-extension/example1.xml");
		
		assertTrue (fExample.exists());
		
		SBMLFormat format = new SBMLFormat(null);
		
		Pathway pathway = format.doImport(fExample);
		//TODO: add assertions to check that it really worked
	}
}
