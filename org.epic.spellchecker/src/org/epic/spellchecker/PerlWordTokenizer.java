package org.epic.spellchecker;

import com.bdaum.SpellChecker.AbstractDocumentWordTokenizer;
import com.bdaum.SpellChecker.SpellCheckConfiguration;

public class PerlWordTokenizer extends AbstractDocumentWordTokenizer {

	// Perl-specific
	private static final int CODE = 0;
	private static final int OPENPOD = 1;
	private static final int LITERAL = 2;
	private static final int LINECOMMENT = 3;
	private static final int POD = 4;
	private static final int CLOSEPOD = 5;
	private static final int ESCAPE = 6;
	private static final int NEWLINE = 7;

	private int state = CODE;

	// JUST FOR TESTING
	//private static String[] states = { "CODE", "OPENPOD", "LITERAL", "LINECOMMENT", "POD", "CLOSEDOC", "ESCAPE", "NEWLINE" };

	/** Configuration **/
	private SpellCheckConfiguration config = new SpellCheckConfiguration();
	private boolean checkLiterals;
	private boolean checkPod;
	private boolean checkComments;
	private boolean ignoreCompounds;
	
	/** Lookup for Java keywords **/
		private static final String[] keywords =   // neu mit Version 1.1
			new String[] {
				"chm",
				"chomp",
				"chop",
				"chown",
				"chr",
				"chroot",
				"close",
				"closedir",
				"connect",
				"cos",
				"crypt",
				"dbmclose",
				"dbmopen",
				"defined",
				"delete",
				"die",
				"do",
				"dump",
				"each",
				"eof",
				"eval",
				"exec",
				"exists",
				"exit",
				"exp",
				"fcntl",
				"fileno",
				"flock",
				"fork",
				"format",
				"formline",
				"getc",
				"getgrent",
				"getgrgid",
				"getgrnam",
				"gethostbyaddr",
				"gethostbyname",
				"gethostent",
				"getlogin",
				"getnetbyaddr",
				"getnetbyname",
				"getnetent",
				"getpeername",
				"getpgrp",
				"getppid",
				"getpriority",
				"getprotobyname",
				"getprotobynumber",
				"getprotoent",
				"getpwnam",
				"getpwuid",
				"getservbyname",
				"getservbyport",
				"getservent",
				"getsockname",
				"getsockopt",
				"glob",
				"gmtime",
				"grep",
				"hex",
				"import",
				"index",
				"int",
				"ioctl",
				"join",
				"keys",
				"kill",
				"last",
				"lc",
				"lcfirst",
				"length",
				"link",
				"listen",
				"local",
				"localtime",
				"lock",
				"log",
				"map",
				"mkdir",
				"msgctl",
				"msgget",
				"msgrcv",
				"msgsnd",
				"my",
				"new",
				"next",
				"no",
				"oct",
				"open",
				"opendir",
				"ord",
				"our",
				"pack",
				"package",
				"pipe",
				"pop",
				"pos",
				"print",
				"printf",
				"prototype",
				"push",
				"quotemeta",
				"rand",
				"read",
				"readdir",
				"readline",
				"readlink",
				"readpipe",
				"recv",
				"redo",
				"ref",
				"rename",
				"require",
				"reset",
				"return",
				"reverse",
				"rewinddir",
				"rindex",
				"rmdir",
				"scalar",
				"seek",
				"seekdir",
				"select",
				"semctl",
				"semget",
				"semop",
				"send",
				"setpgrp",
				"setpriority",
				"setsockopt",
				"shift",
				"shmctl",
				"shmget",
				"shmread",
				"shmwrite",
				"shutdown",
				"sin",
				"sleep",
				"socket",
				"socketpair",
				"sort",
				"splice",
				"sprintf",
				"sqrt",
				"srand",
				"stat",
				"study",
				"substr",
				"symlink",
				"syscall",
				"sysopen",
				"sysread",
				"sysseek",
				"system",
				"syswrite",
				"tell",
				"telldir",
				"tie",
				"time",
				"times",
				"tr",
				"truncate",
				"uc",
				"ucfirst",
				"umask",
				"undef",
				"unlink",
				"unpack",
				"unshift",
				"untie",
				"use",
				"utime",
				"values",
				"vec",
				"wantarray",
				"warn",
				"write"

			};

