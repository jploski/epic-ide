#!/usr/bin/perl

use Data::Dumper;

use lib 'noinc';
use TestDebugger3;
use TestAccessor;

my $arr = [ { key => 1 }, { key => 2 } ];
my @list = ( { key => 1 }, { key => 2 } );
my @list2 = ( 1, 2, 3 );
my $arr2 = [ 'aa', 'bb', \@list2 ];
my $hash = { key1 => $arr2, key2 => \$arr };
@foo = (1, 2, 3, $hash);
$foo3 = [1, 2, 3];
%h = ( key => 'val', key2 => [ 1, 2 ] );
$h2 = { key => 'val', key2 => [ 1, 2 ] };
$scal = 123;
$ref1 = \%h;
$ref2 = \$h2;
my $foo2 = \@foo;

TestDebugger3::foo(123, 'abc');
TestAccessor::test();

print "ok\n";