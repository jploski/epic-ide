use Cwd 'abs_path';
use IO::Socket;
#SET MULTIADDRDEFAULT#
#SET LAUNCHDEFAULT#
my $firstArg=shift @ARGV;
my $multiAddr;
my @launch = @ARGV;
if($firstArg=~/[^:]+:\d+/){
	$multiAddr=$firstArg;
}else{
	$multiAddr=$multiAddrDefault;
	unshift(@launch, $firstArg);
}
die "specify perl launch arguments" if(!@launch);

my ($addr, $mport, $redirect) = ($multiAddr =~ /([^:]+):(\d+)(r?)/);
#ask a perlmultiRemote lauch for a new debugger port
my $sock = IO::Socket::INET->new("$addr:$mport");
my $port;
$sock->send($redirect?"CONR\n":"CONN\n");
$sock->recv($port, 4);
$sock->close();
sleep 2;

$ENV{PERLDB_OPTS}="RemotePort=$addr:$port DumpReused ReadLine=0 PrintRet=0";
mkdir 'EPICDEBUG';
patchPerl5db();
my $autoflush_epic;
my $dumpvar_epic;
my $epic_breakpoints;
setScriptTexts();
write_autoflush_epic();
write_dumpvar_epic();
write_epic_breakpoints();
system("$^X -d  -I\"EPICDEBUG\" @launch");

sub patchPerl5db {
	my $marker = 'return unless $postponed_file{$filename};';
	my $patch  = '    { use epic_breakpoints; my $osingle = $single; $single = 0; $single = epic_breakpoints::_postponed($filename, $line) || $osingle; }';
	my $found  = 0;
	for my $path (@INC) {
		if (-e "$path/perl5db.pl") {

			# Note: we do not use a replace all because of bug 1734045
			open(SFH, "<$path/perl5db.pl");
			open(OFH, ">EPICDEBUG/perl5db.pl");
			while (<SFH>) {
				my $line = $_;
				if ($line =~ /\Q$marker\E/) {
					$found=1;
					print OFH "$patch\n";
				}
				print OFH $line;
			}
			close OFH;
			close SFH;
			if(!$found){
				unlink "perl5db.pl";
			}else{
				return;
			}
		}
	}
	if(!$found){
		die "could not find a patchable perl5db";
	}
}

sub write_dumpvar_epic
{
	open(OFH, ">EPICDEBUG/dumpvar_epic.pm");
	print OFH $dumpvar_epic;
	close OFH;
}

sub write_autoflush_epic
{
	open(OFH, ">EPICDEBUG/autoflush_epic.pm");
	print OFH $autoflush_epic;
	close OFH;
}

sub write_epic_breakpoints
{
	open(OFH, ">EPICDEBUG/epic_breakpoints.pm");
	print OFH $epic_breakpoints;	
	close OFH;
}


sub setScriptTexts{








#      _                                                _      
#     | |                                              (_)     
#   __| |_   _ _ __ ___  _ ____   ____ _ _ __ ___ _ __  _  ___ 
#  / _` | | | | '_ ` _ \| '_ \ \ / / _` | '__/ _ \ '_ \| |/ __|
# | (_| | |_| | | | | | | |_) \ V / (_| | | |  __/ |_) | | (__ 
#  \__,_|\__,_|_| |_| |_| .__/ \_/ \__,_|_|  \___| .__/|_|\___|
#                       | |             ______   | |           
#                       |_|            |______|  |_|           
	$dumpvar_epic=<<'DUMPVAREPICEND';
#DUMPVAREPIC TEXT#
DUMPVAREPICEND








#              _         __ _           _                 _      
#             | |       / _| |         | |               (_)     
#   __ _ _   _| |_ ___ | |_| |_   _ ___| |__    ___ _ __  _  ___ 
#  / _` | | | | __/ _ \|  _| | | | / __| '_ \  / _ \ '_ \| |/ __|
# | (_| | |_| | || (_) | | | | |_| \__ \ | | ||  __/ |_) | | (__ 
#  \__,_|\__,_|\__\___/|_| |_|\__,_|___/_| |_| \___| .__/|_|\___|
#                                          ______  | |           
#                                         |______| |_|           
	$autoflush_epic=<<'AUTOFLUSHEPICEND';
#AUTOFLUSHEPIC TEXT#
AUTOFLUSHEPICEND








#             _        _                    _                _       _       
#            (_)      | |                  | |              (_)     | |      
#   ___ _ __  _  ___  | |__  _ __ ___  __ _| | ___ __   ___  _ _ __ | |_ ___ 
#  / _ \ '_ \| |/ __| | '_ \| '__/ _ \/ _` | |/ / '_ \ / _ \| | '_ \| __/ __|
# |  __/ |_) | | (__  | |_) | | |  __/ (_| |   <| |_) | (_) | | | | | |_\__ \
#  \___| .__/|_|\___| |_.__/|_|  \___|\__,_|_|\_\ .__/ \___/|_|_| |_|\__|___/
#      | |        ______                        | |                          
#      |_|       |______|                       |_|                          
	$epic_breakpoints=<<'EPICBREAKPOINTSEND';
#EPICBREAKPOINTS TEXT#
EPICBREAKPOINTSEND
	
}