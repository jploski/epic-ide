sub main_sub
{
}

{
package First;

use Foo::Bar;
use Blah;

sub one
{
	print "one\n";
}

sub two
{
	print "two\n";
	
	package Third;
	
	sub six
	{
		print "six\n";
	}
	
	sub seven
	{
		print "seven\n";
	}
}

sub two2
{
}

package Second;

use Tra::Lala;

sub three
{
	print "three\n";
}

sub five
{
	print "FIVE\n";
}

sub four
{
	print "four\n";
	
	sub five
	{
		print "five\n";
	}
}
}

sub main_sub2
{
}

1;

__END__

sub after_eof
{
}