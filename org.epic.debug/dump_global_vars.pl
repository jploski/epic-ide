;{
do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_package_vars;

my $savout = select($DB::OUT);
dumpvar_epic::dump_package_vars(__PACKAGE__);
select($savout);
};
