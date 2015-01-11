#!/usr/bin/perl

use strict;
use warnings;
use lib 'noinc';

use Cwd;
use TestDebugger3;

require 'test_Debugger2.pl';

TestDebugger3::foo();

my $x = 123;
my $str = 'string';
my $hash = { first => $x, second => $str };

foo($hash);

sub foo
{
	my $bar = shift;
	$bar->{third} = $bar;
	print "one\n";
	print "two\n";
	print "three\n";
	
	my $path = getcwd().'/test_Debugger2.pl';
	require $path;
}