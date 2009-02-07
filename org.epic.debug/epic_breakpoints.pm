package epic_breakpoints;

# When updating, do not forget the POD description at the end of this file.

use strict;
use warnings;

use Cwd 'abs_path';

# pending_breaks maps the canonical path of each source file with pending
# breakpoints to an array of hash refs with keys { line, cond, add }
# Pending breakpoints are inserted (or removed) as soon as the debugger
# loads the target source file (and invokes epic_breakpoints::_postponed).
my %pending_breaks = ();

# we try to invoke Cwd::abs_path as seldom as possible for better performance...
my %abs_path_cache = ();

# Canonical path to the executed script. We track it separately because it
# does not appear in %INC, unlike paths of all the other source files. $0 may
# be changed by the script later on, but it should be pristine at this point.
my $main_script_key = $0; 
my $main_script_path = _abs_path($main_script_key);

# -------------------------------------------------------------------------
# API for EPIC

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
    eval { _add_breakpoint(@_); };
    # note/TODO: $@ ne '' here if the line was not breakable
}

# Converts a relative path in the debugger file system to absolute.
#
# @param source_path    relative path to the source file
#
sub get_abs_path
{
    no warnings;
    eval { print $DB::OUT _abs_path(@_) };
    # TODO: some sort of error reporting?
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
    eval { _remove_breakpoint(@_); };
    # TODO: some sort of error reporting?
}

# -------------------------------------------------------------------------
# Implementations of private subroutines & API for DB

sub _abs_path
{
    my $path = shift;
    
    my $cached = $abs_path_cache{$path};
    return $cached if $cached;
    
    eval { $cached = $abs_path_cache{$path} = abs_path($path); };
    return defined($cached) ? $cached : $path;
}

sub _add_breakpoint
{
    my $source_path = _abs_path(_trim(shift));
    my $line = _trim(shift);
    my $cond = _trim(shift) || 1;

    if ($source_path eq $main_script_path)
    {
        eval { DB::break_on_filename_line($main_script_path, $line, $cond); };
    }
    else
    {
        foreach my $key(keys %INC)
        {
            next if (!$key || _abs_path($INC{$key}) ne $source_path);
            eval { DB::break_on_filename_line($INC{$key}, $line, $cond); };
        }
    }

    # Here we add this breakpoint to pending_breaks in any case,
    # even if the above break_on_filename_line succeeded. The reason
    # is that the file might become 'require'd in using a different
    # path in the future and we want this breakpoint to be set then.
    _remove_pending($source_path, $line);
    _add_pending($source_path, $line, $cond, 1);
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

# Called (only) from (a patched version of) DB::postponed each time a new
# source file is loaded. It is used to insert/remove pending breakpoints
# in such source files.
#
sub _postponed
{
    my $filename = shift;
    return if (index($filename, ' (autosplit') > 0); # see bug 1742003
    
    my $line = shift;
    my $source_path = _abs_path($filename);
    
    no warnings;

    return if (!defined($pending_breaks{$source_path}));
    
    my %breaks_to_add = ();
    my $ret = 0;
    foreach my $break(@{$pending_breaks{$source_path}})
    {
        if ($break->{add})
        {
            $breaks_to_add{$break->{line}} = $break->{cond};

            # force break now if we just entered the file on a line
            # which had a pending breakpoint:            
            $ret = 1 if ($break->{line} == $line);  
        }
    }
    # Note that we DON'T delete $pending_breaks{$source_path} here.
    # This is because the file might be 'require'd using a different
    # path later in which case we still want its breakpoints to be set.
    
    $DB::postponed_file{$filename} = \%breaks_to_add if (scalar keys(%breaks_to_add));
    
    return $ret;
}

sub _remove_breakpoint
{
    my $source_path = _abs_path(_trim(shift));
    my $line = _trim(shift);

    my $pending = 0;
    
    if ($source_path eq $main_script_path)
    {
        eval
        {
            no warnings;
            local *DB::dbline = $main::{'_<'.$main_script_key};
            DB::delete_breakpoint($line);
        };
        $pending = 1 if ($@ ne '');
    }
    foreach my $key(keys %INC)
    {
        next if (_abs_path($INC{$key}) ne $source_path);
        eval
        {
            no warnings;
            local *DB::dbline = $main::{'_<'.$key};
            DB::delete_breakpoint($line);
        };
        $pending = 1 if ($@ ne '');
    }

    if ($pending)
    {
        _remove_pending($source_path, $line);
        _add_pending($source_path, $line, '', 0);
    }
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

__DATA__

=pod
=head1 epic_breakpoints.pm

This package, together with a patched perl5db.pl, provides a reliable
replacement for the unreliable break-on-load functionality of the Perl
debugger. Basically, breakpoints can only inserted into or removed from
files that have been already loaded by the Perl debugger. Source files
are loaded when the 'require' statement is executed ('use' statements
also call 'require' internally). Perl refers to loaded source files 
using paths, some of which may be relative, and some of which may be
absolute, but non-unique nevertheless (due to symlinks). Each such path
is stored as a value in %INC after the file has been loaded. 

For example, if you use

    require /tmp/x.pl

in the source code, the file will be known to Perl as C</tmp/x.pl>,
and if you use

    require /tmp/foo/../x.pl

the file will be known as C</tmp/foo/../x.pl>, even though both paths
refer to the same source file. Using the command-line debugger, you
could set breakpoints in both files completely independently from each
other. However, this is not the behavior expected in an EPIC, where
individual source files are available for the purpose of setting
breakpoints and their paths are of little interest. 

As we cannot reliably guess the Perl-level paths on the Java side,
we monitor the debugger's loading of source files on the Perl side
and do the necessary path translations here. In particular, we use
canonicalized paths for comparisons to determine whether the newly
loaded file is one with pending breakpoints. Similarly, we canonicalize
the paths contained in %INC to find the set of already loaded source
files referring to a breakpoint about to be set or removed.

perl5db.pl has to be patched by EPIC to achieve this functionality
because the vanilla version does not provide an API to call
a user-defined subroutine on every loaded file, regardless
of the filename.

=cut
