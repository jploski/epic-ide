  ;{\
  print DB::OUT "hallo\n";\
  eval { require PadWalker; PadWalker->VERSION(0.08) }or &warn("PadWalker module not found - please install\n");\
  do 'dumpvar_epic.pl' unless defined &main::dumpvar_epic;\
  defined &main::dumpvar_epic or print $OUT "dumpvar_epic.pl not available.\n";\
  my $h = eval { PadWalker::peek_my(2) };\
  my @vars = split (' ','');\
 $@ and $@ =~ s/ at .*//, &warn($@);\
 my $savout = select($DB::OUT);\
 print  DB::OUT "hallo1\n";\
 dumpvar_epic::dumplex(\
 $_,\
 $h->{$_},\
 defined $option{dumpDepth} ? $option{dumpDepth} : -1,\
 @vars\
 ) for sort keys %$h;\
 select($savout);\
};