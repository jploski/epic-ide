package TestCompletion;

BEGIN
{
	use Exporter();
	use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);
	@ISA = qw(Exporter);
	@EXPORT = qw(first_sub second_sub fourth_sub);
}

sub first_sub
{
}

sub second_sub
{
}

sub third_sub
{
}

sub fourth_sub
{
}

sub new
{
	my $class = shift;
	my $self;

	return bless($self, $class);
}

1;