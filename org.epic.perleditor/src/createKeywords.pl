###########################################################
#
# Generate quickreference.properties file from 
# keyword list (keywords.txt) by parsing perldoc output.
#
##########################################################


use strict;
 
my $keywordFile = "keywords.txt";
my $outFile = "quickreference.properties";

open(KEYWORDS, "$keywordFile") || die "Unable to open $keywordFile: $!\n";
open(OUT, ">$outFile") || die "Unable to open $outFile: $!\n";

foreach my $keyword (<KEYWORDS>) {
    chomp($keyword);
    my $result = `perldoc -t -f $keyword`;
    $result =~ s/(.*?\.)\n\n.*/$1/sm;
    $result =~ s/\\/\\\\/g;
    $result =~ s/\n/\\n/g;
    
    print OUT "$keyword=$result\n" if($result);
}

close(OUT);
close(KEYWORDS);