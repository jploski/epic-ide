;{    
do 'dumpvar_epic2.pm' unless defined &dumpvar_epic2::dump_lexical_vars;
    
#SET_OFFSET#
#SET_VAREXPR#
#SET_SUBREF#
my $savout = select($DB::OUT);
$subref->($offset, $varexpr);
select($savout);
};