	/**
	 * @see com.bdaum.SpellChecker.AbstractDocumentWordTokenizer
	 *                      #init(org.eclipse.jface.text.IDocument, 
	 *                            org.eclipse.swt.widgets.Display)
	 */
	protected void configure() {
		// Benutzeroptionen holen
		checkLiterals =
			config.getBoolean(PerlSpellCheckerPreferences.CHECKSTRINGLITERALS);
		checkPod = config.getBoolean(PerlSpellCheckerPreferences.CHECKPOD);
		checkComments =
			config.getBoolean(PerlSpellCheckerPreferences.CHECKCOMMENTS);
		ignoreCompounds =
			config.getBoolean(PerlSpellCheckerPreferences.IGNORECOMPOUNDS);
	}

	/**
	 * Checks the condition of opereation
	 * @see com.bdaum.SpellChecker.AbstractDocumentWordTokenizer
	 *                                           #isToBeChecked()
	 */
	protected boolean isToBeChecked() {
		if ((state == LITERAL) && checkLiterals)
			return true;
		if ((state == LINECOMMENT) && checkComments)
			return true;
		if (state == POD && checkPod)
			return true;
		return false;
	}

	/**
	 * @see com.bdaum.SpellChecker.AbstractDocumentWordTokenizer
	 *                            #isToBeChecked(java.lang.String)
	 */
	protected boolean isToBeChecked(String word) {
		if (ignoreCompounds) {
			if (word.indexOf("->") >= 0)
				return false;
			if (word.indexOf("::") >= 0)
				return false;
		}
		if (state == CODE) { // neu seit V1.1
			String s = word.toString().intern();
			for (int i = 0; i < keywords.length; i++)
				if (s == keywords[i])
					return false;
		}

		return true;
	}

	protected void parseCharacter(char ch) { // ge?ndert seit V1.1
	}
	
	protected char parseAndTranslateCharacter(char ch) {
		// JUST FOR TESTING
		//System.out.println("STATE: " + states[state] + "[" + ch + "]");

		switch (state) {
			case CODE :
				switch (ch) {
					case '#' :
						state = LINECOMMENT;
						break;
					case '"' :
						state = LITERAL;
						break;
					case '\n' :
					case '\r' :
						state = NEWLINE;
						break;
				}
				break;
			case NEWLINE :
				switch (ch) {
					case '=' :
						state = POD;
						break;
					case '\n' :
					case '\r' :
						state = NEWLINE;
						break;
					case '#' :
						state = LINECOMMENT;
						break;
					case '"' :
						state = LITERAL;
						break;
					default :
						state = CODE;
				}
				break;
			case LITERAL :
				switch (ch) {
					case '\\' :
						state = ESCAPE;
						break;
					case '"' :
						state = CODE;
						break;
					case '\n' :
					case '\r' :
						state = NEWLINE;
						break;
				}
				break;
			case LINECOMMENT :
				switch (ch) {
					case '\n' :
					case '\r' :
						state = NEWLINE;
						break;
				}
				break;
			case POD :
				switch (ch) {
					case '\n' :
					case '\r' :
						state = CLOSEPOD;
						break;
					default :
						state = POD;
						break;
				}
				break;
			case CLOSEPOD :
				switch (ch) {
					case '=' :
						state = CODE;
						break;
					case '\n' :
					case '\r' :
						state = CLOSEPOD;
						break;
					default :
						state = POD;
						break;
				}
				break;
			case ESCAPE :
				state = LITERAL;
				switch (ch) {
					case 'n' :
						return '\n';
					case 'r' :
						return '\r';
					case 't' :
						return '\t';
					case '\\' :
						return '\\';
					case ' ' :
						return ' ';
					case '\'' :
						return '\'';
					case '"' :
						return '"';
				}
				break;
			default :
				break;
		}
		return ch;
	}
}
