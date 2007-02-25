;{
    if (eval { require PadWalker; PadWalker->VERSION(0.08) })
    {
        do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_lexical_vars;

        defined &dumpvar_epic::dump_lexical_vars
            or print $DB::OUT "dumpvar_epic.pm not available.\n";

        my $savout = select($DB::OUT);
        dumpvar_epic::dump_lexical_vars();
        select($savout);
    }
    else
    {
        print $DB::OUT "PadWalker module not found - please install\n";        
    }
};
