;{
    do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_package_vars;

    defined &dumpvar_epic::dump_package_vars
        or print $DB::OUT "dumpvar_epic.pm not available.\n";
        
    my $savout = select($DB::OUT);
    dumpvar_epic::dump_package_vars();
    select($savout);
};
