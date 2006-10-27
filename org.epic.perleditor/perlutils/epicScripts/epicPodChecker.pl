#
# $Id$
#
# EPIC replacement for perlcritic
#
# allows the output format to be controlled for easier parsing and removes
# the need for the user to specify a path to the executable. if the package
# can't be found in the include path, or the minimum version number is not
# found, we can die gracefully and log an error message to alert the user.
#

use strict;

my $version;
eval {
    use Perl::Critic;
    $version = $Perl::Critic::VERSION;

    die if ($version < 0.17);
};

if ($@)
{
    my $error =
      "invalid Perl::Critic version ($version), please upgrade or install" .
      " to version 0.17 or greater";

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
    print $out_fh ("$file:$level:$line:$msg\n")
      if ($self->{-warnings} || !%opts || $opts{-severity} ne 'WARNING');
}

1;

