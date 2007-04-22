;{    
do 'dumpvar_epic2.pm' unless defined &dumpvar_epic2::dump_lexical_vars;

#SET_OFFSET#
my $savout = select($DB::OUT);
dumpvar_epic2::dump_lexical_vars($offset);
select($savout);
};
