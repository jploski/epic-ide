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

__END__

sub fifth
{
	# no fold
}

=comment
no folding here
=cut