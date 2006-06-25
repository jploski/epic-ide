# Refactor.pm - refactor Perl code.
# $Header$
#
###############################################################################
=head1 NAME

Devel::Refactor - Perl extension for refactoring Perl code.

=head1 VERSION

$Revision$  This is the CVS revision number.

=head1 SYNOPSIS

  use Devel::Refactor;
  
  my $refactory = Devel::Refactor->new;
  
  my ($new_sub_call,$new_sub_code) =
     $refactory->extract_subroutine($sub_name, $code_snippet);

  my $files_to_change = $refactory->rename_subroutine('./path/to/dir',
                                                      'oldSubName','newSubName');
  # $files_to_change is a hashref where keys are file names, and values are
  # arrays of hashes with line_number => new_text
  
=head1 ABSTRACT

Perl module that facilitates refactoring Perl code.  

=head1 DESCRIPTION

The B<Devel::Refactor> module is for code refactoring. 

While B<Devel::Refactor> may be used from Perl programs, it is also designed to be
used with the B<EPIC> plug-in for the B<eclipse> integrated development environment.

=cut

package Devel::Refactor;

use strict;
use warnings;
use Cwd;
use File::Basename;

require Exporter;

our @ISA = qw(Exporter);

# Items to export into callers namespace by default. Note: do not export
# names by default without a very good reason. Use EXPORT_OK instead.
# Do not simply export all your public functions/methods/constants.

# This allows declaration	use Dev::Refactor ':all';
# If you do not need this, moving things directly into @EXPORT or @EXPORT_OK
# will save memory.
our %EXPORT_TAGS = ( 'all' => [ qw(
	
) ] );

our @EXPORT_OK = ( @{ $EXPORT_TAGS{'all'} } );

our @EXPORT = qw(
	
);

our $VERSION = '0.05'; 

our $DEBUG = 0;
# Preloaded methods go here.


our %perl_file_extensions = (
    '\.pl$' => 1,
    '\.pm$' => 1,
    '\.pod$' => 1,
);

=head1 CLASS METHODS

Just the constructor for now.

=head2 new

Returns a new B<Devel::Refactor> object.

=cut
# TODO: List the object properties that are initialized.

sub new {
    my $class        = shift;
    $DEBUG           = shift;

    # TODO: Should these really be object properties? No harm I guess, but most
    # of them are for the  extract_subroutine  method, and so maybe they
    # should go in a data structure dedicated to that method?
    my $self = {
        sub_name        => '',
        code_snippet    => '',
        return_snippet  => '',
        return_sub_call => '',
        eval_err        => '',
        scalar_vars     => {},
        array_vars      => {},
        hash_vars       => {},
        local_scalars   => {},
        loop_scalars    => {},
        local_arrays    => {},
        local_hashes    => {},
        parms           => [],
        inner_retvals   => [],
        outer_retvals   => [],
        perl_file_extensions       => { %perl_file_extensions },
    };

    bless $self, $class;
    
    return $self;
}



=head1 PUBLIC OBJECT METHODS

Call on a object returned by new().

=head2 extract_subroutine($new_name,$old_code [,$syntax_check])

Pass it a snippet of Perl code that belongs in its own subroutine as
well as a name for that sub.  It figures out which variables
need to be passed into the sub, and which variables might be
passed back.  It then produces the sub along with a call to
the sub.

Hashes and arrays within the code snippet are converted to 
hashrefs and arrayrefs.

If the I<syntax_check> argument is true then a sytax check is performed
on the refactored code.

Example:

    $new_name = 'newSub';
    $old_code = <<'eos';
      my @results;
      my %hash;
      my $date = localtime;
      $hash{foo} = 'value 1';
      $hash{bar} = 'value 2';
      for my $loopvar (@array) {
         print "Checking $loopvar\n";
         push @results, $hash{$loopvar} || '';
      }
    eos

    ($new_sub_call,$new_code) = $refactory->extract_subroutine($new_name,$old_code);
    # $new_sub_call is 'my ($date, $hash, $results) = newSub (\@array);'
    # $new_code is
    # sub newSub {
    #     my $array = shift;
    # 
    #   my @results;
    #   my %hash;
    #   my $date = localtime;
    #   $hash{foo} = 'value 1';
    #   $hash{bar} = 'value 2';
    #   for my $loopvar (@$array) {
    #      print "Checking $loopvar\n";
    #      push @results, $hash{$loopvar} || '';
    #   }
    # 
    # 
    #     return ($date, \%hash, \@results);
    # }


