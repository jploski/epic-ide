package org.epic.core.parser;

import java.util.HashSet;
import java.util.Set;

import antlr.InputBuffer;
import antlr.LexerSharedInputState;

/**
 * Base class for PerlLexer.
 * 
 * @author jploski
 */
public abstract class PerlLexerBase extends LexerBase
{   
    protected static Set KEYWORDS1;
    protected static Set KEYWORDS2;
    protected static Set OPERATORS;
    protected int pc; // current curly brace nesting level
    protected boolean qmarkRegexp;
    protected boolean slashRegexp;
    protected boolean glob;
    protected boolean afterColon;
    protected boolean afterArrow;
    protected boolean afterSub;
    protected boolean format;
    protected boolean proto;
    
    static
    {
        KEYWORDS1 = new HashSet();
        KEYWORDS2 = new HashSet();
        OPERATORS = new HashSet();
        
        initKeywords(KEYWORDS1, new String[] { 
            "BEGIN", "END", "bless", "caller", "continue", "dbmclose",
            "dbmopen", "die", "do", "dump",
            "else", "elsif", "eval", "exit", "for", "foreach", "goto",
            "if", "import", "last", "local", "my", "new", "next", "no",
            "our", "package", "redo", "ref", "require", "return", "sub",
            "tie", "tied",
            "unless", "untie", "until", "use", "wantarray", "while" });
            
        initKeywords(KEYWORDS2, new String[] {
            "__FILE__", "__LINE__", "__PACKAGE__", "abs", "accept",
            "alarm", "atan2", "bind", "binmode", "closedir",
            "for", "lcfirst", "opendir", "printf", "readdir", "readlink",
            "seekdir", "socketpair", "substr", "telldir", "tied", "times",
            "ucfirst", "waitpid", "chdir", "chmod", "chomp", "chop",
            "chown", "chr", "chroot", "close", "connect", "cos", "crypt",
            "defined", "delete", "each",
            "endgrent", "endhostent", "endnetent", "endprotoent",
            "endpwent", "endservent", "eof", "exec", "exists", "exp",
            "fcntl", "fileno", "flock", "fork", "formline", "getc",
            "getgrent", "getgrgid", "getgrnam", "gethostbyaddr",
            "gethostbyname", "gethostent", "getlogin", "getnetbyaddr",
            "getnetbyname", "getnetent", "getpeername", "getpgrp",
            "getppid", "getpriority", "getprotobyname", "getprotobynumber",
            "getprotoent", "getpwent", "getpwnam", "getpwuid",
            "getservbyname", "getservbyport", "getservent", "getsockname",
            "getsockopt", "glob", "gmtime", "grep", "hex", "index", "int",
            "ioctl", "join", "keys", "kill", "lc", "length", "link",
            "listen", "localtime", "log", "lstat", "map", "mkdir",
            "msgctl", "msgget", "msgrcv", "msgsnd", "oct", "open",
            "ord", "pack", "pipe", "pop", "pos", "print", "push",
            "quotemeta", "rand", "read", "recv", "rename", "reset",
            "reverse", "rewinddir", "rindex", "rmdir", "scalar", "seek",
            "select", "semctl", "semget", "semop", "send", "setgrent",
            "sethostent", "setnetent", "setpgrp", "setpriority",
            "setprotoent", "setpwent", "setservent", "setsockopt", "shift",
            "shmctl", "shmget", "shmread", "shmwrite", "shutdown", "sin",
            "sleep", "socket", "sort", "splice", "split", "sprintf",
            "sqrt", "srand", "stat", "study", "sub", "symlink", "syscall",
            "sysread", "sysseek", "system", "syswrite", "tell",
            "time", "truncate", "uc", "umask", "undef", "unlink",
            "unpack", "unshift", "utime", "values", "vec",
            "wait", "warn", "write" });
        
        initKeywords(OPERATORS, new String[] {
            "lt", "gt", "le", "ge", "eq", "ne", "cmp", "not",
            "and", "or", "xor", "x" });
    }
    
    protected PerlLexerBase()
    {
    }

    protected PerlLexerBase(InputBuffer cb)
    {
        super(cb);
    }

    protected PerlLexerBase(LexerSharedInputState sharedState)
    {
        super(sharedState);
    }
    
    public int getCurlyLevel()
    {
        return pc;
    }
    
    public void setInputState(LexerSharedInputState state)
    {
        super.setInputState(state);
        pc = 0;
        qmarkRegexp = slashRegexp = glob = afterArrow = afterSub = format =
            afterColon = false;
    }
    
    protected CurlyToken createCurlyToken(int type, String text)
    {
        CurlyToken t = new CurlyToken(type, text, pc);
        t.setColumn(getColumn()-1);
        t.setLine(getLine());
        t.setOffset(getParent().computeTokenOffset(t));
        return t;
    }
    
    protected OperatorToken createOperatorToken(int type, String text)
    {
        OperatorToken t = new OperatorToken(type, text);
        t.setColumn(getColumn()-text.length());
        t.setLine(getLine());
        t.setOffset(getParent().computeTokenOffset(t));
        qmarkRegexp = slashRegexp = true;
        return t;
    }
    
    protected boolean isNumber(String word)
    {
        int len = word.length();
        for (int i = 0; i < len; i++)
        {
            char c = word.charAt(i);
            if ((c < '0' || c > '9') && c != 'x' && c != 'X') return false;
        }
        return true;
    }
    
    void setCurlyLevel(int level)
    {
        this.pc = level;
    }

    private static void initKeywords(Set dest, String[] src)
    {
        for (int i = 0; i < src.length; i++) dest.add(src[i]);
    }
}
