#
# EPIC replacement for podchecker
#
# allows the output format to be controlled for easier parsing and removes
# the need for the user to specify a path to the executable. if the package
# can't be found in the include path, or the minimum version number is not
# found, we can die gracefully and log an error message to alert the user.
#

use strict;

my $version;
eval {
    use Pod::Checker;
    $version = $Pod::Checker::VERSION;

    die if ($version < 1.42);
};

if ($@)
{
    my $error =
      "invalid Pod::Checker version ($version), please upgrade" .
      " to version 1.42 or greater";

    print STDERR "$error\n";
    exit(1);
}

my $checker = new EpicPodChecker();
$checker->parse_from_file($ARGV[0]);

package EpicPodChecker;
use base qw(Pod::Checker);

sub poderror
{
    my $self = shift;
    my %opts = (ref $_[0]) ? %{shift()} : ();
    
    # TODO: $_[0] above is sometimes a string, for example:
    # *** WARNING: line containing nothing but whitespace in paragraph at line X in file Y
    # These messages apparently also go to STDERR. 
    # Try running this script on Twig.pm to see them.
    # As a temporary (and probably wrong) workaround, let's ignore
    # these cases to prevent an exception on the Java side:
    return if !defined($opts{-line});

    # file:level:line:message

    my $file  = $opts{-file};
    my $level = $opts{-severity};
    my $line  = $opts{-line};
    my $msg   = $opts{-msg};

    ++($self->{_NUM_ERRORS})
      if (!%opts || ($opts{-severity} && $opts{-severity} eq 'ERROR'));
    ++($self->{_NUM_WARNINGS})
      if (!%opts || ($opts{-severity} && $opts{-severity} eq 'WARNING'));

    my $out_fh = $self->output_handle() || \*STDERR;
    print $out_fh ("$file~|~$level~|~$line~|~$msg\n")
      if ($self->{-warnings} || !%opts || $opts{-severity} ne 'WARNING');
}

1;

