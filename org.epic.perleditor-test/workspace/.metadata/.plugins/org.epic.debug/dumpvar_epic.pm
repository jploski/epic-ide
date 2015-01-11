package dumpvar_epic;

# When updating, do not forget the POD description at the end of this file.

binmode($DB::OUT, ":utf8");

use strict;
use warnings;

use Scalar::Util;
use overload;

use constant SEP => '|';
use constant MAX_SCALAR_LENGTH => 65536; 

# Dumps all elements of an array.
#
# @param array  array (reference to a list)
#
sub dump_array
{
    my $array = shift;
    
    my $i = 0;
    foreach my $elem(@{$array})
    {
        _dump_entity($i, \$$array[$i]);
        $i++;
    }
}

# Dumps all elements of an array designated by an expression.
#
# @param frame_index    0 = top stack frame (in user's code),
#                       1 = next lower frame etc.
# @param expr           expression which, evaluated in context containing
#                       the PadWalker stack frame $h, resolves to the desired
#                       array (list reference)
#
sub dump_array_expr
{
    my $frame_index = shift;
    my $expr = shift;

    my $h = eval { PadWalker::peek_my(3 + $frame_index) };
    if (!$@) { dump_array(eval($expr)); }
}

# Dumps all key-value pairs of a hash.
#
# @param hash   hash reference
#
sub dump_hash
{
    my $hash = shift;
    _dump_keys($hash, 1);
}

# Dumps all key-value pairs of a hash designated by an expression.
#
# @param frame_index    0 = top stack frame (in user's code),
#                       1 = next lower frame etc.
# @param expr           expression which, evaluated in context containing
#                       the PadWalker stack frame $h, resolves to the desired
#                       hash reference
#
sub dump_hash_expr
{
    my $frame_index = shift;
    my $varexpr = shift;

    my $h = eval { PadWalker::peek_my(3 + $frame_index) };     
    if (!$@) { dump_hash(eval($varexpr)); }
}

# Dumps all lexical variables from the stack frame with the given offset.
#
# @param frame_index    0 = top stack frame (in user's code),
#                       1 = next lower frame etc.
#
sub dump_lexical_vars
{
    my $frame_index = shift;

    my $h = eval { PadWalker::peek_my(3 + $frame_index) };
    if (!$@) { _dump_keys($h, 0); }    
}

# Dumps all variables from a package's symbol table.
#
# @param package 	(optional) name of the package; default: 'main'
#
sub dump_package_vars
{
    my $package = shift || '';
    $package .= "::" unless $package =~ /::$/;
    
    my ($dollar_comma, $dollar_backslash) = ($,, $\);
    ($,, $\) = undef;

    my $stab = \%{main::};
    while ($package =~ /(\w+?::)/g) { $stab = $stab->{$1}; }

    foreach my $key (sort keys %{$stab})
    {
        my $val = $stab->{$key};
        return if $DB::signal;
        _dump_package_var($key, $val);
    }

    ($,, $\) = ($dollar_comma, $dollar_backslash);
}