Included in the examples directory is a script for use in KDE
under Linux.  The script gets its code snippet from the KDE 
clipboard and returns the transformed code the same way.  The
new sub name is prompted for via STDIN.

=cut

sub extract_subroutine {
    my $self         = shift;
    my $sub_name     = shift;
    my $code_snippet = shift;
    my $syntax_check = shift;

    $DEBUG and print STDERR "sub name : $sub_name\n";
    $DEBUG and print STDERR "snippet  : $code_snippet\n";
    $self->{sub_name} = $sub_name;
    $self->{code_snippet}  = $code_snippet;
        
    $self->_parse_vars();
    $self->_parse_local_vars();
    $self->_transform_snippet();

     if ($syntax_check) {
         $self->_syntax_check();
     }
     return (  @$self{'return_sub_call','return_snippet'}   );
}

=head2 rename_subroutine($where,$old_name,$new_name,[$max_depth])

I<where> is one of:
  path-to-file
  path-to-directory
 
If I<where> is a directory then all Perl files (default is C<.pl>, C<.pm>,
and C<.pod> See the B<perl_file_extensions> method.) in that directory and its'
descendents (to I<max_depth> deep,) are searched.

Default for I<max_depth> is 0 -- just the directory itself;
I<max_depth> of 1 means the specified directory, and it's
immeadiate sub-directories; I<max_depth> of 2 means the specified directory,
it's sub-directories, and their sub-directrories, and so forth.
If you want to scan very deep, use a high number like 99.

If no matches are found then returns I<undef>, otherwise:

Returns a hashref that tells you which files you might want to change,
and for each file gives you the line numbers and proposed new text for that line.
The hashref looks like this,  where I<old_name>
was found on two lines in the first file and on one line in the second file:

 {
   ./path/to/file1.pl => [
                           { 11  => "if (myClass->newName($x)) {\n" },
                           { 27  => "my $result = myClass->newName($foo);\n"},
                         ],
   ./path/to/file2.pm => [
                           { 235 => "sub newName {\n"},
                         ],
 }

The keys are paths to individual files. The values are arraryrefs
containing hashrefs where the keys are the line numbers where I<old_name>
was found and the values are the proposed
new line, with I<old_name> changed to I<new_name>.

=cut

sub rename_subroutine {
    my $self           = shift;
    my $where          = shift;
    my $old_name       = shift;
    my $new_name       = shift;
    my $max_depth      = shift || 0;  # How many level to descend into directories

    return undef unless ($new_name and $old_name);

    $DEBUG and warn "Looking for $where in ",  getcwd(), "\n";
    my $found = {};  # hashref of file names
    if (-f $where){
        # it's a file or filehandle
        $found->{$where} = $self->_scan_file_for_string ($old_name,$new_name,$where);
    } elsif ( -d $where ) {
        # it's a directory or directory handle
        $self->_scan_directory_for_string($old_name,$new_name,$where,$found,$max_depth);
    } else {
        # uh oh. Should we allow it to be a code snippet?
        die "'$where' does not appear to be a file nor a directory."
    }
    return %$found ? $found : undef;
}

=head2 is_perlfile($filename)

Takes a filename or path and returns true if the file has one of the
extensions in B<perl_file_extensions>, otherwise returns false.

=cut
sub is_perlfile {
    my ($self,$filename) = @_;
    my ($name,$path,$suffix) = fileparse($filename,keys %{$self->perl_file_extensions});
    return $suffix;
}

=head1 OBJECT ACCESSORS

These object methods return various data structures that may be stored
in a B<Devel::Refactor> object. In some cases the method also allows
setting the property, e.g. B<perl_file_extensions>.

=cut

=head2 get_new_code

