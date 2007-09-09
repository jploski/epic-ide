sub first { }

sub second
{	
}#test
sub third
{
	my $sub = sub {
		# no fold
		};
	
	sub fourth
	{
	}
}

=comment
testing
=cut back
=comment
=cut

sub sixth; # no folding

sub optional_1(;$) {
}

sub optional_2($;$) {
}

if (1)
{ 
    
}

__END__

sub fifth
{
	# no fold
}

=comment
no folding here
=cut