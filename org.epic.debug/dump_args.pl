;{    
do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_lexical_vars;
    
my $savout = CORE::select($DB::OUT);
my $savbuf = $|;
$| = 0;
&dumpvar_epic::dump_array(\@_);
$| = $savbuf;
print "";
CORE::select($savout);
};
