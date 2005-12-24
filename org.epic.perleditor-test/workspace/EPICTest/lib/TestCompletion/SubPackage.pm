package TestCompletion::SubPackage;

BEGIN
{
	use Exporter();
	use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);
	@ISA = qw(Exporter);
	@EXPORT = qw(sub_one sub_two sub_three);
}

sub sub_one
{
}

sub sub_two
{
}

sub sub_three
{
}

sub sub_four
{
}

1;