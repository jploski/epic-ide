my($filename) = @ARGV;

die "No file specified!" if(!$filename);

open(IN, "$filename") || die "Unable to open file $filename: $!\n";
undef $/;
my $text = <IN>;
close(IN);

while($text =~ /[0-9]{2}\.[0-9]\.[0-9]{2,3}\. ([^\n]+)\n(.*?)\n([A-Z].*?\.)/smg) {
    next if(!$1 || !$2 || !$3);
    my $name = $1;
    my $desc = $3;
    my $syntax = $2;
    
    chomp($syntax);
    chomp($desc);
    chomp($name);
    
    $name =~ s/\s+$//;
    $desc =~ s/\n//g;
    
    next if(length($desc) > 200);
    
    $syntax =~ s/\n/\\n/g;
    my $out = "$name=$desc\\n\\nSyntax:\\n$syntax";
    $out =~ s/\\n$//;
    
    print "$out\n";
  
     
}
