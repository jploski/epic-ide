# Creates the errorsAndWarnings.properties file
open(DIAG, "perldoc -u perldiag |") || die "$!\n";
while(<DIAG>) { $diag .= $_; }
close(DIAG);

#while($diag =~ /=item(.*?)\n\n(.*?)\n\n/gs) {
while($diag =~ /item(.*?)\n\n(.*?)\n=/gs) {
  $count++;
  $message = $1;
  $desc = $2;
  $message =~ s/^\s//;
  $message =~ s/C</</g;
  $desc =~ s/\n/\\n/g;
  $desc =~ s/\t/ /g;
  print "E$count=$message\t$desc\n";
}

