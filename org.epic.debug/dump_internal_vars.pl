;{
do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dump_package_vars;

my $savout = CORE::select($DB::OUT);
dumpvar_epic::_dump_entity('@_', \@_);
dumpvar_epic::dump_package_vars();
CORE::select($savout);
};
