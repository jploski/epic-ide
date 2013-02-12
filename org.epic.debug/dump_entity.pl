;{    
do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_lexical_vars;
    
#SET_OFFSET#
#SET_VAREXPR#
#SET_SUBREF#
my $savout = CORE::select($DB::OUT);
my $savbuf = $|;
$| = 0;
$subref->($offset, $varexpr);
$| = $savbuf;
print "";
CORE::select($savout);
};
