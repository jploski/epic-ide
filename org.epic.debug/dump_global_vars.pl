;{
    do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dumpvar_epic;

    defined &dumpvar_epic::dumpvar_epic
        or print $DB::OUT "dumpvar_epic.pm not available.\n";
        
    my $savout = select($DB::OUT);
    dumpvar_epic::dumpvar_epic();
    select($savout);
};
