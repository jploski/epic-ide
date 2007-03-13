require 5.002;    # For (defined ref)

package dumpvar_epic;

binmode($DB::OUT, ":utf8");

use strict;
use warnings;

# Needed for PrettyPrinter only:

# require 5.001;  # Well, it coredumps anyway undef DB in 5.000 (not now)

# translate control chars to ^X - Randal Schwartz
# Modifications to print types by Peter Gordon v1.0

# Ilya Zakharevich -- patches after 5.001 (and some before ;-)

# Won't dump symbol tables and contents of debugged files by default

# Defaults

my $MaxUnwrapCount = 200;
my $UnwrapCount    = 0;
my $tick           = "auto";
my $unctrl         = 'quote';
my $subdump        = 1;
########################
my $TOKEN_NAME   = "N";
my $TOKEN_STRING = "S";
my $TOKEN_IN     = "I";
my $TOKEN_OUT    = "O";
my $TOKEN_REUSED = "R";

#########################
my %address;


sub dump_lexical_vars
{
    my $h = eval { PadWalker::peek_my(3) };
    $@ and $@ =~ s/ at .*//, print $DB::OUT ($@);
    _dump_lexical_var($_, $h->{$_}) for sort keys %$h;
    print "E";
}

sub dump_package_vars
{
    my $package = shift || '';
    $package .= "::" unless $package =~ /::$/;
    
    my ($dollar_comma, $dollar_backslash) = ($,, $\);
    ($,, $\) = undef;

    %address = ();
    
    my %stab = %{main::};
    while ($package =~ /(\w+?::)/g) { %stab = $stab{$1}; }

    foreach my $key (sort keys %stab)
    {
        my $val = $stab{$key};
        return if $DB::signal;
        _dump_package_var($key, $val);
    }

    print "E";
    ($,, $\) = ($dollar_comma, $dollar_backslash);
}

sub _CvGV_name_or_bust
{
    my $in = shift;

    $in = \&$in;            # Hard reference...
    eval { require Devel::Peek; 1 } or return;
    my $gv = Devel::Peek::CvGV($in) or return;
    *$gv{PACKAGE} . '::' . *$gv{NAME};
}

sub _dump_elem
{
    my $short = _stringify($_[0], ref $_[0]);

    print "$short";
    _unwrap($_[0], $_[1]) if ref $_[0];
}

sub _dump_lexical_var
{
    return if $DB::signal;

    my $key = shift;
    my $val = shift;

    %address = ();
    $UnwrapCount = 0;

    if (UNIVERSAL::isa($val, 'ARRAY'))
    {
        print _name("$key");
        _unwrap($val, -1);
    }
    elsif (UNIVERSAL::isa($val, 'HASH'))
    {
        print _name("$key");
        _unwrap($val, -1);
    }
    elsif (UNIVERSAL::isa($val, 'IO'))
    {
        print _name("FileHandle($key)") . _string("=> $val");
    }
    #  No lexical subroutines yet...
    #  elsif (UNIVERSAL::isa($val,'CODE')) {
    #    _dump_sub($off, $$val);
    #  }
    else
    {
        print _name(_unctrl($key));
        _dump_elem($$val, -1);
    }
}

sub _dump_package_var
{
    return if $DB::signal;

    my $key = shift;
    my $val = shift;

    local (*dumpvar_epic::entry);
    *dumpvar_epic::entry = $val if (defined $val);

    my $fileno;
    $UnwrapCount = 0;

    if ($key !~ /^_</ and defined $dumpvar_epic::entry)
    {
        print _name("\$" . _unctrl($key));
        _dump_elem($dumpvar_epic::entry, -1);
    }
    if ($key !~ /^_</ and @dumpvar_epic::entry)
    {
        print _name("\@$key");
        _dump_elem(\@dumpvar_epic::entry, -1);
    }
    if (   $key ne "main::"
        && $key ne "DB::"
        && %dumpvar_epic::entry
        && $key !~ /::$/
        && $key !~ /^_</)
    {
        print _name("\%$key");
        _dump_elem(\%dumpvar_epic::entry, -1);
    }
    if (defined($fileno = fileno(*entry)))
    {
        print(
            _name("FileHandle($key)") . _string("fileno($fileno)"));
    }
}

sub _dump_sub
{
    my ($off, $sub) = @_;
    my $ini = $sub;
    my $s;
    $sub = $1 if $sub =~ /^\{\*(.*)\}$/;
    my $subref = defined $1 ? \&$sub : \&$ini;
    my $place = $DB::sub{$sub}
      || (($s = $dumpvar_epic::subs{"$subref"})    && $DB::sub{$s})
      || (($s = _CvGV_name_or_bust($subref))       && $DB::sub{$s})
      || ($subdump && ($s = _find_subs("$subref")) && $DB::sub{$s});
    $place = '???' unless defined $place;
    $s     = $sub  unless defined $s;
    print _string(" -> &$s in $place");
}

sub _find_subs
{
    return undef unless %DB::sub;
    my ($addr, $name, $loc);
    while (($name, $loc) = each %DB::sub)
    {
        $addr = \&$name;
        $dumpvar_epic::subs{"$addr"} = $name;
    }
    $subdump = 0;
    $dumpvar_epic::subs{ shift() };
}

sub _name
{
    my $text = shift;

    return $TOKEN_NAME . _string($text);
}

sub _string
{
    my $text = shift;

    return sprintf("%s%08x%s", $TOKEN_STRING, length $text, $text);
}

