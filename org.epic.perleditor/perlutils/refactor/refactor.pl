#!/usr/bin/perl
package main;

use Devel::Refactor;

my $subName     = $ARGV[0];
my $codeSnippet = $ARGV[1];
my $delimiter   = $ARGV[2];

my $refactory = new Devel::Refactor();
my ($newSubCall, $newCode) =
  $refactory->extract_subroutine($subName, $codeSnippet);

print $newSubCall . $delimiter . $newCode;

