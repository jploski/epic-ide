#!/usr/bin/perl

use warnings;
use strict;

require "lib/TestOpenSub2.pm";

foreach my $x (@INC)
{
	print "$x\n";
}

=comment
sub foo {
	
}

#sub some_sub
{
	print "bla";
}

sub   _baz_zz($$)    { print "foo"; }

		sub brzdek # tralaa
		{ foo(); }
=cut

other_sub(&some_sub());