# The main subroutine which dumps an entity as described in the POD
# section of this module.
#
# @param name   name of the dumped entity
# @param ent    reference to the dumped entity
#
sub _dump_entity
{
    my $name = shift;
    my $ent = shift;
    
    print _token($name);
    
    my @refchain = ( overload::StrVal($ent) );
    my $entaddr = Scalar::Util::refaddr($ent);     
    my $tmp = $ent;
    my $cycle = 0;

    while (ref($tmp) eq 'REF')
    {        
        my $tmp2 = ${$tmp};
        $tmp = $tmp2;
        push(@refchain, overload::StrVal($tmp));
        if (Scalar::Util::refaddr($tmp) == $entaddr)
        {
            $cycle = 1;
            last;
        }        
    }    
    print SEP;
    print _token($#refchain+1);
    foreach my $t(@refchain)
    {
        print SEP;
        print _token($t);
    }

    my $val;
    if ($cycle)
    {
        $val = 'cycle'; 
    }
    elsif (ref($tmp) eq 'HASH' || ref($tmp) eq 'ARRAY')
    {
        $val = '...';
    }
    elsif (ref($tmp) eq 'SCALAR')
    {
        if (defined($$tmp))
        {
            if (length($$tmp) > MAX_SCALAR_LENGTH)
            {
                $val = '\''.substr($$tmp, 0, MAX_SCALAR_LENGTH).'\'';
            }
            else
            {
                $val = '\''.$$tmp.'\'';
            }
        }
        else { $val = 'undef'; }
    }
    else # CODE, GLOB, or possibly a blessed reference
    {
        if (overload::StrVal($tmp) eq ''.$tmp)
        {
            $val = '...'; # without stringify operation, treat as hash
        }
        else
        {
            $val = '\''.$tmp.'\''; # with stringify operation, treat as string
        } 
    }

    print SEP;
    print _token($val);
    print SEP;
    print _token(length($val));
    print "\n";
}

# Dumps all key-value pairs contained in a hash.
#
# @param h          reference to a hash
# @param add_ref    0 if the key values are already addresses, as in case of
#                   a hash provided by PadWalker; 1 if the key values are normal
#                   and therefore the address of each value has to be computed   
#
sub _dump_keys
{
    my $h = shift;
    my $add_ref = shift;

    foreach my $key(sort keys %$h)
    {
        _dump_entity($key, $add_ref ? \$h->{$key} : $h->{$key});
    }
}

# Dumps a single variable from a package's symbol table.
#
# @param key		key under which the variable is stored in the symbol table
# @param val        associated value
#
sub _dump_package_var
{
    return if $DB::signal;

    my $key = shift;
    my $val = shift;

    local (*dumpvar_epic::entry);
    *dumpvar_epic::entry = $val if (defined($val));
    
    eval
    {
        if ($key !~ /^_</ and defined $dumpvar_epic::entry) # SCALAR
        {
        	_dump_entity('$'._unctrl($key), \$dumpvar_epic::entry);
        }
        if ($key !~ /^_</ and @dumpvar_epic::entry) # ARRAY
        {
        	_dump_entity('@'.$key, \@dumpvar_epic::entry);
        }
        if ($key ne "main::" &&
            $key ne "DB::" &&
            %dumpvar_epic::entry &&
            $key !~ /::$/ &&
            $key !~ /^_</) # HASH
        {
        	_dump_entity('%'.$key, \%dumpvar_epic::entry);
        }
        if (defined(my $fileno = fileno(*entry)))
        {
        	_dump_entity("FileHandle($key)", \"fileno($fileno)");
        }
    };
    if ($@ ne '')
    {
        # Do not let the error spread to our caller.
        # See Bug 1735629 for a real example in which $@ ne '' here.
        _dump_entity("Error($key)", \"$@");
    }
}

# Outputs a single token as described in the POD section of this module.
#
# @param value      token's value 
#
sub _token
{
    my $value = shift;
    
    return length($value).SEP.$value;
}

sub _unctrl
{
    my $key = shift;

    return \$key if (ref \$key eq "GLOB");

    $key =~ s/([\001-\037\177])/'^'.pack('c',ord($1)^64)/eg;
    return $key;
}

1;

__DATA__

=pod
=head1 dumpvar_epic.pm

This module is invoked by EPIC to dump contents of Perl variables in order to
present them in the Variables view of the debugger.

=head1 Dumpable entities 

The I<entities> dumpable by this module can be classified as follows:

=over

=item * Type 1: lexical variables

=item * Type 2: key-value pairs of a hash

=item * Type 3: elements of a list

=back

=head1 Format of a dumped entity

The dump format is designed to be reasonably concise and easy to parse
while remaining human-readable to support debugging the debugger.

An entity is dumped by printing a series of tokens. Each token is a utf8
string and is preceded by its length expressed as a decimal integer.
Tokens, as well as the token length and token content, are separated
from each other with a pipe (vertical bar) character. Thus, the string
C<"3|abc"> would contain a single three-character long token with value
C<abc>, while the string C<"3|abc|2|de"> would contain two tokens, C<abc>
and C<de>. The explicit token lengths solve the parsing difficulty
caused by pipe characters potentially appearing inside of the token values.
(Another way would have been to use escaping and un-escaping of separator
characters, but it appeared less efficient and more cumbersome to implement.)
In the following description the token lengths are omitted for clarity.

The tokens which comprise a dumped entity are explained next,
in the order in which they appear in the dump (examples are provided
in the last section of this document):

=over

=item 1. (single token)

entity name

=over

=item *

a qualified name of a lexical variable (C<$x>, C<@x>, C<%x>) OR

=item *

an unqualified name of a hash key (C<x>) OR

=item *

an index of a list element (C<0>, C<1>, C<2>, ...)

=back

=item 2. (single token)

number of tokens comprising "entity address and reference chain" that follows

=item 3. (multiple tokens)

entity address and reference chain

The entity address is the address reported by Perl's built-in C<ref>
function for a hypothetical variable which could be dereferenced
using the C<${...}> operator to assign a new value to the entity.
If the entity itself is a reference, the address of the referenced
entity will be output in the following token, recursively until
either an address of a non-reference entity is finally output or
a cycle in the reference chain is detected. The final address in
the reference chain denotes the value type or is equal to the first
address in case of a circular reference chain.

=item 4. (single token)

entity value

=over

=item *

If the entity is not a reference:

=over

=item *

C<undef> (unquoted literal) for entities with an undefined value

=item *

C<...> (unquoted three dots literal) for hashes and lists

=item *

a singly quoted string value (C<'foo'>) for all other entities

=back

=item *

If the entity is a reference:

=over

=item *

the dumped value is of the final non-reference entity in the reference chain OR

=item *

C<cycle> (unquoted literal) in case of a circular reference chain OR

=item *

singly quoted value (C<'foo'>) of a referenced Perl object which supports
stringification

=back

=back

=item 5. (single token)

character length of the entity value

The total number of Unicode characters (as reported by Perl's length
function) comprising the full string value of the dumped entity.
The artificially inserted surrounding quotes are not counted.
This number can be evaluated in EPIC to determine whether the string value
was truncated before dumping.

=back

If multiple entities are dumped in a single request from EPIC (e.g. all elements
of a list or all key-value pairs of a hash), then each entity's tokens are followed
by a C<\n> (line feed) character.

=head1 Examples

This section contains some normative examples of dumps for the various entity
types. Each example consists of a line which assigns a value to the entity
and the dumped representation of the entity on the second line. Again, note
that the string lengths that in reality appear in front of each token are
skipped here for clarity.

=head2 Type 1: lexical variables 

    $x = 5

        $x|1|SCALAR(0x123456)|'5'|3
      
    $x = 'st\'r'

        $x|1|SCALAR(0x123456)|'st'r'|6

    @x = ( 1, 2, 3 )

        @x|1|ARRAY(0x123456)|...|3

    %x = ( key => 'value' )

        %x|1|HASH(0x123456)|...|3

    $x = 5, $y = \$x

        $y|2|REF(0x123457)|SCALAR(0x123456)|'5'|3

    $x = [ 1, 2, 3 ]

        $x|2|REF(0x123457)|ARRAY(0x123456)|...|3

    $x = { key => 'value' }

        $x|2|REF(0x123457)|HASH(0x123456)|...|3

=head2 Type 2: key-value pairs of a hash 

    $x->{"k ey"} = 5

        k ey|1|SCALAR(0x123456)|'5'|3
          
    $x->{'key'} = 'str'

        key|1|SCALAR(0x123456)|'str'|5
          
    $x->{'key'} = { other => 'value' }

        key|2|REF(0x123457)|HASH(0x123456)|...|3
          
    $x->{'key'} = [ 1, 2, 3 ]

        key|2|REF(0x123457)|ARRAY(0x123456)|...|3

=head2 Type 3: elements of a list

    $x[0] = 5

        0|1|SCALAR(0x123456)|'5'|3
          
    $x[1] = 'str'

        1|1|SCALAR(0x123456)|'str'|5

=cut
