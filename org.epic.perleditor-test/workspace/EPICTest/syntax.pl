# bug 1232049, 1285425
my $pid;
if ($pid=fork()) {
print "Never should be here\n";
exit -1;
}

# bug 1309321
if ($_ !~ /(^#|^\s*$)/) { $hello = 1; } 

# bug 1312851
my $result1= $self->executeSQLSelect(query=>$SQL);

# bug 1314702
$session{qtype} = 'database';

# bug 1354177
$filename=~s/'/_/g;
use File::Copy;
copy( "$OrigDrive:$TempFileName", "$dir\\$filename" );

# bug 1360594
my $test = $h->{m_id};
$test = 'test';

# bug 1256511
/^\s*\"(\w+)\"=\"(.*)\"/;

# bug 1175560
$x->format();

# bug 1175501
$x = 1 ? "?" : "x";

# bug 1305170
if ($path =~ m/\//s ) { $sep = '/'; }

# $something =~ m/#/; looks like a comment

$x =~ s<foo>'bar'; # comment