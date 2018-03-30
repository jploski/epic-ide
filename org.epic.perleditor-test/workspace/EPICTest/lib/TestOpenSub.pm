package TestOpenSub;

use base qw(Exporter);
our @EXPORT = qw(some_sub);

sub other_sub
{
	print "this is my 
		little sub!!!
			"; 				# comment
}

sub some_sub2
{
	print "some_sub";
	
}

sub some_sub #ok
{
	print "Hello, world!\n";
}

sub floobzik
{
}

1;