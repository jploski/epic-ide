package TestCompletion::Sub_Package2;

BEGIN
{
	use Exporter();
	use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);
	@ISA = qw(Exporter);
	@EXPORT = qw(sub2_one sub2_two sub2_three);
}

sub sub2_one
{
}

sub sub2_two
{
}

sub sub2_three
{
}

sub sub2_four
{
}

1;