Returns the I<return_snippet> object property.

=cut
sub get_new_code{
    my $self = shift;
    
    return $self->{return_snippet};
}

=head2 get_eval_results

Returns the I<eval_err> object property.

=cut
sub get_eval_results{
    my $self = shift;
    
    return $self->{eval_err};
}


=head2 get_sub_call

Returns the I<return_sub_call> object property.

=cut
sub get_sub_call{
    my $self = shift;
    
    return $self->{return_sub_call};
}


=head2 get_scalars

Returns an array of the keys from I<scalar_vars> object property.
=cut
sub get_scalars {
    my $self = shift;

    return sort keys %{ $self->{scalar_vars} };
}

=head2 get_arrays

Returns an array of the keys from the I<array_vars> object property.
=cut
sub get_arrays {
    my $self = shift;

    return sort keys %{ $self->{array_vars} };
}

=head2 get_hashes

Returns an array of the keys from the I<hash_vars> object property.

=cut
sub get_hashes {
    my $self = shift;

    return sort keys %{ $self->{hash_vars} };
}

=head2 get_local_scalars

Returns an array of the keys from the I<local_scalars> object property.

=cut
sub get_local_scalars {
    my $self = shift;

    return sort keys %{ $self->{local_scalars} };
}

=head2 get_local_arrays

Returns an array of the keys from the I<local_arrays> object property.

=cut
sub get_local_arrays {
    my $self = shift;

    return sort keys %{ $self->{local_arrays} };
}

=head2 get_local_hashes

Returns an array of the keys from the I<local_hashes> object property.

=cut

sub get_local_hashes {
    my $self = shift;

    return sort keys %{ $self->{local_hashes} };
}

=head2 perl_file_extensions([$arrayref|$hashref])

Returns a hashref where the keys are regular expressions that match filename
extensions that we think are for Perl files. Default are C<.pl>,
C<.pm>, and C<.pod>

If passed a hashref then it replaces the current values for this object. The
keys should be regular expressions, e.g. C<\.cgi$>.

If passed an arrayref then the list of values are added as valid Perl
filename extensions. The list should be filename extensions, NOT regular expressions,
For example:

  my @additonal_filetypes = qw( .ipl .cgi );
  my $new_hash = $refactory->perl_file_extensions(\@additional_filetypes);
  # $new_hash = {
  #   '\.pl$'   => 1,
  #   '\.pm$'   => 1,
  #   '\.pod$'  => 1,
  #   '\.ipl$'  => 1,
  #   '\.cgi$'  => 1,
  #   '\.t$'    => 1,
  # }

=cut

sub perl_file_extensions {
    my($self,$args) = @_;
    if (ref $args eq 'HASH') {
        $self->{perl_file_extensions} = $args;
    } elsif (ref $args eq 'ARRAY') {
        map $self->{perl_file_extensions}->{"\\$_\$"} = 1 , @$args;
    }
    return $self->{perl_file_extensions};
}


=head1 TODO LIST

=over 2

=item Come up with a more uniform approach to B<ACCESSORS>.

=item Add more refactoring features, such as I<add_parameter>.

=item Add a SEE ALSO section with URLs for eclipse/EPIC, refactoring.com, etc.

=back

=cut

###################################################################################
############################## Utility Methods ####################################

