/**
 * Copyright 2014 Fernando Rincon Martin <frm.rincon@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openeos.tools.bndplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.service.AnalyzerPlugin;

public class UnoBndPlugin implements AnalyzerPlugin {

	private static final String UNO_MODEL_CLASSES_HEADER = "Uno-Model-Classes";

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		analyzeForEntities(analyzer);
		analyzeForDictionaryBasedWindows(analyzer);
		analyzeForMenus(analyzer);
		analyzeForTypeListAdditions(analyzer);
		analyzeForForms(analyzer);
		return false;
	}

	private void analyzeForTypeListAdditions(Analyzer analyzer) {
		try {
			Collection<Clazz> classList = analyzer.getClasses("", Clazz.QUERY.IMPLEMENTS.name(),
					"org.openeos.dao.ListTypeAdditions");
			filterAbstractClasses(classList);
			analyzer.setProperty("Uno-ListTypeAdditions-Classes", Analyzer.join(classList));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void analyzeForDictionaryBasedWindows(Analyzer analyzer) {
		try {
			Collection<Clazz> classList = analyzer.getClasses("", Clazz.QUERY.IMPLEMENTS.name(),
					"org.openeos.services.ui.window.DictionaryBasedWindowDefinition");
			filterAbstractClasses(classList);
			analyzer.setProperty("Uno-Window-Classes", Analyzer.join(classList));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void analyzeForForms(Analyzer analyzer) {
		try {
			Collection<Clazz> classList = analyzer.getClasses("", Clazz.QUERY.IMPLEMENTS.name(),
					"org.openeos.services.ui.form.abstractform.AbstractFormBindingForm");
			filterAbstractClasses(classList);
			analyzer.setProperty("Uno-BindingForm-Classes", Analyzer.join(classList));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void analyzeForEntities(Analyzer analyzer) {
		try {
			String header = analyzer.getProperty(UNO_MODEL_CLASSES_HEADER);
			if (header == null || header.trim().length() == 0) {
				analyzer.setProperty(UNO_MODEL_CLASSES_HEADER,
						analyzer._classes("", Clazz.QUERY.ANNOTATED.name(), "javax.persistence.Entity"));
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void analyzeForMenus(Analyzer analyzer) {
		try {
			Collection<Clazz> classList = analyzer.getClasses("", Clazz.QUERY.IMPLEMENTS.name(),
					"org.openeos.services.ui.menu.MenuProvider");
			filterAbstractClasses(classList);
			analyzer.setProperty("Uno-Menu-Classes", Analyzer.join(classList));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private Collection<Clazz> filterAbstractClasses(Collection<Clazz> classList) {
		List<Clazz> toRemove = new ArrayList<Clazz>();
		for (Clazz clazz : classList) {
			if (clazz.isAbstract()) {
				toRemove.add(clazz);
			}
		}
		classList.removeAll(toRemove);
		return classList;
	}
}
