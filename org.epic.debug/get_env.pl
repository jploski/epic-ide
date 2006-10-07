$marker = $ARGV[0]; 
while(($k,$v)= each %ENV){ print "$marker$k=$v\n"};