/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import java.text.DateFormat;
import java.util.Calendar;

import org.eclipse.ui.PlatformUI;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.perleditor.templates.SimpleTemplateVariable;
import org.epic.perleditor.templates.TemplateContext;

/**
 * Global variables which are available in any context.
 */
public class GlobalVariables {

	/**
	 * The cursor variable determines the cursor placement after template edition.
	 */
	static class Cursor extends SimpleTemplateVariable {
		public Cursor() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.cursor"), PerlTemplateMessages.getString("GlobalVariables.variable.description.cursor")); //$NON-NLS-1$ //$NON-NLS-2$
			setEvaluationString(""); //$NON-NLS-1$
			setResolved(true);
		}
	}

	/**
	 * The dollar variable inserts an escaped dollar symbol.
	 */
	static class Dollar extends SimpleTemplateVariable {
		public Dollar() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.dollar"), PerlTemplateMessages.getString("GlobalVariables.variable.description.dollar")); //$NON-NLS-1$ //$NON-NLS-2$
			setEvaluationString("$"); //$NON-NLS-1$
			setResolved(true);
		}
	}

	/**
	 * The date variable evaluates to the current date.
	 */
	static class Date extends SimpleTemplateVariable {
		public Date() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.date"), PerlTemplateMessages.getString("GlobalVariables.variable.description.date")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return DateFormat.getDateInstance().format(new java.util.Date());
		}
	}

	/**
	 * The year variable evaluates to the current year.
	 */
	static class Year extends SimpleTemplateVariable {
		public Year() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.year"), PerlTemplateMessages.getString("GlobalVariables.variable.description.year")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
		}
	}
	/**
	 * The time variable evaluates to the current time.
	 */
	static class Time extends SimpleTemplateVariable {
		public Time() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.time"), PerlTemplateMessages.getString("GlobalVariables.variable.description.time")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return DateFormat.getTimeInstance().format(new java.util.Date());
		}
	}

	/**
	 * The user variable evaluates to the current user.
	 */
	static class User extends SimpleTemplateVariable {
		public User() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.user"), PerlTemplateMessages.getString("GlobalVariables.variable.description.user")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return System.getProperty("user.name"); //$NON-NLS-1$
		}
	}

	/**
		 * The user variable evaluates to the current user.
		 */
	static class Filename extends SimpleTemplateVariable {
		public Filename() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.filename"), PerlTemplateMessages.getString("GlobalVariables.variable.description.filename")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.getActiveEditor()
				.getTitle();
		}
	}

	/**
		* The name of the PerlInterpreter.
		*/
	static class PerlInterpreter extends SimpleTemplateVariable {
		public PerlInterpreter() {
			super(PerlTemplateMessages.getString("GlobalVariables.variable.name.perlInterpreter"), PerlTemplateMessages.getString("GlobalVariables.variable.description.perlInterpreter")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			 return (String) PerlExecutableUtilities.getPerlCommandLine().get(0);
		}
	}
}
