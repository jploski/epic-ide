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

*foo = 5;    # TODO *foo should be a variable, not operator * followed by bareword
$x = 2*5;    # * is an operator, *5 is not a variable
$y = 5**$x;  # ** is an operator, *$ not a variable
$ x = 10;    # TODO "$ x" should be a variable, same as "$x"; 'x' not an operator here
sub abc($$;@) { }  # $$;@ is prototype
@$x = @$y;   # @$x, @$y are variables
my $b = $y % $x; # % is the modulo operator, not part of the "% $y" variable, cf. "$ x" above

# A backslash followed by \n should not mess up line counting and syntax hl thereafter
print " test\
";
print $blah;

my ($x, $y) = ($Ev->x, $Ev->y); # x and y after -> are normal words, not operators

$p->{'mode'} =~ m//; # m after =~ is an operator, even if it comes after ->

format=
hello world
.

sub format { $x = 'baba'; }

$result .= qq?<a href="#" onclick="parent.ausgabeVererben()">? . $cat->{'x'} . qq?</a>.<br />?;

foo(x => 5, y => 'b', y/a/b/, 'x' x 5);

# bug 1598235
$x = $args{y};

# bug 1685564
use constant DEGTORAD => PI/180;

# bug 1700095
my $hash_ref = {names => qw/lisa bart/ };