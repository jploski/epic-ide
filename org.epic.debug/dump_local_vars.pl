;{
    eval { require PadWalker; PadWalker->VERSION(0.08) }
        or print $DB::OUT "PadWalker module not found - please install\n";

    do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dumpvar_epic;
    
    defined &dumpvar_epic::dumpvar_epic
        or print $DB::OUT "dumpvar_epic.pm not available.\n";

    my $h = eval { PadWalker::peek_my(2) };
    my @vars = split(' ', '');
    $@ and $@ =~ s/ at .*//, print $DB::OUT ($@);
    my $savout = select($DB::OUT);
    dumpvar_epic::dumplex(
        $_,
        $h->{$_},
        defined $option{dumpDepth} ? $option{dumpDepth} : -1,
        @vars) for sort keys %$h;
    print "E";
    select($savout);
};