sub _parse_vars {
    my $self = shift;

    my $var;
    my $hint;

    # find the variables
    while ( $self->{code_snippet} =~ /([\$\@]\w+?)(\W\W)/g ) {

        $var  = $1;
        $hint = $2;
        if ( $hint =~ /^{/ ) {    #}/ 
            $var =~ s/\$/\%/;
            $self->{hash_vars}->{$var}++;
        } elsif ( $hint =~ /^\[>/ ) {
            $var =~ s/\$/\@/;
            $self->{array_vars}->{$var}++;
        } elsif ( $var =~ /^\@/ ){
            $self->{array_vars}->{$var}++;
        } elsif ( $var =~ /^\%/ ) {
            $self->{hash_vars}->{$var}++;
        } else {
            $self->{scalar_vars}->{$var}++;
        }
    }

}

sub _parse_local_vars {
    my $self = shift;

    my $reg;
    my $reg2;
    my $reg3;   # To find loops variables declared in for and foreach

    # figure out which are declared in the snippet
    foreach my $var ( keys %{ $self->{scalar_vars} } ) {
        $reg  = "\\s*my\\s*\\$var\\s*[=;\(]";
        $reg2 = "\\s*my\\s*\\(.*?\\$var.*?\\)";
        $reg3 = "(?:for|foreach)\\s+my\\s*\\$var\\s*\\(";

        if ( $var =~ /(?:\$\d+$|\$[ab]$)/ ) {
            $self->{local_scalars}->{$var}++;
        } elsif ( $self->{code_snippet} =~ /$reg|$reg2/ ) {
            $self->{local_scalars}->{$var}++;
            # skip loop variables
            if ( $self->{code_snippet} =~ /$reg3/ ) {
                $self->{loop_scalars}->{$var}++;
            }
        }
    }
    foreach my $var ( keys %{ $self->{array_vars}} ) {
        $var =~ s/\$/\@/;
        $reg  = "\\s*my\\s*\\$var\\s*[=;\(]";
        $reg2 = "\\s*my\\s*\\(.*?\\$var.*?\\)";

        if ( $self->{code_snippet} =~ /$reg|$reg2/ ) {
            $self->{local_arrays}->{$var}++;
        }

    }
    foreach my $var ( keys %{ $self->{hash_vars}} ) {
        $var =~ s/\$/\%/;
        $reg  = "\\s*my\\s*\\$var\\s*[=;\(]";
        $reg2 = "\\s*my\\s*\\(.*?\\$var.*?\\)";

        if ( $self->{code_snippet} =~ /$reg|$reg2/ ) {
            $self->{local_hashes}->{$var}++;
        }
    }

}


sub _syntax_check{
    my $self = shift;
    my $tmp;
    
    my $eval_stmt = "my (". join ', ', @{$self->{parms}};
    $eval_stmt .= ");\n";
    $eval_stmt .= $self->get_sub_call();
    $eval_stmt .= $self->get_new_code();
    
    $self->{eval_code} = $eval_stmt;
    
    eval " $eval_stmt ";
    if ($@) {
        $self->{eval_err} = $@;
        
        my @errs = split /\n/, $self->{eval_err};
        my @tmp = split /\n/, $self->{return_snippet};
        my $line;
        foreach my $err (@errs){
            if ($err =~ /line\s(\d+)/){
                $line = ($1 - 3);
                $tmp[$line] .= " #<--- ".$err;
            }
        }
        $self->{return_snippet} = join "\n", @tmp;
        
    }
    
}

sub _transform_snippet {
    my $self = shift;

    my $reg;
    my $reg2;
    my $arref;
    my $href;
    # Create a sub call that accepts all non-locally declared
    # vars as parameters
    foreach my $parm ( keys %{$self->{scalar_vars} } ) {
        if ( !defined( $self->{local_scalars}->{$parm} ) ) {
            push @{$self->{parms}}, $parm;
        } else {
            # Don't return loop variables
            next if grep $parm eq $_, keys %{$self->{loop_scalars}};
            if ( $parm !~ /\$\d+$/ ) {
                push @{$self->{inner_retvals}}, $parm;
                push @{$self->{outer_retvals}}, $parm;
            }
        }
    }
    foreach my $parm ( keys %{ $self->{array_vars}} ) {
        $parm =~ s/\$/\@/;

        if ( !defined( $self->{local_arrays}->{$parm} ) ) {
            push @{$self->{parms}}, $parm;
            $reg2 = "\\$parm";
            ($arref = $parm) =~ s/\@/\$/;
            $self->{code_snippet} =~ s/$reg2/\@$arref/g;
            
            $parm =~ s/\@/\$/;
            $reg = "\\$parm\\[";

            $self->{code_snippet} =~ s/$reg/$parm\-\>\[/g;

                        
        } else {
            push @{$self->{inner_retvals}}, "\\$parm"; # \@array
            push @{$self->{outer_retvals}}, "$parm";
        }
    }
    foreach my $parm ( keys %{ $self->{hash_vars} }  ) {
        $parm =~ s/\$/\%/;

        if ( !defined( $self->{local_hashes}->{$parm} ) ) {
            push @{$self->{parms}}, $parm;
            $reg2 = "\\$parm";
            ($href = $parm) =~ s/\%/\$/;
            $self->{code_snippet} =~ s/$reg2/\%$href/g;
            
            $parm =~ s/\%/\$/;
            $reg = "\\$parm\\{";

            $self->{code_snippet} =~ s/$reg/$parm\-\>\{/g;
        } else {
            push @{$self->{inner_retvals}}, "\\$parm";  # \%hash
            push @{$self->{outer_retvals}}, "$parm";
        }
    }
    my $retval;
    my $return_call;
    my $tmp;
    
    $return_call .= "my (";
    $return_call .= join ', ', map {my $tmp; ($tmp = $_) =~ s/[\@\%](.*)/\$$1/; $tmp} sort @{$self->{outer_retvals}};
    $return_call .= ") = ".$self->{sub_name}." (";
    $return_call .= join ', ',
         map { ( $tmp = $_ ) =~ s/(\%|\@)(.*)/\\$1$2/; $tmp } @{$self->{parms}};
    $return_call .= ");\n";
    
    $retval  = "sub ".$self->{sub_name}." {\n";
    $retval .= join '', map {($tmp = $_) =~ tr/%@/$/; "    my $tmp = shift;\n" } @{$self->{parms}};
    $retval .= "\n" . $self->{code_snippet};
    $retval .= "\n    return (";
    $retval .= join ', ', sort @{$self->{inner_retvals}};
    $retval .= ");\n";
    $retval .= "}\n";

    # protect quotes and dollar signs
#    $retval =~ s/\"/\\"/g;
#    $retval =~ s/(\$)/\\$1/g;
    

    $self->{return_snippet} = $retval;
    $self->{return_sub_call} = $return_call;
}


# returns arrayref of hashrefs, or undef
sub _scan_file_for_string {
    my $self          = shift;
    my $old_name      = shift;
    my $new_name      = shift;
    my $file          = shift;

    my $fh;
    
    open $fh, "$file"
          || die("Could not open code file '$file' - $!");

    my $line_number = 0;
    my @lines;
    my $regex1 = '(\W)(' . $old_name . ')(\W)'; # Surrounded by non-word characters
    my $regex2 = "^$old_name(" . '\W)';  # At start of line
    while (<$fh>) {
        $line_number++;
        # Look for $old_name surrounded by non-word characters, or at start of line
        if (/$regex1/o or /$regex2/o) {
            my $new_line = $_;
            $new_line =~ s/$regex1/$1$new_name$3/g;
            $new_line =~ s/$regex2/$new_name$1/;
            my $hash = {$line_number => $new_line};
            push @lines, $hash;
        }
    }
    close $fh;
    return @lines ? \@lines : undef;
}

# Scan a directory, possibly recuring into sub-directories.
sub _scan_directory_for_string {
    my ($self,$old_name,$new_name,$where,$hash,$depth) = @_;
    my $dh;
    opendir $dh, $where ||
       die "Could not open directory '$where': $!";
    my @files = grep { $_ ne '.' and $_ ne '..' } readdir $dh;
    close $dh;
    $depth--;
    foreach my $file (@files) {
        $file = "$where/$file";  # add the directory back on to the path
        if (-f $file && $self->is_perlfile($file)) {
            $hash->{$file} = $self->_scan_file_for_string($old_name,$new_name,$file);
        }
        if (-d $file && $depth >= 0) {
            # It's a directory, so call this method on the directory.
            $self->_scan_directory_for_string($old_name,$new_name,$file,$hash,$depth);
        }
    }
    return $hash;
}


1; # File must return true when compiled. Keep Perl happy, snuggly and warm.

__END__

=head1 AUTHOR

Scott Sotka, E<lt>ssotka@barracudanetworks.comE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright 2005 by Scott Sotka

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself. 

=cut
