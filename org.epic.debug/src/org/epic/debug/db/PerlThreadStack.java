package org.epic.debug.db;

import gnu.regexp.REMatch;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * A helper class for PerlDebugThread. This class is responsible
 * for updating contents of the Variables view, that is, fetching
 * the stack trace and values of contained variables.
 * 
 * @author jploski
 */
class PerlThreadStack
{
    private final static IStackFrame[] NO_FRAMES = new IStackFrame[0];

    private final PerlDebugThread thread;
    private final RE re = new RE();
    
    private IStackFrame[] frames;
    
    public PerlThreadStack(PerlDebugThread thread)
    {
        this.thread = thread;
        this.frames = NO_FRAMES;
    }
    
    public IStackFrame[] getFrames() throws DebugException
    {
        return thread.isSuspended() ? frames : NO_FRAMES;
    }
    
    public void update() throws DebugException
    {
        try
        {
            DebuggerInterface db = thread.getDB();
            IPPosition currentIP = db.getCurrentIP();
            if (currentIP == null) return; // debugger terminated?
            
            String stackTrace = db.getStackTrace();
            REMatch[] matches = re.STACK_TRACE.getAllMatches(stackTrace);
            
            IStackFrame[] previousFrames = this.frames;
            StackFrame previousTopFrame =
                previousFrames.length > 0
                ? (StackFrame) previousFrames[0]
                : null;

            int skipFirst = 0;
            if (matches.length > 0) {
                int firstLineNumber = Integer.parseInt(matches[0].toString(4));
                if (matches[0].toString(2).startsWith("DB::DB ")
                        && firstLineNumber == currentIP.getLine()) {
                    // This is a duplicate, skip.
                    skipFirst = 1;
                }
            }
            StackFrame[] frames = new StackFrame[matches.length + 1 - skipFirst];
            frames[0] = new StackFrame(
                thread,
                currentIP.getPath(),
                currentIP.getLine(),
                thread.getEpicPath(currentIP.getPath()),
                db,
                previousTopFrame,
                0);
    
            for (int pos = skipFirst; pos < matches.length; ++pos)
            {
                IPath dbPath = new Path(matches[pos].toString(3));

                frames[pos + 1 - skipFirst] = new StackFrame(
                    thread,
                    dbPath,
                    Integer.parseInt(matches[pos].toString(4)),
                    thread.getEpicPath(dbPath),
                    db,
                    pos + 1 < previousFrames.length
                        ? (StackFrame) previousFrames[pos + 1]
                        : null,
                    pos + 1);
            }
            
            this.frames = frames;
        }
        catch (IOException e)
        {
            thread.throwDebugException(e);
        }
    }
}