sub _stringify
{
    my $str = shift;
    my $noticks = shift || 0;

    return _string('undef') unless defined $str;
    return _string($str . "") if ref \$str eq 'GLOB';
    
    my $strval = ${overload::}{'StrVal'};
    eval { $str = $strval->($str)
      if ref $str
      and %overload::
      and defined $strval; };

    if ($tick eq 'auto')
    {
        if ($str =~ m/[\000-\011\013-\037\177]/)
        {
            $tick = '"';
        }
        else
        {
            $tick = "'";
        }
    }
    if ($tick eq "'")
    {
        $str =~ s/([\'])/\\$1/g;
    }
    elsif ($unctrl eq 'unctrl')
    {
        $str =~ s/([\"])/\\$1/g;
        $str =~ s/([\000-\037\177])/'^'.pack('c',ord($1)^64)/eg;
    }
    elsif ($unctrl eq 'quote')
    {
        $str =~ s/([\"\$\@])/\\$1/g if $tick eq '"';
        $str =~ s/\033/\\e/g;
        $str =~ s/([\000-\037\177])/'\\c'.chr(ord($1)^64)/eg;
    }
    $str = _uniescape($str);
    ($noticks || $str =~ m/^\d+(\.\d*)?\Z/)
      ? _string($str)
      : _string($tick . $str . $tick);
}

sub _unctrl
{
    my $key = shift;

    return \$key if (ref \$key eq "GLOB");

    $key =~ s/([\001-\037\177])/'^'.pack('c',ord($1)^64)/eg;
    return $key;
}

sub _uniescape
{
    join("",
        map { $_ > 255 ? sprintf("\\x{%04X}", $_) : chr($_) }
          unpack("U*", $_[0]));
}

sub _unwrap
{
    return if $DB::signal;
    
    my $v = shift;
    my $m = shift || 0;    # maximum recursion depth

    return if $m == 0;

    my $item_type;

    # Check for reused addresses
    if (ref $v)
    {
        my $val = $v;
        my $strval = ${overload::}{'StrVal'};
        eval { $val = $strval->($v)
          if %overload:: and defined $strval; };

        # Match type and address.
        # Unblessed references will look like TYPE(0x...)
        # Blessed references will look like Class=TYPE(0x...)
        my ($addr, $start_part);
        ($start_part, $val) = split /=/, $val;
        $val = $start_part unless defined $val;
        ($item_type, $addr) =
            $val =~ /([^\(]+)    # Keep stuff that's
                                 # not an open paren
                 \(              # Skip open paren
                 (0x[0-9a-f]+)   # Save the address
                 \)              # Skip close paren
                 $/x;    # Should be at end now

        if (defined $addr)
        {
            $address{$addr}++;
            if ($address{$addr} > 1)
            {
                print $TOKEN_REUSED;
                return;
            }
        }
    }

    if ($UnwrapCount > $MaxUnwrapCount)
    {
        print _string("...Cut...");
        return;
    }
    $UnwrapCount++;

    if (ref \$v eq 'GLOB')
    {
        # This is a raw glob. Special handling for that.
        my $addr = "$v" . "";    # To avoid a bug with globs
        $address{$addr}++;
        if ($address{$addr} > 1)
        {
            print _string("*DUMPED_GLOB*");
            $UnwrapCount--;
            return;
        }
    }

    if (ref $v eq 'Regexp')
    {
        # Reformat the regexp to look the standard way.
        my $re = "$v";
        $re =~ s,/,\\/,g;
        print _string(" -> qr/$re/");
        $UnwrapCount--;
        return;
    }

    if ($item_type eq 'HASH')
    {
        # Hash ref or hash-based object.
        my @sortKeys = sort keys(%$v);

        if (@sortKeys) { print _string("...") . $TOKEN_IN }
        for my $key (@sortKeys)
        {
            return if $DB::signal;
            my $value = ${$v}{$key};
            print $TOKEN_NAME, _stringify($key);
            _dump_elem($value, $m - 1);
        }
        if   (@sortKeys) { print $TOKEN_OUT}
        else             { print _string(" <empty hash> ") }
    }
    elsif ($item_type eq 'ARRAY')
    {
        # Array ref or array-based object. Also: undef.
        # See how big the array is.

        print _string("...") . $TOKEN_IN if @$v;

        for my $num ($[ .. $#{$v})
        {
            return if $DB::signal;
            print _name("[$num]");
            if (exists $v->[$num])
            {
                if (defined $v->[$num])
                {
                    _dump_elem($v->[$num], $m - 1);
                }
                else
                {
                    print _string("undef");
                }
            }
            else
            {
                print _string("empty slot");
            }
        }
        if (@$v) { print $TOKEN_OUT }
        else     { print _string(" <empty array> "); }
    }
    elsif ($item_type eq 'SCALAR')
    {
        unless (defined $$v)
        {
            print _string("-> undef");
            $UnwrapCount--;
            return;
        }
        print _string("-> ");
        _dump_elem($$v, $m - 1);
    }
    elsif ($item_type eq 'REF')
    {
        print _string("-> $$v");
        return unless defined $$v;
        _unwrap($$v, $m - 1);
    }
    elsif ($item_type eq 'CODE')
    {
        # Code object or reference.
        _string("-> ");
        _dump_sub(0, $v);
    }
    elsif ($item_type eq 'GLOB')
    {
        # Glob object or reference.
        print _string("-> "), _stringify($$v, 1);
        
        my $fileno;
        if (defined($fileno = fileno($v)))
        {
            print _string("FileHandle({$$v}) => fileno($fileno)");
        }
    }
    elsif (ref \$v eq 'GLOB')
    {
        my $fileno;
        
        # Raw glob (again?)
        if (defined($fileno = fileno(\$v)))
        {
            print _string("FileHandle({$v}) => fileno($fileno)");
        }
    }
}

1;
