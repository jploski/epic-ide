package Devel::Refactor;

use strict;
use warnings;

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

our $VERSION = '0.03';

our $DEBUG = 0;
# Preloaded methods go here.


sub new() {
    my $class        = shift;
    my $sub_name     = shift;
    my $code_snippet = shift;
    my $syntax_check = shift;
    $DEBUG           = shift;

    $DEBUG and print STDERR "sub name : $sub_name\n";
    $DEBUG and print STDERR "snippet  : $code_snippet\n";
    
    my $self = {
        sub_name        => $sub_name,
        code_snippet    => $code_snippet,
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
        retvals         => [],
    };

    bless $self, $class;

    $self->_parse_vars();
    $self->_parse_local_vars();
    $self->_transform_snippet();
    
    if ($syntax_check){
        $self->_syntax_check();
    }
    
    return $self;
}

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

sub get_scalars {
    my $self = shift;

    return sort keys %{ $self->{scalar_vars} };
}

sub get_arrays {
    my $self = shift;

    return sort keys %{ $self->{array_vars} };
}

sub get_hashes {
    my $self = shift;

    return sort keys %{ $self->{hash_vars} };
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

sub get_local_scalars {
    my $self = shift;

    return sort keys %{ $self->{local_scalars} };
}

sub get_local_arrays {
    my $self = shift;

    return sort keys %{ $self->{local_arrays} };
}

sub get_local_hashes {
    my $self = shift;

    return sort keys %{ $self->{local_hashes} };
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
            push @{$self->{retvals}}, $parm if ( $parm !~ /\$\d+$/ );
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
            push @{$self->{retvals}}, "\\$parm"; # \@array
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
            push @{$self->{retvals}}, "\\$parm";  # \%hash
        }
    }
    my $retval;
    my $return_call;
    my $tmp;
    
    $return_call .= "my (";
    $return_call .= join ', ', map {my $tmp; ($tmp = $_) =~ s/[\@\%](.*)/\$$1/; $tmp} sort @{$self->{retvals}};
    $return_call .= ") = ".$self->{sub_name}." (";
    $return_call .= join ', ',
         map { ( $tmp = $_ ) =~ s/(\%|\@)(.*)/\\$1$2/; $tmp } @{$self->{parms}};
    $return_call .= ");\n";
    
    $retval  = "sub ".$self->{sub_name}." {\n";
    $retval .= join '', map {($tmp = $_) =~ tr/%@/$/; "    my $tmp = shift;\n" } @{$self->{parms}};
    $retval .= "\n" . $self->{code_snippet};
    $retval .= "\n    return (";
    $retval .= join ', ', sort @{$self->{retvals}};
    $retval .= ");\n";
    $retval .= "}\n";

    # protect quotes and dollar signs
#    $retval =~ s/\"/\\"/g;
#    $retval =~ s/(\$)/\\$1/g;
    

    $self->{return_snippet} = $retval;
    $self->{return_sub_call} = $return_call;
}

sub get_new_code{
    my $self = shift;
    
    return $self->{return_snippet};
}

sub get_sub_call{
    my $self = shift;
    
    return $self->{return_sub_call};
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

sub get_eval_results{
    my $self = shift;
    
    return $self->{eval_err};
}

1;
__END__


=head1 NAME

Devel::Refactor - Perl extension for refactoring Perl code.

=head1 SYNOPSIS

  use Devel::Refactor;
  
  my $refactory = Dev::Refactor->new($sub_name, $code_snippet);
  
  my $new_sub_code = $refactory->get_new_code();
  my $sub_call     = $refactory->get_sub_call();

=head1 ABSTRACT

Perl module that facilitates refactoring Perl code.  

=head1 DESCRIPTION

The Devel::Refactor module is for code refactoring.  Pass it
a snippet of Perl code that belongs in its own subroutine as
well as a name for that sub.  It figures out which variables
need to be passed into the sub, and which variables might be
passed back.  It then produces the sub along with a call to
the sub.

Hashes and arrays within the code snippet are converted to 
hashrefs and arrayrefs.  

Included in the examples directory is a script for use in KDE
under Linux.  The script gets its code snippet from the KDE 
clipboard and returns the transformed code the same way.  The
new sub name is prompted for via STDIN.

=head1 AUTHOR

Scott Sotka, E<lt>ssotka@barracudanetworks.comE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright 2003 by Scott Sotka

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself. 

=cut
