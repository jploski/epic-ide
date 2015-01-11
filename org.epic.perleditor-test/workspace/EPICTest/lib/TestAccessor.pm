package TestAccessor;

use Class::Accessor 'antlers';

has select                 => ( is => 'ro', ); # evil: accessor name = builtin function 

sub new
{
	my $class = shift;
	my $self = { select => 'abc', };
	bless($self, $class);
	return $self;
}

sub test
{
	print "bla\n";
}

1;