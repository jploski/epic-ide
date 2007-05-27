# This package provides a reliable replacement for the unreliable break-on-load
# functionality of the Perl debugger. Basically, breakpoints can only inserted into
# or removed from files that have been already loaded by the Perl debugger.
# The debugger refers to such files using paths, some of which may be relative,
# and some of which may be absolute but non-unique nevertheless (due to symlinks).
# As we cannot reliably guess these paths from the Java side, we choose to
# monitor the debugger's entering of source files on the Perl side and do the
# path comparisons here.

package epic_breakpoints;

use strict;
use warnings;

use Cwd 'abs_path';
use Data::Dumper;

# pending_breaks maps the canonical path of each source file with pending
# breakpoints to an array of hash refs with keys { line, cond, add }
# Pending breakpoints are inserted (or removed) as soon as the debugger
# enters the target source file (which is observed by watchfunction).
my %pending_breaks;

# path to the current source file (possibly non-canonical)
my $cur_source_path;

# we try to invoke Cwd::abs_path as seldom as possible for better performance...
my %abs_path_cache = ();

# Adds a breakpoint for a given source file, line and condition.
# Depending on the debugger's state the breakpoint is inserted immediately
# or added to the pending list. The invoker should not care. 
#
# @param source_path    path to the source file (possibly non-canonical)
# @param line           line on which to break
# @param cond           (optional) breakpoint condition, ignored if ''
#
sub add_breakpoint
{
    $DB::trace = $DB::otrace = 0;
    eval { _add_breakpoint(@_); };
    # note/TODO: $@ ne '' here if the line was not breakable
    $DB::trace = $DB::otrace = 4;
}

# Installs epic_breakpoints::watchfunction in the debugger.
#
sub init
{        
    no warnings;
    %epic_breakpoints::pending_breaks = ();
    $DB::otrace = $DB::trace = 4;
    sub DB::watchfunction {
        $DB::trace = $DB::otrace = 0;
        my $ret;
        eval { $ret = epic_breakpoints::watchfunction($_[1], $_[2]); };
        # TODO: some sort of error reporting?
        $DB::trace = $DB::otrace = 4;
        return $ret;
    }
}

# Removes a breakpoint from a given source file.
# Depending on the debugger's state the breakpoint is removed immediately
# or added to the pending list. The invoker should not care.
#
# @param source_path    path to the source file (possibly non-canonical)
# @param line           line on which the breakpoint was previously added
#
sub remove_breakpoint
{
    $DB::trace = $DB::otrace = 0;
    eval { _remove_breakpoint(@_); };
    # TODO: some sort of error reporting?
    $DB::trace = $DB::otrace = 4;
}

# Called each time when DB::DB() is invoked, i.e. each time when the debugger
# gains control (which is very often).
# It is used to insert/remove pending breakpoints in newly entered source files.
#
sub watchfunction
{   
    my $source_path = _abs_path(shift);
    my $line = shift;
    
    $cur_source_path = $source_path;
    return if !defined($source_path);

    if (defined($pending_breaks{$source_path}))
    {
        foreach my $break(@{$pending_breaks{$source_path}})
        {
            if ($break->{add})
            {
                my $cond = $break->{cond} ne '' ? $break->{cond} : 1;
                eval { DB::break_on_line($break->{line}, $cond); };
                # note/TODO: $@ ne '' if the line was not breakable

                # force break now if we just entered the file on a line
                # which had a pending breakpoint:
                no warnings;
                $DB::single = 1 if ($break->{line} == $line);  
            }
            else
            {
                DB::delete_breakpoint($break->{line});
            }
        }
        delete $pending_breaks{$source_path};
    }
    return 0;
}

sub _abs_path
{
    my $path = shift;
    
    my $cached = $abs_path_cache{$path};
    return $cached if $cached;
    
    $cached = $abs_path_cache{$path} = abs_path($path);
    return $cached;
}

sub _add_breakpoint
{
    my $source_path = _abs_path(_trim(shift));
    my $line = _trim(shift);
    my $cond = _trim(shift);
    
    if (defined($cur_source_path) && $source_path eq _abs_path($cur_source_path))
    {        
        DB::break_on_line($line, $cond);
    }
    else
    {
        _remove_pending($source_path, $line);
        _add_pending($source_path, $line, $cond, 1);
    }    
}

sub _add_pending
{
    my $source_path = shift;
    my $line = shift;
    my $cond = shift;
    my $add = shift;
    
    $pending_breaks{$source_path} = [] if (!defined($pending_breaks{$source_path}));
    push(
        @{$pending_breaks{$source_path}},
        { line => $line, cond => $cond, add => $add });
}

sub _remove_breakpoint
{
    my $source_path = _abs_path(_trim(shift));
    my $line = _trim(shift);
    
    if ($source_path eq _abs_path($cur_source_path))
    {
        DB::delete_breakpoint($line);
    }
    else
    {
        _remove_pending($source_path, $line);
        _add_pending($source_path, $line, '', 0);
    }
    
    $DB::trace = $DB::otrace = 4;
}

sub _remove_pending
{
    my $source_path = shift;
    my $line = shift;

    return if (!defined($pending_breaks{$source_path}));

    my $i = 0;
    foreach my $break(@{$pending_breaks{$source_path}})
    {
        if ($break->{line} == $line)
        {
            splice(@{$pending_breaks{$source_path}}, $i, 1);
            return;
        }
        $i++;
    }
}

sub _trim
{
    my $str = shift;
    $str =~ s/^\s*//s;
    $str =~ s/\s*$//s;
    return $str;
}

1;