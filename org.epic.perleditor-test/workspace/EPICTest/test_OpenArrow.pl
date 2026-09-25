#!/usr/bin/perl

use warnings;
use strict;

use TestOpenArrow;

my $size = TestOpenArrow->MAX_SIZE;
my $value = TestOpenArrow->compute_value();

print "$size $value\n";
