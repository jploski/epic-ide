;{    
do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_lexical_vars;

#SET_OFFSET#
my $savout = select($DB::OUT);
dumpvar_epic::dump_lexical_vars($offset);
select($savout);
};
