# $Id: Twig.pm.slow,v 1.229 2005/08/05 10:15:21 mrodrigu Exp $
#
# Copyright (c) 1999-2004 Michel Rodriguez
# All rights reserved.
#
# This program is free software; you can redistribute it and/or
# modify it under the same terms as Perl itself.
#

# This is created in the caller's space
BEGIN
{ sub ::PCDATA { '#PCDATA' } 
  sub ::CDATA  { '#CDATA'  } 
}


######################################################################
package XML::Twig;
######################################################################

require 5.004;
use strict; 

use vars qw($VERSION @ISA %valid_option);
use Carp;

*isa = \&UNIVERSAL::isa;

#start-extract twig_global

# constants: element types
use constant (PCDATA  => '#PCDATA');
use constant (CDATA   => '#CDATA');
use constant (PI      => '#PI');
use constant (COMMENT => '#COMMENT');
use constant (ENT     => '#ENT');

# element classes
use constant (ELT     => '#ELT');
use constant (TEXT    => '#TEXT');

# element properties
use constant (ASIS    => '#ASIS');
use constant (EMPTY   => '#EMPTY');

#end-extract twig_global

# used in parseurl to set the buffer size to the same size as in XML::Parser::Expat
use constant (BUFSIZE => 32768);


# used to store the gi's
my %gi2index;   # gi => index
my @index2gi;   # list of gi's
my $SPECIAL_GI; # first non-special gi;
my %base_ent;   # base entity character => replacement

# flag, set to true if the weaken sub is available
use vars qw( $weakrefs);

#start-extract twig_global
my $REG_NAME       = q{(?:(?:[^\W\d]|[:#])(?:[\w.-]*:)?[\w.-]*)};     # xml name
my $REG_NAME_W     = q{(?:(?:[^\W\d_]|[:#])(?:[\w.-]*:)?[\w.-]*|\*)}; # name or wildcard (* or '')
my $REG_REGEXP     = q{(?:/(?:[^\\/]|\\.)*/[eimsox]*)};               # regexp
my $REG_REGEXP_EXP = q{(?:(?:[^\\/]|\\.)*)};                          # content of a regexp
my $REG_REGEXP_MOD = q{(?:[eimso]*)};                                 # regexp modifiers
my $REG_MATCH      = q{[!=]~};                                        # match (or not)
my $REG_STRING     = q{(?:"(?:[^\\"]|\\.)*"|'(?:[^\\']|\\.)*')};      # string (simple or double quoted)
my $REG_NUMBER     = q{(?:\d+(?:\.\d*)?|\.\d+)};                      # number
my $REG_VALUE      = qq{(?:$REG_STRING|$REG_NUMBER)};                 # value
my $REG_OP         = q{=|==|!=|>|<|>=|<=|eq|ne|lt|gt|le|ge};          # op

# simple for now
my $REG_PREDICATE  = q{\[\s*\@$REG_NAME\s*(?:$REG_MATCH\s*$REG_REGEXP|$REG_OP\s*$REG_VALUE)\s*\])}; # attribute match


#end-extract twig_global

my $parser_version;
my( $FB_HTMLCREF, $FB_XMLCREF);


BEGIN
{ 
$VERSION = '3.18';

use XML::Parser;
my $needVersion = '2.23';
$parser_version= $XML::Parser::VERSION;
croak "need at least XML::Parser version $needVersion" unless $parser_version >= $needVersion;

if( $] >= 5.008) 
  { eval "use Encode qw( :all)";
    $FB_XMLCREF  = 0x0400; # Encode::FB_XMLCREF;
    $FB_HTMLCREF = 0x0200; # Encode::FB_HTMLCREF;
  }

# test whether we can use weak references
# set local empty signal handler to trap error messages
{ local $SIG{__DIE__};
  if( eval( 'require Scalar::Util') && defined( &Scalar::Util::weaken) ) 
    { import Scalar::Util( 'weaken'); $weakrefs= 1; }
  elsif( eval( 'require WeakRef')) 
    { import WeakRef; $weakrefs= 1;                 }
  else  
    { $weakrefs= 0;                                 } 
}

import XML::Twig::Elt;
import XML::Twig::Entity;
import XML::Twig::Entity_list;

# used to store the gi's
# should be set for each twig really, at least when there are several
# the init ensures that special gi's are always the same

# gi => index
# do NOT use => or the constants become quoted!
%XML::Twig::gi2index=( PCDATA, 0, CDATA, 1, PI, 2, COMMENT, 3, ENT, 4); 
# list of gi's
@XML::Twig::index2gi=( PCDATA, CDATA, PI, COMMENT, ENT);

# gi's under this value are special 
$XML::Twig::SPECIAL_GI= @XML::Twig::index2gi;

%XML::Twig::base_ent= ( '>' => '&gt;', '<' => '&lt;', '&' => '&amp;', "'" => '&apos;', '"' => '&quot;',);

# now set some aliases
*find_nodes           = *get_xpath;               # same as XML::XPath
*findnodes            = *get_xpath;               # same as XML::LibXML
*getElementsByTagName = *descendants;
*descendants_or_self  = *descendants;             # valid in XML::Twig, not in XML::Twig::Elt
*find_by_tag_name     = *descendants;
*getElementById       = *elt_id;
*getEltById           = *elt_id;
*toString             = *sprint;
}

@ISA = qw(XML::Parser);

# fake gi's used in twig_handlers and start_tag_handlers
my $ALL    = '_all_';     # the associated function is always called
my $DEFAULT= '_default_'; # the function is called if no other handler has been

# some defaults
my $COMMENTS_DEFAULT= 'keep';
my $PI_DEFAULT      = 'keep';


# handlers used in regular mode
my %twig_handlers=( Start      => \&_twig_start, 
                    End        => \&_twig_end, 
                    Char       => \&_twig_char, 
                    Entity     => \&_twig_entity, 
                    XMLDecl    => \&_twig_xmldecl, 
                    Doctype    => \&_twig_doctype, 
                    Element    => \&_twig_element, 
                    Attlist    => \&_twig_attlist, 
                    CdataStart => \&_twig_cdatastart, 
                    CdataEnd   => \&_twig_cdataend, 
                    Proc       => \&_twig_pi,
                    Comment    => \&_twig_comment,
                    Default    => \&_twig_default,
      );

# handlers used when twig_roots is used and we are outside of the roots
my %twig_handlers_roots=
  ( Start      => \&_twig_start_check_roots, 
    End        => \&_twig_end_check_roots, 
    Doctype    => \&_twig_doctype, 
    Char       => undef, Entity     => undef, XMLDecl    => \&_twig_xmldecl, 
    Element    => undef, Attlist    => undef, CdataStart => undef, 
    CdataEnd   => undef, Proc       => undef, Comment    => undef, 
    Proc       => \&_twig_pi_check_roots,
    Default    =>  sub {}, # hack needed for XML::Parser 2.27
  );

# handlers used when twig_roots and print_outside_roots are used and we are
# outside of the roots
my %twig_handlers_roots_print_2_30=
  ( Start      => \&_twig_start_check_roots, 
    End        => \&_twig_end_check_roots, 
    Char       => \&_twig_print, 
    # I have no idea why I should not be using this handler!
    Entity     => \&_twig_print_entity, 
    XMLDecl    => \&_twig_print,
    Doctype   =>  \&_twig_print_doctype, # because recognized_string is broken here
    # Element    => \&_twig_print, Attlist    => \&_twig_print, 
    CdataStart => \&_twig_print, CdataEnd   => \&_twig_print, 
    Proc       => \&_twig_pi_check_roots, Comment    => \&_twig_print, 
    Default    => \&_twig_print_check_doctype,
  );

# handlers used when twig_roots, print_outside_roots and keep_encoding are used
# and we are outside of the roots
my %twig_handlers_roots_print_original_2_30=
  ( Start      => \&_twig_start_check_roots, 
    End        => \&_twig_end_check_roots, 
    Char       => \&_twig_print_original, 
    # I have no idea why I should not be using this handler!
    #Entity     => \&_twig_print_original, 
    ExternEnt  => \&_twig_print_entity,
    XMLDecl    => \&_twig_print_original, 
    Doctype    => \&_twig_print_original_doctype,  # because original_string is broken here
    Element    => \&_twig_print_original, Attlist   => \&_twig_print_original,
    CdataStart => \&_twig_print_original, CdataEnd  => \&_twig_print_original,
    Proc       => \&_twig_pi_check_roots, Comment   => \&_twig_print_original,
    Default    => \&_twig_print_original_check_doctype, 
  );

# handlers used when twig_roots and print_outside_roots are used and we are
# outside of the roots
my %twig_handlers_roots_print_2_27=
  ( Start      => \&_twig_start_check_roots, 
    End        => \&_twig_end_check_roots, 
    Char       => \&_twig_print, 
    # I have no idea why I should not be using this handler!
    #Entity     => \&_twig_print, 
    XMLDecl    => \&_twig_print, Doctype    => \&_twig_print, 
    CdataStart => \&_twig_print, CdataEnd   => \&_twig_print, 
    Proc       => \&_twig_pi_check_roots, Comment    => \&_twig_print, 
    Default    => \&_twig_print, 
  );

# handlers used when twig_roots, print_outside_roots and keep_encoding are used
# and we are outside of the roots
my %twig_handlers_roots_print_original_2_27=
  ( Start      => \&_twig_start_check_roots, 
    End        => \&_twig_end_check_roots, 
    Char       => \&_twig_print_original, 
    # for some reason original_string is wrong here 
    # this can be a problem if the doctype includes non ascii characters
    XMLDecl    => \&_twig_print, Doctype    => \&_twig_print,
    # I have no idea why I should not be using this handler!
    Entity     => \&_twig_print, 
    #Element    => undef, Attlist   => undef,
    CdataStart => \&_twig_print_original, CdataEnd  => \&_twig_print_original,
    Proc       => \&_twig_pi_check_roots, Comment   => \&_twig_print_original,
    Default    => \&_twig_print, #  _twig_print_original does not work
  );


my %twig_handlers_roots_print= $parser_version > 2.27 
                               ? %twig_handlers_roots_print_2_30 
                               : %twig_handlers_roots_print_2_27; 
my %twig_handlers_roots_print_original= $parser_version > 2.27 
                               ? %twig_handlers_roots_print_original_2_30 
                               : %twig_handlers_roots_print_original_2_27; 


# handlers used when the finish_print method has been called
my %twig_handlers_finish_print=
  ( Start      => \&_twig_print, 
    End        => \&_twig_print, Char       => \&_twig_print, 
    Entity     => \&_twig_print, XMLDecl    => \&_twig_print, 
    Doctype    => \&_twig_print, Element    => \&_twig_print, 
    Attlist    => \&_twig_print, CdataStart => \&_twig_print, 
    CdataEnd   => \&_twig_print, Proc       => \&_twig_print, 
    Comment    => \&_twig_print, Default    => \&_twig_print, 
  );

# handlers used when the finish_print method has been called and the keep_encoding
# option is used
my %twig_handlers_finish_print_original=
  ( Start      => \&_twig_print_original, End      => \&_twig_print_end_original, 
    Char       => \&_twig_print_original, Entity   => \&_twig_print_original, 
    XMLDecl    => \&_twig_print_original, Doctype  => \&_twig_print_original, 
    Element    => \&_twig_print_original, Attlist  => \&_twig_print_original, 
    CdataStart => \&_twig_print_original, CdataEnd => \&_twig_print_original, 
    Proc       => \&_twig_print_original, Comment  => \&_twig_print_original, 
    Default    => \&_twig_print_original, 
  );

# handlers used whithin ignored elements
my %twig_handlers_ignore=
  ( Start      => \&_twig_ignore_start, 
    End        => \&_twig_ignore_end, 
    Char       => undef, Entity     => undef, XMLDecl    => undef, 
    Doctype    => undef, Element    => undef, Attlist    => undef, 
    CdataStart => undef, CdataEnd   => undef, Proc       => undef, 
    Comment    => undef, Default    => undef,
  );


# those handlers are only used if the entities are NOT to be expanded
my %twig_noexpand_handlers= ( Default => \&_twig_default );

my @saved_default_handler;

my $ID= 'id'; # default value, set by the Id argument

# all allowed options
%valid_option=
    ( # XML::Twig options
      TwigHandlers          => 1, Id                    => 1,
      TwigRoots             => 1, TwigPrintOutsideRoots => 1,
      StartTagHandlers      => 1, EndTagHandlers        => 1,
      ForceEndTagHandlersUsage => 1,
      DoNotChainHandlers    => 1,
      IgnoreElts            => 1,
      Index                 => 1,
      CharHandler           => 1, 
      KeepEncoding          => 1, DoNotEscapeAmpInAtts  => 1,
      ParseStartTag         => 1, KeepAttsOrder         => 1,
      LoadDTD               => 1, DTDHandler            => 1,
      DoNotOutputDTD        => 1, NoProlog              => 1,
      ExpandExternalEnts    => 1,
      DiscardSpaces         => 1, KeepSpaces            => 1, 
      DiscardSpacesIn       => 1, KeepSpacesIn          => 1, 
      PrettyPrint           => 1, EmptyTags             => 1, 
      Comments              => 1, Pi                    => 1, 
      OutputFilter          => 1, InputFilter           => 1,
      OutputTextFilter      => 1, 
      OutputEncoding        => 1, 
      RemoveCdata           => 1,
      EltClass              => 1,
      MapXmlns              => 1, KeepOriginalPrefix    => 1,
      # XML::Parser options
      ErrorContext          => 1, ProtocolEncoding      => 1,
      Namespaces            => 1, NoExpand              => 1,
      Stream_Delimiter      => 1, ParseParamEnt         => 1,
      NoLWP                 => 1, Non_Expat_Options     => 1,
      Xmlns                 => 1,
    );

# predefined input and output filters
use vars qw( %filter);
%filter= ( html       => \&html_encode,
           safe       => \&safe_encode,
           safe_hex   => \&safe_encode_hex,
         );
1;
sub new
  { my ($class, %args) = @_;
    my $handlers;

    # change all nice_perlish_names into nicePerlishNames
    %args= _normalize_args( %args);

    # check options
    unless( $args{MoreOptions})
      { foreach my $arg (keys %args)
        { carp "invalid option $arg" unless $valid_option{$arg}; }
      }
     
    # a twig is really an XML::Parser
    # my $self= XML::Parser->new(%args);
    my $self;
    $self= XML::Parser->new(%args);   
    
    bless $self, $class;

    if( exists $args{TwigHandlers})
      { $handlers= $args{TwigHandlers};
        $self->setTwigHandlers( $handlers);
        delete $args{TwigHandlers};
      }

    # take care of twig-specific arguments
    if( exists $args{StartTagHandlers})
      { $self->setStartTagHandlers( $args{StartTagHandlers});
        delete $args{StartTagHandlers};
      }

    if( exists $args{DoNotChainHandlers})
      { $self->{twig_do_not_chain_handlers}=  $args{DoNotChainHandlers}; }

    if( exists $args{IgnoreElts})
      { $self->setIgnoreEltsHandlers( $args{IgnoreElts});
        delete $args{IgnoreElts};
      }

    if( exists $args{Index})
      { my $index= $args{Index};
        # we really want a hash name => path, we turn an array into a hash if necessary
        if( ref( $index) eq 'ARRAY')
          { my %index= map { $_ => $_ } @$index;
            $index= \%index;
          }
        while( my( $name, $exp)= each %$index)
          { $self->setTwigHandler( $exp, sub { push @{$_[0]->{_twig_index}->{$name}}, $_; 1; }); }
      }

    $self->{twig_elt_class}= $args{EltClass} || 'XML::Twig::Elt';
    if( exists( $args{EltClass})) { delete $args{EltClass}; }

    if( exists( $args{MapXmlns}))
      { $self->{twig_map_xmlns}=  $args{MapXmlns};
        $self->{Namespaces}=1;
        delete $args{MapXmlns};
      }

    if( exists( $args{KeepOriginalPrefix}))
      { $self->{twig_keep_original_prefix}= $args{KeepOriginalPrefix};
        delete $args{KeepOriginalPrefix};
      }

    $self->{twig_dtd_handler}= $args{DTDHandler};
    delete $args{DTDHandler};

    if( $args{CharHandler})
      { $self->setCharHandler( $args{CharHandler});
        delete $args{CharHandler};
      }

    if( $args{LoadDTD})
      { $self->{twig_read_external_dtd}= 1;
        delete $args{LoadDTD};
      }
      
    if( $args{ExpandExternalEnts})
      { $self->set_expand_external_entities( 1);
        $self->{twig_read_external_dtd}= 1; # implied by ExpandExternalEnts
        delete $args{LoadDTD};
        delete $args{ExpandExternalEnts};
      }

    if( $args{DoNotEscapeAmpInAtts})
      { $self->set_do_not_escape_amp_in_atts( 1); 
        $self->{twig_do_not_escape_amp_in_atts}=1;
      }
    else
      { $self->set_do_not_escape_amp_in_atts( 0); 
        $self->{twig_do_not_escape_amp_in_atts}=0;
      }

    # deal with TwigRoots argument, a hash of elements for which
    # subtrees will be built (and associated handlers)
     
    if( $args{TwigRoots})
      { $self->setTwigRoots( $args{TwigRoots});
        delete $args{TwigRoots}; 
      }
    
    if( $args{EndTagHandlers})
      { unless ($self->{twig_roots} || $args{ForceEndTagHandlersUsage})
          { croak "you should not use EndTagHandlers without TwigRoots\n",
                  "if you want to use it anyway, normally because you have ",
                  "a start_tag_handlers that calls 'ignore' and you want to ",
                  "call an ent_tag_handlers at the end of the element, then ",
                  "pass 'force_end_tag_handlers_usage => 1' as an argument ",
                  "to new";
          }
                  
        $self->setEndTagHandlers( $args{EndTagHandlers});
        delete $args{EndTagHandlers};
      }
      
    if( $args{TwigPrintOutsideRoots})
      { croak "cannot use TwigPrintOutsideRoots without TwigRoots"
          unless( $self->{twig_roots});
        # if the arg is a filehandle then store it
        if( _is_fh( $args{TwigPrintOutsideRoots}) )
          { $self->{twig_output_fh}= $args{TwigPrintOutsideRoots}; }
        $self->{twig_default_print}= $args{TwigPrintOutsideRoots};
      }

    if( $args{PrettyPrint})
      { $self->set_pretty_print( $args{PrettyPrint}); }

    if( $args{EmptyTags})
      { $self->set_empty_tag_style( $args{EmptyTags}); }

    # space policy
    if( $args{KeepSpaces})
      { croak "cannot use both keep_spaces and discard_spaces" if( $args{DiscardSpaces});
        croak "cannot use both keep_spaces and keep_spaces_in" if( $args{KeepSpacesIn});
        $self->{twig_keep_spaces}=1;
        delete $args{KeepSpaces}; 
      }
    if( $args{DiscardSpaces})
      { croak "cannot use both discard_spaces and keep_spaces_in" if( $args{KeepSpacesIn});
        $self->{twig_discard_spaces}=1; 
        delete $args{DiscardSpaces}; 
      }
    if( $args{KeepSpacesIn})
      { croak "cannot use both keep_spaces_in and discard_spaces_in" if( $args{DiscardSpacesIn});
        $self->{twig_discard_spaces}=1; 
        $self->{twig_keep_spaces_in}={}; 
        my @tags= @{$args{KeepSpacesIn}}; 
        foreach my $tag (@tags) { $self->{twig_keep_spaces_in}->{$tag}=1; } 
        delete $args{KeepSpacesIn}; 
      }
    if( $args{DiscardSpacesIn})
      { $self->{twig_keep_spaces}=1; 
        $self->{twig_discard_spaces_in}={}; 
        my @tags= @{$args{DiscardSpacesIn}};
        foreach my $tag (@tags) { $self->{twig_discard_spaces_in}->{$tag}=1; } 
        delete $args{DiscardSpacesIn}; 
      }
    # discard spaces by default 
    $self->{twig_discard_spaces}= 1 unless(  $self->{twig_keep_spaces});

    $args{Comments}||= $COMMENTS_DEFAULT;
    if( $args{Comments} eq 'drop')       { $self->{twig_keep_comments}= 0;    }
    elsif( $args{Comments} eq 'keep')    { $self->{twig_keep_comments}= 1;    }
    elsif( $args{Comments} eq 'process') { $self->{twig_process_comments}= 1; }
    else { croak "wrong value for comments argument: '$args{Comments}' (should be 'drop', 'keep' or 'process')"; }
    delete $args{Comments};

    $args{Pi}||= $PI_DEFAULT;
    if( $args{Pi} eq 'drop')       { $self->{twig_keep_pi}= 0;    }
    elsif( $args{Pi} eq 'keep')    { $self->{twig_keep_pi}= 1;    }
    elsif( $args{Pi} eq 'process') { $self->{twig_process_pi}= 1; }
    else { croak "wrong value for pi argument: '$args{Pi}' (should be 'drop', 'keep' or 'process')"; }
    delete $args{Pi};

    if( $args{KeepEncoding})
      { $self->{twig_keep_encoding}= $args{KeepEncoding};
        # set it in XML::Twig::Elt so print functions know what to do
        $self->set_keep_encoding( 1); 
        $self->{parse_start_tag}= $args{ParseStartTag} || \&_parse_start_tag; 
        delete $args{ParseStartTag} if defined( $args{ParseStartTag}) ;
        delete $args{KeepEncoding};
        $self->{NoExpand}= 1;
      }
    else
      { $self->set_keep_encoding( 0);  
        $self->{parse_start_tag}= $args{ParseStartTag} if( $args{ParseStartTag}); 
      }

    if( $args{OutputFilter})
      { $self->set_output_filter( $args{OutputFilter}); 
        delete $args{OutputFilter};
      }
    else
      { $self->set_output_filter( 0); }

    if( $args{RemoveCdata})
      { $self->set_remove_cdata( $args{RemoveCdata}); 
        delete $args{RemoveCdata}; 
      }
    else
      { $self->set_remove_cdata( 0); }

    if( $args{OutputTextFilter})
      { $self->set_output_text_filter( $args{OutputTextFilter}); 
        delete $args{OutputTextFilter};
      }
    else
      { $self->set_output_text_filter( 0); }


    if( exists $args{KeepAttsOrder})
      { $self->{keep_atts_order}= $args{KeepAttsOrder};
        if( eval 'require Tie::IxHash') 
          { import Tie::IxHash; 
            $self->set_keep_atts_order(  $self->{keep_atts_order}); 
          }
        else 
          { carp "Tie::IxHash not available, option  keep_atts_order not allowed"; }
      }
    else
      { $self->set_keep_atts_order( 0); }

    if( my $output_encoding= $args{OutputEncoding})
      { $self->set_output_encoding( $output_encoding);
        delete $args{OutputFilter};
      }

    if( $args{InputFilter})
      { $self->set_input_filter(  $args{InputFilter}); 
        delete  $args{InputFilter}; 
      }

    if( exists $args{Id}) { $ID= $args{Id}; delete $args{ID}; }

    if( $args{NoExpand})
      { $self->setHandlers( %twig_noexpand_handlers);
        $self->{twig_no_expand}=1;
      }

    if( $args{NoProlog})
      { $self->{no_prolog}= 1; 
        delete $args{NoProlog}; 
      }

    if( $args{DoNotOutputDTD})
      { $self->{no_dtd_output}= 1; 
        delete $args{DoNotOutputDTD}; 
      }

    # set handlers
    if( $self->{twig_roots})
      { if( $self->{twig_default_print})
          { if( $self->{twig_keep_encoding})
              { $self->setHandlers( %twig_handlers_roots_print_original); }
            else
              { $self->setHandlers( %twig_handlers_roots_print);  }
          }
        else
          { $self->setHandlers( %twig_handlers_roots); }
      }
    else
      { $self->setHandlers( %twig_handlers); }

    # XML::Parser::Expat does not like these handler to be set. So in order to 
    # use the various sets of handlers on XML::Parser or XML::Parser::Expat
    # objects when needed, these ones have to be set only once, here, at 
    # XML::Parser level
    $self->setHandlers( Init => \&_twig_init, Final => \&_twig_final);

    $self->{twig_entity_list}= XML::Twig::Entity_list->new; 

    $self->{twig_id}= $ID; 
    $self->{twig_stored_spaces}='';

    $self->{twig}= $self;
    weaken( $self->{twig}) if( $weakrefs);

    return $self;
  }


sub parseurl
  { my $t= shift;
    return $t->_parseurl( 0, @_);
  }

sub safe_parseurl
  { my $t= shift;
    return $t->_parseurl( 1, @_);
  }

# I should really add extra options to allow better configuration of the 
# LWP::UserAgent object
# this method forks: 
#   - the child gets the data and copies it to the pipe,
#   - the parent reads the stream and sends it to XML::Parser
# the data is cut it chunks the size of the XML::Parser::Expat buffer
# the method returns the twig and the status
sub _parseurl
  { my( $t, $safe, $url, $agent)= @_;
    pipe( README, WRITEME) or croak  "cannot create connected pipes: $!";
    if( my $pid= fork)
      { # parent code: parse the incoming file
        close WRITEME; # no need to write
        my $result= $safe ? $t->safe_parse( \*README) : $t->parse( \*README);
        close README;
        return $@ ? 0 : $t;
      }
    else
     { # child
        close README; # no need to read
        require LWP;  # so we can get LWP::UserAgent and HTTP::Request
        $|=1;
        $agent    ||= LWP::UserAgent->new;
        my $request  = HTTP::Request->new( GET => $url);
        # _pass_url_content is called with chunks of data the same size as
        # the XML::Parser buffer 
        my $response = $agent->request( $request, 
                         sub { _pass_url_content( \*WRITEME, @_); }, BUFSIZE);
        $response->is_success or croak "$url ", $response->message;
        close WRITEME;
        CORE::exit(); # CORE is there for mod_perl (which redefines exit)
      }
  }

# get the (hopefully!) XML data from the URL and 
sub _pass_url_content
  { my( $fh, $data, $response, $protocol)= @_;
    print {$fh} $data;
  }

sub add_options
  { my %args= map { $_, 1 } @_;
    %args= _normalize_args( %args);
    foreach (keys %args) { $valid_option{$_}++; } 
  }

sub _twig_store_internal_dtd
  { 
    my( $p, $string)= @_;
    my $t= $p->{twig};
    $string= $p->original_string() if( $t->{twig_keep_encoding});
    $t->{twig_doctype}->{internal} .= $string;
  }

sub _twig_stop_storing_internal_dtd
  { my $p= shift;
    if( @saved_default_handler && defined $saved_default_handler[1])
      { $p->setHandlers( @saved_default_handler); }
    else
      { my $t= $p->{twig};
        $p->setHandlers( Default => undef);
      }
    $p->{twig}->{twig_doctype}->{internal}=~ s{^\s*\[}{};
    $p->{twig}->{twig_doctype}->{internal}=~ s{\]\s*$}{};
  }


sub _normalize_args
  { my %normalized_args;
    while( my $key= shift )
      { $key= join '', map { ucfirst } split /_/, $key;
        #$key= "Twig".$key unless( substr( $key, 0, 4) eq 'Twig');
        $normalized_args{$key}= shift ;
      }
    return %normalized_args;
  }    

sub _is_fh { return unless $_[0]; return $_[0] if( isa( $_[0], 'GLOB') || isa( $_[0], 'IO::Scalar')); }

sub _set_handler
  { my( $handlers, $path, $handler)= @_;

    #$handlers ||= {}; # create the handlers struct if necessary

    my $prev_handler= $handlers->{handlers}->{$path} || undef;

       _set_gi_handler              ( $handlers, $path, $handler, $prev_handler)
    || _set_path_handler            ( $handlers, $path, $handler, $prev_handler)
    || _set_subpath_handler         ( $handlers, $path, $handler, $prev_handler)
    || _set_attribute_handler       ( $handlers, $path, $handler, $prev_handler)
    || _set_star_att_handler        ( $handlers, $path, $handler, $prev_handler)
    || _set_star_att_regexp_handler ( $handlers, $path, $handler, $prev_handler)
    || _set_string_handler          ( $handlers, $path, $handler, $prev_handler)
    || _set_attribute_regexp_handler( $handlers, $path, $handler, $prev_handler)
    || _set_string_regexp_handler   ( $handlers, $path, $handler, $prev_handler)
    || _set_pi_handler              ( $handlers, $path, $handler, $prev_handler)
    || _set_level_handler           ( $handlers, $path, $handler, $prev_handler)
    || _set_regexp_handler          ( $handlers, $path, $handler, $prev_handler)
    || croak "unrecognized expression in handler: '$path'";


    # this both takes care of the simple (gi) handlers and store
    # the handler code reference for other handlers
    $handlers->{handlers}->{$path}= $handler;

    return $prev_handler;
  }


sub _set_gi_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    if( $path =~ m{^\s*($REG_NAME)\s*$}o )
      { my $gi= $1;
        $handlers->{handlers}->{gi}->{$gi}= $handler; 
        return 1;
      }
    else 
      { return 0; }
  }

sub _set_path_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    if( $path=~ m{^\s*(?:/$REG_NAME)*/($REG_NAME)\s*$}o)
      { # a full path has been defined
        # update the path_handlers count, knowing that
        # either the previous or the new handler can be undef
        $handlers->{path_handlers}->{gi}->{$1}-- if( $prev_handler);
        if( $handler)
         { $handlers->{path_handlers}->{gi}->{$1}++;
           $handlers->{path_handlers}->{path}->{$path}= $handler;
         }
        return 1;
      }
    else 
      { return 0; }
  }

sub _set_subpath_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    if( $path=~ m{^\s*(?:$REG_NAME/)+($REG_NAME)\s*$}o)
      { # a partial path has been defined
        # $1 is the "final" gi
        $handlers->{subpath_handlers}->{gi}->{$1}-- if( $prev_handler);
        if( $handler)
         { $handlers->{subpath_handlers}->{gi}->{$1}++;
           $handlers->{subpath_handlers}->{path}->{$path}= $handler;
         }
        return 1;
      }
    else 
      { return 0; }
  }


sub _set_attribute_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for attribute conditions
    if( $path=~ m{^\s*($REG_NAME)          # elt
                 \s*\[\s*\@                #    [@
                 ($REG_NAME)\s*            #      att
                 (?:=\s*($REG_STRING)\s*)? #           = value (optional)         
                 \]\s*$}xo)                #                             ]  
      { my( $gi, $att, $val)= ($1, $2, $3);
        $val= substr( $val, 1, -1) if( defined $val); # remove the quotes
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{attcond_handlers_exp}->{$gi}})
             { if( ($exp->{att} eq $att) && ( _eq( $exp->{val}, $val)) )
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler; }
                   else          # remove the handler
                     { $handlers->{attcond_handlers}->{$gi}--;
                       splice( @{$handlers->{attcond_handlers_exp}->{$gi}}, $i, 1);
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            $handlers->{attcond_handlers}->{$gi}++;
            my $exp={att => $att, val => $val, handler => $handler};
            $handlers->{attcond_handlers_exp}->{$gi} ||= [];
            push @{$handlers->{attcond_handlers_exp}->{$gi}}, $exp;
          }
        return 1;
      }
    else 
      { return 0; }
  }


sub _set_attribute_regexp_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for attribute regexp conditions
    if( $path=~ m{^\s*($REG_NAME)     # elt
                 \s*\[\s*\@           #    [@
                 ($REG_NAME)          #      att
                 \s*=~\s*             #          =~
                 /($REG_REGEXP_EXP)/  #             /regexp/
                 ($REG_REGEXP_MOD)    #                     mods
                 \s*]\s*$}gxo)        #                         ] 
      { my( $gi, $att, $regexp, $mods)= ($1, $2, $3, $4);
        $regexp= qr/(?$mods:$regexp)/;
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{attregexp_handlers_exp}->{$gi}})
             { if( ($exp->{att} eq $att) && ($exp->{regexp} eq $regexp) )
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler; }
                   else          # remove the handler
                     { $handlers->{attregexp_handlers}->{$gi}--;
                       splice( @{$handlers->{attregexp_handlers_exp}->{$gi}}, $i, 1);
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            $handlers->{attregexp_handlers}->{$gi}++;
            my $exp={att => $att, regexp => $regexp, handler => $handler};
            $handlers->{attregexp_handlers_exp}->{$gi} ||= [];
            push @{$handlers->{attregexp_handlers_exp}->{$gi}}, $exp;
          }
        return 1;
      }
    else 
      { return 0; }
  }

sub _set_string_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for string conditions
    if( $path=~/^\s*($REG_NAME)            # elt
                 \s*\[\s*string            #    [string
                 \s*\(\s*($REG_NAME)?\s*\) #           (sub_elt)
                 \s*=\s*                   #                     =
                 ($REG_STRING)             #                       "text" (or 'text')
                 \s*\]\s*$/ox)             #                              ] 
      { my( $gi, $sub_elt, $text)= ($1, $2, $3);
        $text= substr( $text, 1, -1) if( defined $text); # remove the quotes
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{text_handlers_exp}->{$gi}})
             { if( ($exp->{text} eq $text) &&
                   ( !$exp->{sub_elt} || ($exp->{sub_elt} eq $sub_elt) )
                 )
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler; }
                   else          # remove the handler
                     { $handlers->{text_handlers}->{$gi}--;
                       splice( @{$handlers->{text_handlers_exp}->{$gi}}, $i, 1);
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            $handlers->{text_handlers}->{$gi}++;
            my $exp={sub_elt => $sub_elt, text => $text, handler => $handler};
            $handlers->{text_handlers_exp}->{$gi} ||= [];
            push @{$handlers->{text_handlers_exp}->{$gi}}, $exp;
          }
        return 1;
      }
    else 
      { return 0; 
      }
  }


sub _set_string_regexp_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for string regexp conditions
    if( $path=~m{^\s*($REG_NAME)        # (elt)
                 \s*\[\s*string         #    [string
                 \s*\(\s*($REG_NAME?)\) #           (sub_elt)
                 \s*=~\s*               #              =~ 
                 /($REG_REGEXP_EXP)/    #                 /(regexp)/
                 \s*($REG_REGEXP_MOD)?  #                         (mods)
                 \s*\]\s*$}ox)          #                             ]   (or ')
      { my( $gi, $sub_elt, $regexp, $mods)= ($1, $2, $3, $4);
        $mods||="";
        $regexp= qr/(?$mods:$regexp)/;
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{regexp_handlers_exp}->{$gi}})
             { if( ($exp->{regexp} eq $regexp) &&
                   ( !$exp->{sub_elt} || ($exp->{sub_elt} eq $sub_elt) )
                 )
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler;  
                     }
                   else          # remove the handler
                     { $handlers->{regexp_handlers}->{$gi}--;
                       splice( @{$handlers->{regexp_handlers_exp}->{$gi}}, $i, 1);
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            $handlers->{regexp_handlers}->{$gi}++;
            my $exp= {sub_elt => $sub_elt, regexp => $regexp, handler => $handler};
            $handlers->{regexp_handlers_exp}->{$gi} ||= [];
            push @{$handlers->{regexp_handlers_exp}->{$gi}}, $exp;
          }
        return 1;
      }
    else 
      { return 0; 
      }
  }


sub _set_star_att_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for *[@att="val"] or *[@att] conditions
    if( $path=~/^(?:\s*\*)?         # * (optional)
                 \s*\[\s*\@         #    [@
                 ($REG_NAME)        #      att
                 (?:\s*=\s*         #         = 
                 ($REG_STRING))?    #           string
                     \s*\]\s*$/ox)  #                 ]  
      { my( $att, $val)= ($1, $2);
        $val= substr( $val, 1, -1) if( defined $val); # remove the quotes from the string
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{att_handlers_exp}->{$att}})
             { if( ($exp->{att} eq $att) && ( !defined( $val) || _eq( $exp->{val}, $val) ) )
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler; }
                   else          # remove the handler
                     { splice( @{$handlers->{att_handlers_exp}->{$att}}, $i, 1);
                       $handlers->{att_handlers}->{$att}--;
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            $handlers->{att_handlers}->{$att}++;
            my $exp={att => $att, val => $val, handler => $handler};
            $handlers->{att_handlers_exp}->{$att} ||= [];
            push @{$handlers->{att_handlers_exp}->{$att}}, $exp;
          }
        return 1;
      }
    else 
      { return 0; 
      }
  }

sub _set_star_att_regexp_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # check for *[@att=~ /regexp/] conditions
    if( $path=~ m{^(?:\s*\*)?             # * (optional)
                   \s*\[\s*\@             #  [@
                   ($REG_NAME)            #    att
                   \s*=~\s*               #        =~ 
                   /($REG_REGEXP_EXP)/    #           /(regexp)/
                   \s*($REG_REGEXP_MOD)?  #                     (mods)
                   \s*\]\s*$}ox)          #                           ]  
      { my( $att, $regexp, $mods)= ($1, $2, $3);
        $mods||="";
        $regexp= qr/(?$mods:$regexp)/;
        if( $prev_handler)
          { # replace or remove the previous handler
            my $i=0; # so we can splice the array if need be
            foreach my $exp ( @{$handlers->{att_regexp_handlers_exp}->{$att}})
             { if( $exp->{regexp} eq $regexp)
                 { if( $handler) # just replace the handler
                     { $exp->{handler}= $handler;  
                     }
                   else          # remove the handler
                     { splice( @{$handlers->{att_regexp_handlers_exp}->{$att}}, $i, 1);
                   $handlers->{att_regexp_handlers}--;
                       last;
                     }
                 }
               $i++;
             }
          }
        elsif( $handler)
          { # new handler only
            my $exp= { regexp => $regexp, handler => $handler};
            $handlers->{regexp_handlers_exp}->{$att} ||= [];
            push @{$handlers->{att_regexp_handlers_exp}->{$att}}, $exp;
            $handlers->{att_regexp_handlers}++;
          }
        return 1;
      }
    else 
      { return 0; 
      }
  }


sub _set_pi_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    # PI conditions ( '?target' => \&handler or '?' => \&handler
    #             or '#PItarget' => \&handler or '#PI' => \&handler)
    if( $path=~ /^\s*(?:\?|#PI)\s*(?:([^\s]*)\s*)$/)
      { my $target= $1 || '';
        # update the path_handlers count, knowing that
        # either the previous or the new handler can be undef
        $handlers->{pi_handlers}->{$1}= $handler;
        return 1;
      }
    else 
      { return 0; 
      }
  }

sub _set_level_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_;
    if( $path =~ m{^ \s* level \s* \( \s* ([0-9]+) \s* \) \s* $}ox )
      { my $level= $1;
        $handlers->{handlers}->{level}->{$level}= $handler; 
        return 1;
      }
    else 
      { return 0; }
  }

sub _set_regexp_handler
  { my( $handlers, $path, $handler, $prev_handler)= @_; 
    # if the expression was a regexp it is now a string (it was stringified when it became a hash key)
    if( $path=~ m{^\(\?([xism]*)(?:-[xism]*)?:(.*)\)$}) 
      { my $regexp= qr/(?$1:$2)/; # convert it back into a regexp
        $handlers->{handlers}->{regexp}->{$path}= { regexp => $regexp, handler => $handler}; 
        return 1;
      }
    else 
      { return 0; }
  }


# just like eq except that undef values do not trigger warnings
sub _eq
  { my( $val1, $val2)= @_;
    if( !defined $val1) { return !defined $val2 }
    if( !defined $val2) { return 0; }
    return $val1 eq $val2;
  }

sub setCharHandler
  { my( $t, $handler)= @_;
    $t->{twig_char_handler}= $handler;
  }


sub _reset_handlers
  { my $handlers= shift;
    delete $handlers->{handlers};
    delete $handlers->{path_handlers};
    delete $handlers->{subpath_handlers};
    $handlers->{attcond_handlers_exp}=[] if( $handlers->{attcond_handlers});
    delete $handlers->{attcond_handlers};
  }
  
sub _set_handlers
  { my $handlers= shift || return;
    my $set_handlers= {};
    foreach my $path (keys %{$handlers})
      { _set_handler( $set_handlers, $path, $handlers->{$path}); }
    return $set_handlers;
  }
    

sub setTwigHandler
  { my( $t, $path, $handler)= @_;
    $t->{twig_handlers} ||={};
    return _set_handler( $t->{twig_handlers}, $path, $handler);
  }

sub setTwigHandlers
  { my( $t, $handlers)= @_;
    my $previous_handlers= $t->{twig_handlers} || undef;
    _reset_handlers( $t->{twig_handlers});
    $t->{twig_handlers}= _set_handlers( $handlers);
    return $previous_handlers;
  }

sub setStartTagHandler
  { my( $t, $path, $handler)= @_;
    $t->{twig_starttag_handlers}||={};
    return _set_handler( $t->{twig_starttag_handlers}, $path, $handler);
  }

sub setStartTagHandlers
  { my( $t, $handlers)= @_;
    my $previous_handlers= $t->{twig_starttag_handlers} || undef;
    _reset_handlers( $t->{twig_starttag_handlers});
    $t->{twig_starttag_handlers}= _set_handlers( $handlers);
    return $previous_handlers;
   }

sub setIgnoreEltsHandler
  { my( $t, $path, $action)= @_;
    $t->{twig_ignore_elts_handlers}||={};
    return _set_handler( $t->{twig_ignore_elts_handlers}, $path, $action );
  }

sub setIgnoreEltsHandlers
  { my( $t, $handlers)= @_;
    my $previous_handlers= $t->{twig_ignore_elts_handlers};
    _reset_handlers( $t->{twig_ignore_elts_handlers});
    $t->{twig_ignore_elts_handlers}= _set_handlers( $handlers);
    return $previous_handlers;
   }

sub setEndTagHandler
  { my( $t, $path, $handler)= @_;
    $t->{twig_endtag_handlers}||={};
    return _set_handler( $t->{twig_endtag_handlers}, $path,$handler);
  }

sub setEndTagHandlers
  { my( $t, $handlers)= @_;
    my $previous_handlers= $t->{twig_endtag_handlers};
    _reset_handlers( $t->{twig_endtag_handlers});
    $t->{twig_endtag_handlers}= _set_handlers( $handlers);
    return $previous_handlers;
   }

# a little more complex: set the twig_handlers only if a code ref is given
sub setTwigRoots
  { my( $t, $handlers)= @_;
    my $previous_roots= $t->{twig_roots};
    _reset_handlers($t->{twig_roots});
    $t->{twig_roots}= _set_handlers( $handlers);
    foreach my $path (keys %{$handlers})
      { $t->{twig_handlers}||= {};
        _set_handler( $t->{twig_handlers}, $path, $handlers->{$path})
          if( isa( $handlers->{$path}, 'CODE')); 
      }
    return $previous_roots;
  }

# just store the reference to the expat object in the twig
sub _twig_init
  { 
    my $p= shift;
    my $t=$p->{twig};
    $t->{twig_parser}= $p; 
    weaken( $t->{twig_parser}) if( $weakrefs);
    $t->{twig_parsing}=1;
    # in case they had been created by a previous parse
    delete $t->{twig_dtd};
    delete $t->{twig_doctype};
    delete $t->{twig_xmldecl};
    # if needed set the output filehandle
    $t->_set_fh_to_twig_output_fh();
  }

# uses eval to catch the parser's death
sub safe_parse
  { my( $t, $str)= @_;
    eval { $t->parse( $str); } ;
    return $@ ? 0 : $t;
  }

sub safe_parsefile
  { my( $t, $file)= @_;
    eval { $t->parsefile( $file); } ;
    return $@ ? 0 : $t;
  }


sub _add_or_discard_stored_spaces
  { my $t= shift;
    my %option= @_;
   
    return unless( $t->{twig_current}); # ugly hack, with ignore twig_current can disapper 
    if( $t->{twig_stored_spaces} || $option{force} || $t->{twig_preserve_space})
      { if( $t->{twig_current}->is_pcdata)
          { $t->{twig_current}->append_pcdata($t->{twig_stored_spaces}); }
        else
          { my $current_gi= $t->{twig_current}->gi;
            unless( defined( $t->{twig_space_policy}->{$current_gi}))
              { $t->{twig_space_policy}->{$current_gi}= _space_policy( $t, $current_gi); }

            if( $t->{twig_space_policy}->{$current_gi} ||  ($t->{twig_stored_spaces}!~ m{\n}) || $option{force} || $t->{twig_preserve_space})
              { _insert_pcdata( $t, $t->{twig_stored_spaces} ); }
            $t->{twig_stored_spaces}='';

          }
      }
  }

# the default twig handlers, which build the tree
sub _twig_start
  { 
    my ($p, $gi, @att)= @_;
    my $t=$p->{twig};

    # empty the stored pcdata (space stored in case they are really part of 
    # a pcdata element) or stored it if the space policy dictades so
    # create a pcdata element with the spaces if need be
    _add_or_discard_stored_spaces( $t);
    my $parent= $t->{twig_current};

    # if we were parsing PCDATA then we exit the pcdata
    if( $t->{twig_in_pcdata})
      { $t->{twig_in_pcdata}= 0;
        delete $parent->{'twig_current'};
        $parent= $parent->{parent};
      }

    # if we choose to keep the encoding then we need to parse the tag
    if( my $func = $t->{parse_start_tag})
      { ($gi, @att)= &$func($p->original_string); }
    elsif( $t->{twig_entities_in_attribute})
      { 
       ($gi,@att)= _parse_start_tag( $p->recognized_string); 
         $t->{twig_entities_in_attribute}=0;
      }

    # if we are using an external DTD, we need to fill the default attributes
    if( $t->{twig_read_external_dtd}) { _fill_default_atts( $t, $gi, \@att); }
    
    # filter the input data if need be  
    if( my $filter= $t->{twig_input_filter})
      { $gi= $filter->( $gi);
        @att= map { $filter->($_) } @att; 
      }

    _replace_ns( $t, \$gi, \@att) if( $t->{twig_map_xmlns});

    my $elt= $t->{twig_elt_class}->new( $gi);
    $elt->set_atts( @att);
 
    delete $parent->{'twig_current'} if( $parent);
    $t->{twig_current}= $elt;
    $elt->{'twig_current'}=1;

    if( $parent)
      { my $prev_sibling= $parent->{last_child};
        if( $prev_sibling) 
          { $prev_sibling->{next_sibling}=  $elt; 
            $elt->set_prev_sibling( $prev_sibling);
          }

        $elt->set_parent( $parent);
        $parent->{first_child}=  $elt unless( $parent->{first_child}); 
        $parent->set_last_child( $elt);
      }
    else 
      { # processing root
        $t->set_root( $elt);
        # call dtd handlerif need be
        $t->{twig_dtd_handler}->($t, $t->{twig_dtd})
          if( defined $t->{twig_dtd_handler});
      
        # set this so we can catch external entities
        # (the handler was modified during DTD processing)
        if( $t->{twig_default_print})
          { $p->setHandlers( Default => \&_twig_print); }
        elsif( $t->{twig_roots})
          { $p->setHandlers( Default => sub { return }); }
        else
          { $p->setHandlers( Default => \&_twig_default); }
      }
   
    $elt->{empty}=  $p->recognized_string=~ m{/\s*>$}s ? 1 : 0;

    $elt->{extra_data}= $t->{extra_data} if( $t->{extra_data});
    $t->{extra_data}='';

    # if the element is ID-ed then store that info
    my $id= $elt->{'att'}->{$ID};
    if( defined $id)
      { $t->{twig_id_list}->{$id}= $elt; 
        weaken( $t->{twig_id_list}->{$id}) if( $weakrefs);
      }

    # call user handler if need be
    if( $t->{twig_starttag_handlers})
      { # call all appropriate handlers
        my @handlers= _handler( $t, $t->{twig_starttag_handlers}, $gi, $elt);
    
        local $_= $elt;
    
        foreach my $handler ( @handlers)
          { $handler->($t, $elt) || last; }
        # call _all_ handler if needed
        if( my $all= $t->{twig_starttag_handlers}->{handlers}->{$ALL})
          { $all->($t, $elt); }
      }

    # check if the tag is in the list of tags to be ignored
    if( $t->{twig_ignore_elts_handlers})
      { my @handlers= _handler( $t, $t->{twig_ignore_elts_handlers}, $gi, $elt);
        # only the first handler counts, it contains the action (discard/print/string)
        if( @handlers) { my $action= shift @handlers; $t->ignore( $action); }
      }

    if( $elt->{'att'}->{'xml:space'} && (  $elt->{'att'}->{'xml:space'} eq 'preserve')) { $t->{twig_preserve_space}++; }

  }

sub _replace_ns
  { my( $t, $gi, $atts)= @_;
    foreach my $new_prefix ( $t->parser->new_ns_prefixes)
      { my $uri= $t->parser->expand_ns_prefix( $new_prefix);
        # replace the prefix if it is mapped
        if( !$t->{twig_keep_original_prefix} && (my $mapped_prefix= $t->{twig_map_xmlns}->{$uri}))
          { $new_prefix= $mapped_prefix; }
        # now put the namespace declaration back in the element
        if( $new_prefix eq '#default')
          { push @$atts, "xmlns" =>  $uri; } 
        else
          { push @$atts, "xmlns:$new_prefix" =>  $uri; } 
      }

    if( $t->{twig_keep_original_prefix})
      { # things become more complex: we need to find the original prefix
        # and store both prefixes
        my $ns_info= $t->_ns_info( $$gi);
        my $map_att;
        if( $ns_info->{mapped_prefix})
          { $$gi= "$ns_info->{mapped_prefix}:$$gi";
            $map_att->{$ns_info->{mapped_prefix}}= $ns_info->{prefix};
          }
        my $att_name=1;
        foreach( @$atts) 
          { if( $att_name) 
              { 
                my $ns_info= $t->_ns_info( $_);
                if( $ns_info->{mapped_prefix})
                  { $_= "$ns_info->{mapped_prefix}:$_";
                    $map_att->{$ns_info->{mapped_prefix}}= $ns_info->{prefix};
                  }
                $att_name=0; 
              }
            else           
              {  $att_name=1; }
          }
        push @$atts, '#original_gi', $map_att if( $map_att);
      }
    else
      { $$gi= $t->_replace_prefix( $$gi); 
        my $att_name=1;
        foreach( @$atts) 
          { if( $att_name) { $_= $t->_replace_prefix( $_); $att_name=0; }
            else           {  $att_name=1; }
          }
      }
  }


# extract prefix, local_name, uri, mapped_prefix from a name
# will only work if called from a start or end tag handler
sub _ns_info
  { my( $t, $name)= @_;
    my $ns_info={};
    my $p= $t->parser;
    $ns_info->{uri}= $p->namespace( $name); 
    return $ns_info unless( $ns_info->{uri});

    $ns_info->{prefix}= _a_proper_ns_prefix( $p, $ns_info->{uri});
    $ns_info->{mapped_prefix}= $t->{twig_map_xmlns}->{$ns_info->{uri}} || $ns_info->{prefix};

    return $ns_info;
  }
    
sub _a_proper_ns_prefix
  { my( $p, $uri)= @_;
    foreach my $prefix ($p->current_ns_prefixes)
      { if( $p->expand_ns_prefix( $prefix) eq $uri)
          { return $prefix; }
      }
  }

sub _fill_default_atts
  { my( $t, $gi, $atts)= @_;
    my $dtd= $t->{twig_dtd};
    my $attlist= $dtd->{att}->{$gi};
    my %value= @$atts;
    foreach my $att (keys %$attlist)
      { if(   !exists( $value{$att}) 
            && exists( $attlist->{$att}->{default})
            && ( $attlist->{$att}->{default} ne '#IMPLIED')
          )
          { # the quotes are included in the default, so we need to remove them
            my $default_value= substr( $attlist->{$att}->{default}, 1, -1);
            push @$atts, $att, $default_value;
          }
      }
  }


# the default function to parse a start tag (in keep_encoding mode)
# can be overridden with the parse_start_tag method
# only works for 1-byte character sets
sub _parse_start_tag
  { my $string= shift;
    my( $gi, @atts);

    # get the gi (between < and the first space, / or > character)
    #if( $string=~ s{^<\s*([^\s>/]*)[\s>/]*}{}s)
    if( $string=~ s{^<\s*($REG_NAME)\s*[\s>/]}{}s)
      { $gi= $1; }
    else
      { croak "error parsing tag '$string'"; }
    while( $string=~ s{^([^\s=]*)\s*=\s*(["'])(.*?)\2\s*}{}s)
      { push @atts, $1, $3; }
    return $gi, @atts;
  }

sub set_root
  { my( $t, $elt)= @_;
    $t->{twig_root}= $elt;
    $elt->{twig}= $t;
    weaken(  $elt->{twig}) if( $weakrefs);
  }

sub _twig_end($$;@)
  { 
    my ($p, $gi)  = @_;
    my $t=$p->{twig};

    if( $t->{twig_map_xmlns}) { $gi= $t->_replace_prefix( $gi); }
  
    _add_or_discard_stored_spaces( $t);
 
    # the new twig_current is the parent
    my $elt= $t->{twig_current};
    delete $elt->{'twig_current'};

    # if we were parsing PCDATA then we exit the pcdata too
    if( $t->{twig_in_pcdata})
      { $t->{twig_in_pcdata}= 0;
        $elt= $elt->{parent} if($elt->{parent});
        delete $elt->{'twig_current'};
      }

    # parent is the new current element
    my $parent= $elt->{parent};
    $parent->{'twig_current'}=1 if( $parent);
    $t->{twig_current}= $parent;

    $elt->{extra_data_before_end_tag}= $t->{extra_data} if( $t->{extra_data}); 
    $t->{extra_data}='';

    if( $t->{twig_handlers})
      { # look for handlers
        my @handlers= _handler( $t, $t->{twig_handlers}, $gi, $elt);

        local $_= $elt; # so we can use $_ in the handlers
    
        foreach my $handler ( @handlers)
          { $handler->($t, $elt) || last; }
        # call _all_ handler if needed
        if( my $all= $t->{twig_handlers}->{handlers}->{$ALL})
          { $all->($t, $elt); }
      }

    # if twig_roots is set for the element then set appropriate handler
    if(  $t->{twig_root_depth} and ($p->depth == $t->{twig_root_depth}) )
      { if( $t->{twig_default_print})
          { # select the proper fh (and store the currently selected one)
            $t->_set_fh_to_twig_output_fh(); 
            if( $t->{twig_keep_encoding})
              { $p->setHandlers( %twig_handlers_roots_print_original); }
            else
              { $p->setHandlers( %twig_handlers_roots_print); }
          }
        else
          { $p->setHandlers( %twig_handlers_roots); }
      }

    if( $elt->{'att'}->{'xml:space'} && (  $elt->{'att'}->{'xml:space'} eq 'preserve')) { $t->{twig_preserve_space}--; }
  }

# return the list of handler that can be activated for an element 
# (either of CODE ref's or 1's for twig_roots)

sub _handler
  { my( $t, $handlers, $gi, $elt)= @_;

    my @found_handlers=();
    my $found_handler;

    # warning: $elt can be either 
    # - a regular element
    # - a ref to the attribute hash (when called for an element 
    #   for which the XML::Twig::Elt has not been built, outside 
    #   of the twig_roots)
    # - a string (case of an entity in keep_encoding mode)

    # check for an attribute expression with no gi
    if( $handlers->{att_handlers})
      { my %att_handlers= %{$handlers->{att_handlers_exp}};
        foreach my $att ( keys %att_handlers)
          { my $att_val;
            # get the attribute value
            if( ref $elt eq 'HASH')
              { $att_val= $elt->{$att}; }     # $elt is the atts hash
            elsif( isa( $elt,'XML::Twig::Elt'))
              { $att_val= $elt->{'att'}->{$att}; }  # $elt is an element
                if( defined $att_val)
                  { my @cond= @{$handlers->{att_handlers_exp}->{$att}};
                    foreach my $cond (@cond)
                      {  # 2 cases: either there is a val and the att value should be equal to it
                         #          or there is no val (condition was gi[@att]), just for the att to be defined 
                    if( !defined $cond->{val} || ($att_val eq $cond->{val}) )  
                      { push @found_handlers, $cond->{handler};}
                  }
              }
          }
      }

    # check for an attribute regexp expression with no gi
    if( $handlers->{att_regexp_handlers})
      { my %att_handlers= %{$handlers->{att_regexp_handlers_exp}};
        foreach my $att ( keys %att_handlers)
          { my $att_val;
            # get the attribute value
            if( ref $elt eq 'HASH')
              { $att_val= $elt->{$att}; }     # $elt is the atts hash
            elsif( isa( $elt,'XML::Twig::Elt'))
              { $att_val= $elt->{'att'}->{$att}; }  # $elt is an element

            if( defined $att_val)
              { my @cond= @{$handlers->{att_regexp_handlers_exp}->{$att}};
                foreach my $cond (@cond)
                  { if( $att_val=~ $cond->{regexp})  
                      { push @found_handlers, $cond->{handler};}
                  }
              }
          }
      }

    # check for a text expression
    if( $handlers->{text_handlers}->{$gi})
      { my @text_handlers= @{$handlers->{text_handlers_exp}->{$gi}};
        foreach my $exp ( @text_handlers)
          { if (!$exp->{sub_elt})
              { push @found_handlers, $exp->{handler}
                  if( $elt->text eq $exp->{text});
              }
            else
              { foreach my $child ($elt->children($exp->{sub_elt}))
                  { if( $child->text eq $exp->{text})
                      { push @found_handlers, $exp->{handler};
                        last;
                      }
                  }
              }
          }
      }

    # check for a text regexp expression
    if( $handlers->{regexp_handlers}->{$gi})
      { my @regexp_handlers= @{$handlers->{regexp_handlers_exp}->{$gi}};
        foreach my $exp ( @regexp_handlers)
          { if( !$exp->{sub_elt})
              { push @found_handlers, $exp->{handler}
                  if $elt->text =~ $exp->{regexp};
              }
            else
              { foreach my $child ($elt->children($exp->{sub_elt}))
                  { if( $child->text =~ $exp->{regexp})
                      { push @found_handlers, $exp->{handler};
                        last;
                      }
                  }
              }
          }
      }

    # check for an attribute expression
    if( $handlers->{attcond_handlers}->{$gi})
      { my @attcond_handlers= @{$handlers->{attcond_handlers_exp}->{$gi}};
        foreach my $exp ( @attcond_handlers)
          { my $att_val;
        # get the attribute value
        if( ref $elt eq 'HASH')
          { $att_val= $elt->{$exp->{att}}; }    # $elt is the atts hash
        else
          { $att_val= $elt->{'att'}->{$exp->{att}}; }# $elt is an element

        # 2 cases: either there is a val and the att value should be equal to it
        #          or there is no val (condition was gi[@att]), just for the att to be defined 
        if( defined $att_val && ( !defined $exp->{val} || ($att_val eq $exp->{val}) ) ) 
              { push @found_handlers, $exp->{handler}; }
          }
      }

    # check for an attribute regexp
    if( $handlers->{attregexp_handlers}->{$gi})
      { my @attregexp_handlers= @{$handlers->{attregexp_handlers_exp}->{$gi}};
        foreach my $exp ( @attregexp_handlers)
          { my $att_val;
        # get the attribute value
        if( ref $elt eq 'HASH')
          { $att_val= $elt->{$exp->{att}}; }    # $elt is the atts hash
        else
          { $att_val= $elt->{'att'}->{$exp->{att}}; }# $elt is an element

        if( defined $att_val && ( ($att_val=~  $exp->{regexp}) ) ) 
              { push @found_handlers, $exp->{handler}; }
          }
      }

    # check for a full path
    if( defined $handlers->{path_handlers}->{gi}->{$gi})
      { my $path= $t->path( $gi); 
        if( defined( $found_handler= $handlers->{path_handlers}->{path}->{$path}) )
          { push @found_handlers, $found_handler; }
      }

    # check for a partial path
    if( $handlers->{subpath_handlers}->{gi}->{$gi})
      { my $path= $t->path( $gi);
        while( $path)
          { # test each sub path
            if( defined( $found_handler= $handlers->{subpath_handlers}->{path}->{$path}) )
              { push @found_handlers, $found_handler; }
             $path=~ s{^[^/]*/?}{}; # remove initial gi and /
          }
      }

    # check for a gi (simple gi's are stored directly in the handlers field)
    if( defined $handlers->{handlers}->{gi}->{$gi})
      { push @found_handlers, $handlers->{handlers}->{gi}->{$gi}; }

    # check for a gi regexp
    if( defined $handlers->{handlers}->{regexp})
      { foreach my $potential (values %{$handlers->{handlers}->{regexp}})
          { if( $gi=~ $potential->{regexp})
              { push @found_handlers, $potential->{handler}; }
          }
      }

    if( defined $handlers->{handlers}->{level}->{$t->depth})
      { push @found_handlers, $handlers->{handlers}->{level}->{$t->depth}; }


    # if no handler found call default handler if defined
    if( !@found_handlers && defined $handlers->{handlers}->{$DEFAULT})
      { push @found_handlers, $handlers->{handlers}->{$DEFAULT}; }

    if( @found_handlers and $t->{twig_do_not_chain_handlers}) 
      { @found_handlers= ($found_handlers[0]); }

    return @found_handlers; # empty if no handler found

  }


sub _replace_prefix
  { my( $t, $name)= @_;
    my $p= $t->parser;
    my $uri= $p->namespace( $name);
    # try to get the namespace from default if none is found (for attributes)
    # this should probably be an option
    if( !$uri and( $name!~/^xml/)) { $uri= $p->expand_ns_prefix( '#default'); }
    if( $uri)
      { if (my $mapped_prefix= $t->{twig_map_xmlns}->{$uri})
          { return "$mapped_prefix:$name"; }
        else
          { my $prefix= _a_proper_ns_prefix( $p, $uri);
            return $prefix ? "$prefix:$name" : $name; 
          }
      }
    else
      { return $name; }
  }

sub _twig_char
  { 
    my ($p, $string)= @_;
    my $t=$p->{twig}; 

    # if keep_encoding was set then use the original string instead of
    # the parsed (UTF-8 converted) one
    if( $t->{twig_keep_encoding})
      { $string= $p->original_string(); }

    if( $t->{twig_input_filter})
      { $string= $t->{twig_input_filter}->( $string); }

    if( $t->{twig_char_handler})
      { $string= $t->{twig_char_handler}->( $string); }

    my $elt= $t->{twig_current};

    if(    $t->{twig_in_cdata})
      { # text is the continuation of a previously created pcdata
        $elt->{cdata}.=  $t->{twig_stored_spaces}.$string
          unless( $t->{twig_keep_encoding} && $elt->{cdata}) ; # fixes a bug in XML::Parser for long CDATA
      } 
    elsif( $t->{twig_in_pcdata})
      { # text is the continuation of a previously created cdata
        if( $t->{extra_data})
          { $elt->{extra_data_in_pcdata} ||=[];
            push @{$elt->{extra_data_in_pcdata}}, { text => $t->{extra_data}, offset => length( $elt->{pcdata}) };
            $t->{extra_data}='';
          }
        $elt->{pcdata}.=  $string; 
      } 
    else
      { # text is just space, which might be discarded later
        if( $string=~/\A\s*\Z/s)
          { if( $t->{extra_data})
              { # we got extra data (comment, pi), lets add the spaces to it
                $t->{extra_data} .= $string; 
              }
            else
              { # no extra data, just store the spaces
                $t->{twig_stored_spaces}.= $string;
              }
          } 
        else
          { my $new_elt= _insert_pcdata( $t, $t->{twig_stored_spaces}.$string);
            delete $elt->{'twig_current'};
            $new_elt->{'twig_current'}=1;
            $t->{twig_current}= $new_elt;
            $t->{twig_in_pcdata}=1;
            if( $t->{extra_data})
              { $new_elt->{extra_data_in_pcdata}=[];
                push @{$new_elt->{extra_data_in_pcdata}}, { text => $t->{extra_data}, offset => 0 };
                $t->{extra_data}='';
              }
          }
      }
  }


sub _twig_cdatastart
  { 
    my $p= shift;
    my $t=$p->{twig};

    $t->{twig_in_cdata}=1;
    my $cdata=  $t->{twig_elt_class}->new( '#CDATA');
    my $twig_current= $t->{twig_current};

    if( $t->{twig_in_pcdata})
      { # create the node as a sibling of the #PCDATA
        $cdata->set_prev_sibling( $twig_current);
        $twig_current->{next_sibling}=  $cdata;
        my $parent= $twig_current->{parent};
        $cdata->set_parent( $parent);
        $parent->set_last_child( $cdata);
        $t->{twig_in_pcdata}=0;
      }
    else
      { # we have to create a PCDATA element if we need to store spaces
        if( $t->_space_policy($XML::Twig::index2gi[$twig_current->{'gi'}]) && $t->{twig_stored_spaces})
          { _insert_pcdata( $t, $t->{twig_stored_spaces}); }
        $t->{twig_stored_spaces}='';
      
        # create the node as a child of the current element      
        $cdata->set_parent( $twig_current);
        if( my $prev_sibling= $twig_current->{last_child})
          { $cdata->set_prev_sibling( $prev_sibling);
            $prev_sibling->{next_sibling}=  $cdata;
          }
        else
          { $twig_current->{first_child}=  $cdata; }
        $twig_current->set_last_child( $cdata);
      
      }

    delete $twig_current->{'twig_current'};
    $t->{twig_current}= $cdata;
    $cdata->{'twig_current'}=1;
    if( $t->{extra_data}) { $cdata->set_extra_data( $t->{extra_data}); $t->{extra_data}='' };
  }

sub _twig_cdataend
  { 
    my $p= shift;
    my $t=$p->{twig};

    $t->{twig_in_cdata}=0;

    my $elt= $t->{twig_current};
    delete $elt->{'twig_current'};
    my $cdata= $elt->{cdata};
    $elt->{cdata}=  $cdata;

    if( $t->{twig_handlers})
      { # look for handlers
        my @handlers= _handler( $t, $t->{twig_handlers}, CDATA, $elt);
        local $_= $elt; # so we can use $_ in the handlers
        foreach my $handler ( @handlers) { $handler->($t, $elt) || last; }
      }

    $elt= $elt->{parent};
    $t->{twig_current}= $elt;
    $elt->{'twig_current'}=1;
  }

sub _twig_pi
  { 
    my( $p, $target, $data)= @_;
    my $t=$p->{twig};
    return unless( $t->{twig_process_pi} || $t->{twig_keep_pi});

    if( $t->{twig_input_filter})
      { $target = $t->{twig_input_filter}->( $target) ;
        $data   = $t->{twig_input_filter}->( $data)   ;
      }

    my $twig_current= $t->{twig_current};    # always defined

    # if pi's are to be kept then we piggiback them to the current element
    if( $t->{twig_keep_pi})
      {  
        if( my $handler= $t->{twig_handlers}->{pi_handlers}->{$target})
          { $t->{extra_data}.= $handler->( $t, $target, $data); }
        elsif( $handler= $t->{twig_handlers}->{pi_handlers}->{''})
          { $t->{extra_data}.= $handler->( $t, $target, $data); }
        else
          { if( $t->{twig_stored_spaces})
              { $t->{extra_data}.= $t->{twig_stored_spaces};
                $t->{twig_stored_spaces}= '';
              }
            # then the recognized/original string (input filtered if needed)
            my $extra_data= $XML::Twig::Elt::keep_encoding ?  $p->recognized_string() : $p->original_string();
            $extra_data= $t->{twig_input_filter}->( $extra_data) if( $t->{twig_input_filter});
            $t->{extra_data}.= $extra_data;
            return;
          }

      }
    else
      { # pi's are processed
        my $pi=  $t->{twig_elt_class}->new( PI);
        $pi->set_pi( $target, $data);

        unless( $t->root)
          {  _pi_handlers( $t, $pi, $target);
            _add_prolog_data( $t, $pi); 
            return;
          }

        if( $t->{twig_in_pcdata})
          { # create the node as a sibling of the #PCDATA
            $pi->paste_after( $twig_current);
            $t->{twig_in_pcdata}=0;
          }
        else
          { # we have to create a PCDATA element if we need to store spaces
            if( $t->_space_policy($XML::Twig::index2gi[$twig_current->{'gi'}]) && $t->{twig_stored_spaces})
              { _insert_pcdata( $t, $t->{twig_stored_spaces}); }
            $t->{twig_stored_spaces}='';
            # create the node as a child of the current element
            $pi->paste_last_child( $twig_current);
          }
    
        delete $twig_current->{'twig_current'};
        my $parent= $pi->{parent}; 
        $t->{twig_current}= $parent;
        $parent->{'twig_current'}=1;

        _pi_handlers( $t, $pi, $target);
      }

  }

sub _pi_handlers
  { my( $t, $pi, $target)= @_;
    if( my $handler= $t->{twig_handlers}->{pi_handlers}->{$target})
      { local $_= $pi; $handler->( $t, $pi); }
    elsif( $handler= $t->{twig_handlers}->{pi_handlers}->{''})
      { local $_= $pi; $handler->( $t, $pi); }
  }


sub _twig_comment
  { 
    my( $p, $comment_text)= @_;
    my $t=$p->{twig};
    return unless( $t->{twig_process_comments} || $t->{twig_keep_comments});

    my $twig_current= $t->{twig_current};    # always defined

    # if comments are to be kept then we piggiback them to the current element
    if( $t->{twig_keep_comments})
      { # first add spaces
        if( $t->{twig_stored_spaces})
              { $t->{extra_data}.= $t->{twig_stored_spaces};
                $t->{twig_stored_spaces}= '';
              }
        # then the recognized/original string (input filtered if needed)
        my $extra_data= $XML::Twig::Elt::keep_encoding ?  $p->recognized_string() : $p->original_string();
        $extra_data= $t->{twig_input_filter}->( $extra_data) if( $t->{twig_input_filter});
        $t->{extra_data}.= $extra_data;
        return;
      }

    $comment_text= $t->{twig_input_filter}->( $comment_text) if( $t->{twig_input_filter});

    my $comment=  $t->{twig_elt_class}->new( COMMENT);
    $comment->{comment}=  $comment_text;

    unless( $t->root) 
      { _add_prolog_data( $t, $comment);
        _comment_handler( $t, $comment);
        return;
      }

    if( $t->{twig_in_pcdata})
      { # create the node as a sibling of the #PCDATA
        $comment->paste_after( $twig_current);
        $t->{twig_in_pcdata}=0;
      }
    else
      { # we have to create a PCDATA element if we need to store spaces
        if( $t->_space_policy($XML::Twig::index2gi[$twig_current->{'gi'}]) && $t->{twig_stored_spaces})
          { _insert_pcdata( $t, $t->{twig_stored_spaces}); }
        $t->{twig_stored_spaces}='';
        # create the node as a child of the current element
        $comment->paste_last_child( $twig_current);

      }
    _comment_handler( $t, $comment);

    delete $twig_current->{'twig_current'};

    my $parent= $comment->{parent};
    $t->{twig_current}= $parent;
    $parent->{'twig_current'}=1;

  }

sub _comment_handler
  { my( $t, $comment)= @_;
    if( $t->{twig_handlers}->{handlers}->{gi}->{'#COMMENT'})
      { # look for handlers
        local $_= $comment;
        my @handlers= _handler( $t, $t->{twig_handlers}, '#COMMENT', $comment);
        foreach my $handler ( @handlers)
          { $handler->($t, $comment) || last; }
      }
  }


sub _add_prolog_data
  { my($t, $prolog_data)= @_;
    # comment before the first element
    $t->{prolog_data} ||= $t->{twig_elt_class}->new( '#PROLOG_DATA');
    # create the node as a child of the current element
    $prolog_data->paste_last_child( $t->{prolog_data});
  }
  
sub _twig_final
  { 
    my $p= shift;
    my $t=$p->{twig};

    # restore the selected filehandle if needed
    $t->_set_fh_to_selected_fh();

    select $t->{twig_original_selected_fh} if($t->{twig_original_selected_fh}); # probably dodgy

    # tries to clean-up (probably not very well at the moment)
    undef $p->{twig};
    undef $t->{twig_parser};

    undef $t->{twig_parsing};

    return $t;
  }

sub _insert_pcdata
  { my( $t, $string)= @_;
    # create a new #PCDATA element
    my $parent= $t->{twig_current};    # always defined
    my $elt=  $t->{twig_elt_class}->new( PCDATA);
    $elt->{pcdata}=  $string;
    my $prev_sibling= $parent->{last_child};
    if( $prev_sibling) 
      { $prev_sibling->{next_sibling}=  $elt; 
        $elt->set_prev_sibling( $prev_sibling);
      }
    else
      { $parent->{first_child}=  $elt; }

    $elt->set_parent( $parent);
    $parent->set_last_child( $elt);
    $t->{twig_stored_spaces}='';
    return $elt;
  }

sub _space_policy
  { my( $t, $gi)= @_;
    my $policy;
    $policy=0 if( $t->{twig_discard_spaces});
    $policy=1 if( $t->{twig_keep_spaces});
    $policy=1 if( $t->{twig_keep_spaces_in}
               && $t->{twig_keep_spaces_in}->{$gi});
    $policy=0 if( $t->{twig_discard_spaces_in} 
               && $t->{twig_discard_spaces_in}->{$gi});
    return $policy;
  }


sub _twig_entity($$$$$$)
  { 
    my( $p, $name, $val, $sysid, $pubid, $ndata)= @_;
    my $t=$p->{twig};
    my $ent=XML::Twig::Entity->new( $name, $val, $sysid, $pubid, $ndata);
    $t->entity_list->add( $ent);
    if( $parser_version > 2.27) 
      { # this is really ugly, but with some versions of XML::Parser the value 
        # of the entity is not properly returned by the default handler
        my $ent_decl= $ent->text;
        if( $t->{twig_keep_encoding})
          { if( defined $ent->{val} && ($ent_decl !~ /["']/))
              { my $val=  $ent->{val};
                $ent_decl .= $val =~ /"/ ? qq{'$val' } : qq{"$val" }; 
              }
            # for my solaris box (perl 5.6.1, XML::Parser 2.31, expat?)
            $t->{twig_doctype}->{internal}=~ s{<!ENTITY\s+$name\s+$}{substr( $ent_decl, 0, -1)}e;
          }
        $t->{twig_doctype}->{internal} .= $ent_decl 
          unless( $t->{twig_doctype}->{internal}=~ m{<!ENTITY\s+$name\s+});
      }
  }

sub _twig_xmldecl
  { 
    my $p= shift;
    my $t=$p->{twig};
    $t->{twig_xmldecl}||={};                 # could have been set by set_output_encoding
    $t->{twig_xmldecl}->{version}= shift;
    $t->{twig_xmldecl}->{encoding}= shift; 
    $t->{twig_xmldecl}->{standalone}= shift;
  }

sub _twig_doctype
  { 
    my( $p, $name, $sysid, $pub, $internal)= @_;
    my $t=$p->{twig};
    $t->{twig_doctype}||= {};                   # create 
    $t->{twig_doctype}->{name}= $name;          # always there
    $t->{twig_doctype}->{sysid}= $sysid;        #  
    $t->{twig_doctype}->{pub}= $pub;            #  

    # now let's try to cope with XML::Parser 2.28 and above
    if( $parser_version > 2.27)
      { @saved_default_handler= $p->setHandlers( Default     => \&_twig_store_internal_dtd,
                                                 Entity      => \&_twig_entity,
                                               );
      $p->setHandlers( DoctypeFin  => \&_twig_stop_storing_internal_dtd);
      $t->{twig_doctype}->{internal}='';
      }
    else            
      # for XML::Parser before 2.28
      { $t->{twig_doctype}->{internal}=$internal; }

    # now check if we want to get the DTD info
    if( $t->{twig_read_external_dtd} && $sysid)
      { # let's build a fake document with an internal DTD
        my $dtd;
        # slurp the DTD
          { open( DTD, "<$sysid") or croak "cannot open dtd file $sysid: $!";
            local $/= undef;
            $dtd= "<!DOCTYPE $name [" . <DTD> . "]><$name/>";
            close DTD;
          }
       
        $t->save_global_state();            # save the globals (they will be reset by the following new)  
        my $t_dtd= XML::Twig->new;          # create a temp twig
        $t_dtd->parse( $dtd);               # parse it
        $t->{twig_dtd}= $t_dtd->{twig_dtd}; # grab the dtd info
        #$t->{twig_dtd_is_external}=1;
        $t->entity_list->_add_list( $t_dtd->entity_list) if( $t_dtd->entity_list); # grab the entity info
        $t->restore_global_state();
      }

  }

sub _twig_element
  { 
    my( $p, $name, $model)= @_;
    my $t=$p->{twig};
    $t->{twig_dtd}||= {};                      # may create the dtd 
    $t->{twig_dtd}->{model}||= {};             # may create the model hash 
    $t->{twig_dtd}->{elt_list}||= [];          # ordered list of elements 
    push @{$t->{twig_dtd}->{elt_list}}, $name; # store the elt
    $t->{twig_dtd}->{model}->{$name}= $model;  # store the model
    if( ($parser_version > 2.27) && ($t->{twig_doctype}->{internal}=~ m{(^|>)\s*$}) ) 
      { my $text= $XML::Twig::Elt::keep_encoding ? $p->original_string : $p->recognized_string; 
        unless( $text)
          { # this version of XML::Parser does not return the text in the *_string method
            # we need to rebuild it
            $text= "<!ELEMENT $name $model>";
          }
        $t->{twig_doctype}->{internal} .= $text;
      }
  }

sub _twig_attlist
  { 
    my( $p, $gi, $att, $type, $default, $fixed)= @_;
    #warn "in attlist: gi: '$gi', att: '$att', type: '$type', default: '$default', fixed: '$fixed'\n";
    my $t=$p->{twig};
    $t->{twig_dtd}||= {};                      # create dtd if need be 
    $t->{twig_dtd}->{$gi}||= {};               # create elt if need be 
    #$t->{twig_dtd}->{$gi}->{att}||= {};        # create att if need be 
    if( ($parser_version > 2.27) && ($t->{twig_doctype}->{internal}=~ m{(^|>)\s*$}) ) 
      { my $text= $XML::Twig::Elt::keep_encoding ? $p->original_string : $p->recognized_string; 
        unless( $text)
          { # this version of XML::Parser does not return the text in the *_string method
            # we need to rebuild it
            my $att_decl="$att $type";
            $att_decl .= " #FIXED"   if( $fixed);
            $att_decl .= " $default" if( defined $default);
            # 2 cases: there is already an attlist on that element or not
            if( $t->{twig_dtd}->{att}->{$gi})
              { # there is already an attlist, add to it
                $t->{twig_doctype}->{internal}=~ s{(<!ATTLIST\s*$gi )(.*?)\n?>}
                                                  { "$1$2\n" . ' ' x length( $1) . "$att_decl\n>"}es;
              }
            else
              { # create the attlist
                 $t->{twig_doctype}->{internal}.= "<!ATTLIST $gi $att_decl>"
              }
          }
      }
    $t->{twig_dtd}->{att}->{$gi}->{$att}= {} ;
    $t->{twig_dtd}->{att}->{$gi}->{$att}->{type}= $type; 
    $t->{twig_dtd}->{att}->{$gi}->{$att}->{default}= $default if( defined $default);
    $t->{twig_dtd}->{att}->{$gi}->{$att}->{fixed}= $fixed; 
  }

sub _twig_default
  { 
    my( $p, $string)= @_;
    
    my $t= $p->{twig};
    
    # process only if we have an entity
    return unless( $string=~ m{^&([^;]*);$});
    # the entity has to be pure pcdata, or we have a problem
    if( ($p->original_string=~ m{^<}) && ($p->original_string=~ m{>$}) ) 
      { # string is a tag, entity is in an attribute
        $t->{twig_entities_in_attribute}=1 if( $t->{twig_do_not_escape_amp_in_atts});
      }
    else
      { my $ent;
        if( $t->{twig_keep_encoding}) 
          { _twig_char( $p, $string); 
            $ent= substr( $string, 1, -1);
          }
        else
          { $ent= _twig_insert_ent( $t, $string); 
          }

        return $ent;
      }
  }
    
sub _twig_insert_ent
  { 
    my( $t, $string)=@_;

    my $twig_current= $t->{twig_current};

    my $ent=  $t->{twig_elt_class}->new( '#ENT');
    $ent->{ent}=  $string;

    _add_or_discard_stored_spaces( $t, force => 0);
    
    if( $t->{twig_in_pcdata})
      { # create the node as a sibling of the #PCDATA

        $ent->set_prev_sibling( $twig_current);
        $twig_current->{next_sibling}=  $ent;
        my $parent= $twig_current->{parent};
        $ent->set_parent( $parent);
        $parent->set_last_child( $ent);
        # the twig_current is now the parent
        delete $twig_current->{'twig_current'};
        $t->{twig_current}= $parent;
        # we left pcdata
        $t->{twig_in_pcdata}=0;
      }
    else
      { # create the node as a child of the current element
        $ent->set_parent( $twig_current);
        if( my $prev_sibling= $twig_current->{last_child})
          { $ent->set_prev_sibling( $prev_sibling);
            $prev_sibling->{next_sibling}=  $ent;
          }
        else
          { $twig_current->{first_child}=  $ent if( $twig_current); }
        $twig_current->set_last_child( $ent) if( $twig_current);
      }

    # meant to trigger entity handler, does not seem to be activated at this time
    #if( my $handler= $t->{twig_handlers}->{gi}->{'#ENT'})
    #  { local $_= $ent; $handler->( $t, $ent); }

    return $ent;
  }

sub parser
  { return $_[0]->{twig_parser}; }

# returns the declaration text (or a default one)
sub xmldecl
  { my $t= shift;
    return '' unless( $t->{twig_xmldecl} || $t->{output_encoding});
    my $decl_string;
    my $decl= $t->{twig_xmldecl};
    if( $decl)
      { my $version= $decl->{version};
        $decl_string= q{<?xml};
        $decl_string .= qq{ version="$version"};

        # encoding can either have been set (in $decl->{output_encoding})
        # or come from the document (in $decl->{encoding})
        if( $t->{output_encoding})
          { my $encoding= $t->{output_encoding};
            $decl_string .= qq{ encoding="$encoding"};
          }
        elsif( $decl->{encoding})
          { my $encoding= $decl->{encoding};
            $decl_string .= qq{ encoding="$encoding"};
          }
    
        if( defined( $decl->{standalone}))
          { $decl_string .= q{ standalone="};  
            $decl_string .= $decl->{standalone} ? "yes" : "no";  
            $decl_string .= q{"}; 
          }
      
        $decl_string .= "?>\n";
      }
    else
      { my $encoding= $t->{output_encoding};
        $decl_string= qq{<?xml version="1.0" encoding="$encoding"?>};
      }
      
    my $output_filter= XML::Twig::Elt::output_filter();
    return $output_filter ? $output_filter->( $decl_string) : $decl_string;
  }

# returns the doctype text (or none)
# that's the doctype just has it was in the original document
sub doctype
  { my $t= shift;
    my $doctype= $t->{twig_doctype} or return '';
    my $string= "<!DOCTYPE " . $doctype->{name};
    $string  .= qq{ SYSTEM "$doctype->{sysid}"} if( $doctype->{sysid});
    $string  .= qq{ PUBLIC  "$doctype->{pub}" } if( $doctype->{pub});
    if( $doctype->{internal})
      { # !@#$%^&* code to deal with various expat/XML::Parser versions
        $string.= " [" unless( $doctype->{internal}=~ m{^\s*\[});
        $string.= " " if( $doctype->{internal}=~ m{^\[});
        $string  .= $doctype->{internal};
        $string=~ s{\n?]?>?$}{\n]>};
      }
    return $string;
  }

sub set_doctype
  { my( $t, $name, $system, $public, $internal)= @_;
    $t->{twig_doctype}= {};
    my $doctype= $t->{twig_doctype};
    $doctype->{name}     = $name     if( defined $name);
    $doctype->{sysid}    = $system   if( defined $system);
    $doctype->{pub}      = $public   if( defined $public);
    $doctype->{internal} = $internal if( defined $internal);
  }

# return the dtd object
sub dtd
  { my $t= shift;
    return $t->{twig_dtd};
  }

# return an element model, or the list of element models
sub model
  { my $t= shift;
    my $elt= shift;
    return $t->dtd->{model}->{$elt} if( $elt);
    return sort keys %{$t->dtd->{model}};
  }

        
# return the entity_list object 
sub entity_list($)
  { my $t= shift;
    return $t->{twig_entity_list};
  }

# return the list of entity names 
sub entity_names($)
  { my $t= shift;
    return $t->entity_list->entity_names;
  }

# return the entity object 
sub entity($$)
  { my $t= shift;
    my $entity_name= shift;
    return $t->entity_list->ent( $entity_name);
  }


sub print_prolog
  { my $t= shift;
    my $fh=  _is_fh($_[0])  ? shift : $t->{twig_output_fh} || select() || \*STDOUT;
    no strict 'refs';
    print {$fh} $t->prolog( @_);
  }

sub prolog
  { my $t= shift;
    my %args= _normalize_args( @_);
    my $prolog='';

    return $prolog if( $t->{no_prolog});

    my $update_dtd = $args{UpdateDTD} || '';

    $prolog .= $t->xmldecl;
    return $prolog if( defined( $t->{no_dtd_output}));

    my $dtd='';
    
    my $doctype= $t->{twig_doctype};
    if( $update_dtd)
      { #use YAML; warn "doctype:\n", Dump( $doctype);
        if( $doctype)  
          { #warn "option 1-1\n";
            $dtd .= "<!DOCTYPE ".$doctype->{name};
            $dtd .= " PUBLIC  \"$doctype->{pub}\""  if( $doctype->{pub});
            $dtd .= " SYSTEM \"$doctype->{sysid}\"" if( $doctype->{sysid} && !$doctype->{pub});
            $dtd .= "[\n";
            # awfull hack, but at least it works a little better that what was there before
            if( my $internal= $doctype->{internal})
              { 
                # remove extra [ and ] (for some versions of XML::Parser/expat
                #$internal=~s{^\s*\[}{};
                #$internal=~s{\[\s*$}{};
                $internal=~ s{<! \s* ENTITY \s+ \w+ \s+ ( ("[^"]*"|'[^']*') \s* | SYSTEM [^>]*) >\s*}{}xg;
                $internal=~ s{^\n}{};
                $dtd.= $internal;
              }
            $dtd .= $t->entity_list->text ||'' if( $t->entity_list);
            $dtd .= "]>\n";
          }
        elsif( !$t->{'twig_dtd'} && keys %{$t->entity_list}) 
          { #warn "option 1-2\n";
            $dtd .= "<!DOCTYPE " . $t->root->gi . " [\n" . $t->entity_list->text . "\n]>"; }
        else
          { #warn "option 1-3\n";
            my $dtd= $t->{'twig_dtd'};
            $dtd .= $t->dtd_text;
          }            
      }
    elsif( $doctype)
      { #warn "option 2\n";
        $dtd .= "<!DOCTYPE ". $doctype->{name}  if( $doctype->{name});
        $dtd .= " PUBLIC \"$doctype->{pub}\""  if( $doctype->{pub});
        $dtd .= " SYSTEM" if( $doctype->{sysid} && !$doctype->{pub});
        $dtd .= ' "' . $doctype->{sysid} . '"'  if( $doctype->{sysid}); 
        if( my $internal= $doctype->{internal}) 
          { # add opening and closing brackets if not already there
            # plus some spaces and newlines for a nice formating
            # I test it here because I can't remember which version of
            # XML::Parser need it or not, nor guess which one will in the
            # future, so this about the best I can do
            #warn "option 2-int\n";
            #warn "internal: '$internal'\n";
            $internal=~ s{^\s*(\[\s*)?}{ [\n};
            $internal=~ s{\s*(\]\s*(>\s*)?)?\s*$}{\n]>\n};
            $dtd .=  $internal; 
          }
      }
      
    if( $dtd)
      {
        # terrible hack, as I can't figure out in which case the darn prolog
        # should get an extra > (depends on XML::Parser and expat versions)
        $dtd=~ s/(>\s*)*$/>\n/ if($dtd);

        $prolog .= $dtd;

        my $output_filter= XML::Twig::Elt::output_filter();
        return $output_filter ? $output_filter->( $prolog) : $prolog;
      }
    else
      { return $prolog; }
  }

sub _print_prolog_data
  { my $t= shift;
    my $fh=  _is_fh($_[0])  ? shift : undef;
    if( $fh) { print $fh $t->_prolog_data( @_); }
    else     { print $t->_prolog_data( @_);     }
  }

sub _prolog_data
  { my $t= shift;
    return''  unless( $t->{prolog_data});
    my $prolog_data_text='';
    foreach ( $t->{prolog_data}->children)
      { $prolog_data_text .= $_->sprint . "\n"; }
    return$ prolog_data_text;
  }

sub print
  { my $t= shift;
    my $fh=  _is_fh( $_[0])  ? shift : undef;
    my %args= _normalize_args( @_);

    my $old_pretty;
    if( defined $args{PrettyPrint})
      { $old_pretty= $t->set_pretty_print( $args{PrettyPrint}); 
        delete $args{PrettyPrint}; 
      }

     my $old_empty_tag_style;
     if( defined $args{EmptyTags})
      { $old_empty_tag_style= $t->set_empty_tag_style( $args{EmptyTags}); 
        delete $args{EmptyTags};
      }

    if( $fh) 
      { $t->print_prolog( $fh, %args); 
        $t->_print_prolog_data( $fh, %args);
      }
    else 
      { $t->print_prolog( %args);
        $t->_print_prolog_data( %args);
      }

    $t->{twig_root}->print( $fh) if( $t->{twig_root});
    $t->set_pretty_print( $old_pretty) if( defined $old_pretty); 
    $t->set_empty_tag_style( $old_empty_tag_style) if( defined $old_empty_tag_style); 
  }


sub flush
  { my $t= shift;
    my $fh=  _is_fh( $_[0]) ? shift : undef;
    my $old_select= defined $fh ? select $fh : undef;
    my $up_to= ref $_[0] ? shift : undef;
    my %args= _normalize_args( @_);

    my $old_pretty;
    if( defined $args{PrettyPrint})
      { $old_pretty= $t->set_pretty_print( $args{PrettyPrint}); 
        delete $args{PrettyPrint};
      }

     my $old_empty_tag_style;
     if( $args{EmptyTags})
      { $old_empty_tag_style= $t->set_empty_tag_style( $args{EmptyTags}); 
        delete $args{EmptyTags};
      }


    # the "real" last element processed, as _twig_end has closed it
    my $last_elt;
    if( $up_to)
      { $last_elt= $up_to; }
    elsif( $t->{twig_current})
      { $last_elt= $t->{twig_current}->_last_child; }
    else
      { $last_elt= $t->{twig_root}; }

    # flush the DTD unless it has ready flushed (ie root has been flushed)
    my $elt= $t->{twig_root};
    $t->print_prolog( %args) unless( $elt->_flushed);

    while( $elt)
      { my $next_elt; 
        if( $last_elt && $last_elt->in( $elt))
          { 
            unless( $elt->_flushed) 
              { # just output the front tag
                print $elt->start_tag();
                $elt->_set_flushed;
              }
            $next_elt= $elt->{first_child};
          }
        else
          { # an element before the last one or the last one,
            $next_elt= $elt->{next_sibling};  
            $elt->_flush();
            $elt->delete; 
            last if( $last_elt && ($elt == $last_elt));
          }
        $elt= $next_elt;
      }
    select $old_select if( defined $old_select);
    $t->set_pretty_print( $old_pretty) if( defined $old_pretty); 
    $t->set_empty_tag_style( $old_empty_tag_style) if( defined $old_empty_tag_style); 
  }


# flushes up to an element
# this method just reorders the arguments and calls flush
sub flush_up_to
  { my $t= shift;
    my $up_to= shift;
    if( _is_fh( $_[0]))
      { my $fh=  shift;
        $t->flush( $fh, $up_to, @_);
      }
    else
      { $t->flush( $up_to, @_); }
  }

    
# same as print except the entire document text is returned as a string
sub sprint
  { my $t= shift;
    my %args= _normalize_args( @_);

    my $old_pretty;
    if( defined $args{PrettyPrint})
      { $old_pretty= $t->set_pretty_print( $args{PrettyPrint}); 
        delete $args{PrettyPrint};
      }

     my $old_empty_tag_style;
     if( defined $args{EmptyTags})
      { $old_empty_tag_style= $t->set_empty_tag_style( $args{EmptyTags}); 
        delete $args{EmptyTags};
      }
      
    my $prolog= $t->prolog( %args) || '';
    my $prolog_data= $t->_prolog_data( %args) || '';
    
    my $string=  $prolog . $prolog_data . $t->{twig_root}->sprint;

    $t->set_pretty_print( $old_pretty) if( defined $old_pretty); 
    $t->set_empty_tag_style( $old_empty_tag_style) if( defined $old_empty_tag_style); 

    return $string;
  }
    

# this method discards useless elements in a tree
# it does the same thing as a flush except it does not print it
# the second argument is an element, the last purged element
# (this argument is usually set through the purge_up_to method)
sub purge
  { my $t= shift;
    my $up_to= shift;

    # the "real" last element processed, as _twig_end has closed it
    my $last_elt;
    if( $up_to)
      { $last_elt= $up_to; }
    elsif( $t->{twig_current})
      { $last_elt= $t->{twig_current}->_last_child; }
    else
      { $last_elt= $t->{twig_root}; }
    
    my $elt= $t->{twig_root};

    while( $elt)
      { my $next_elt; 
        if( $last_elt && $last_elt->in( $elt))
          { $elt->_set_flushed;
            $next_elt= $elt->{first_child};
          }
        else
          { # an element before the last one or the last one,
            $next_elt= $elt->{next_sibling};  
            $elt->delete; 
            last if( $last_elt && ($elt == $last_elt) );
          }
        $elt= $next_elt;
      }
  }
    
# flushes up to an element. This method just calls purge
sub purge_up_to
  { my $t= shift;
    $t->purge( @_);
  }

sub root
  { return $_[0]->{twig_root}; }

# create accessor methods on attribute names
sub create_accessors
  { my $twig_or_class= shift;
    my $elt_class= ref $twig_or_class ? $twig_or_class->{twig_elt_class}
                                      : 'XML::Twig::Elt'
                                      ;
    no strict 'refs';
    foreach my $att (@_)
      { croak "attempt to redefine existing method $att using create_accessors"
          if( $elt_class->can( $att));
        *{"$elt_class\::$att"}= 
            sub :lvalue
              { my $elt= shift;
                if( @_) { $elt->{att}->{$att}= $_[0]; }
                $elt->{att}->{$att}; 
              };
     }
  }


#start-extract twig_document (used to generate XML::(DOM|GDOME)::Twig)
sub first_elt
  { my( $t, $cond)= @_;
    my $root= $t->root || return undef;
    return $root if( $root->passes( $cond));
    return $root->next_elt( $cond); 
  }

sub last_elt
  { my( $t, $cond)= @_;
    my $root= $t->root || return undef;
    return $root->last_descendant( $cond); 
  }


sub next_n_elt
  { my( $t, $offset, $cond)= @_;
    $offset -- if( $t->root->matches( $cond) );
    return $t->root->next_n_elt( $offset, $cond);
  }

sub get_xpath
  { my $twig= shift;
    if( isa( $_[0], 'ARRAY'))
      { my $elt_array= shift;
        return _unique_elts( map { $_->get_xpath( @_) } @$elt_array);
      }
    else
      { return $twig->root->get_xpath( @_); }
  }

# get a list of elts and return a sorted list of unique elts
sub _unique_elts
  { my @sorted= sort { $a ->cmp( $b) } @_;
    my @unique;
    while( my $current= shift @sorted)
      { push @unique, $current unless( @unique && ($unique[-1] == $current)); }
    return @unique;
  }

sub findvalue
  { my $twig= shift;
    if( isa( $_[0], 'ARRAY'))
      { my $elt_array= shift;
        return join( '', map { $_->findvalue( @_) } @$elt_array);
      }
    else
      { return $twig->root->findvalue( @_); }
  }

sub set_id_seed
  { my $t= shift;
    XML::Twig::Elt->set_id_seed( @_);
  }

# return an array ref to an index, or undef
sub index
  { my( $twig, $name, $index)= @_;
    return defined( $index) ? $twig->{_twig_index}->{$name}->[$index] : $twig->{_twig_index}->{$name};
  }

# return a list with just the root
# if a condition is given then return an empty list unless the root matches
sub children
  { my( $t, $cond)= @_;
    my $root= $t->root;
    unless( $cond && !($root->passes( $cond)) )
      { return ($root); }
    else
      { return (); }
  }

sub _children { return ($_[0]->root); }

sub descendants
  { my( $t, $cond)= @_;
    my $root= $t->root;
    if( $root->passes( $cond) )
      { return ($root, $root->descendants( $cond)); }
    else
      { return ( $root->descendants( $cond)); }
  }

sub simplify  { my $t= shift; $t->root->simplify( @_);  }
sub subs_text { my $t= shift; $t->root->subs_text( @_); }
sub trim      { my $t= shift; $t->root->trim( @_);      }

 #end-extract twig_document

sub set_keep_encoding
  { return XML::Twig::Elt::set_keep_encoding( @_); }

sub set_expand_external_entities
  { return XML::Twig::Elt::set_expand_external_entities( @_); }

# WARNING: at the moment the id list is not updated reliably
sub elt_id
  { return $_[0]->{twig_id_list}->{$_[1]}; }

# change it in ALL twigs at the moment
sub change_gi 
  { my( $twig, $old_gi, $new_gi)= @_;
    my $index;
    return unless($index= $XML::Twig::gi2index{$old_gi});
    $XML::Twig::index2gi[$index]= $new_gi;
    delete $XML::Twig::gi2index{$old_gi};
    $XML::Twig::gi2index{$new_gi}= $index;
  }


# builds the DTD from the stored (possibly updated) data
sub dtd_text
  { my $t= shift;
    my $dtd= $t->{twig_dtd};
    my $doctype= $t->{twig_doctype} or return '';
    my $string= "<!DOCTYPE ".$doctype->{name};

    $string .= " [\n";

    foreach my $gi (@{$dtd->{elt_list}})
      { $string.= "<!ELEMENT $gi ".$dtd->{model}->{$gi}.">\n" ;
        if( $dtd->{att}->{$gi})
          { my $attlist= $dtd->{att}->{$gi};
            $string.= "<!ATTLIST $gi\n";
            foreach my $att ( sort keys %{$attlist})
              { 
                if( $attlist->{$att}->{fixed})
                  { $string.= "   $att $attlist->{$att}->{type} #FIXED $attlist->{$att}->{default}"; }
                else
                  { $string.= "   $att $attlist->{$att}->{type} $attlist->{$att}->{default}"; }
                $string.= "\n";
              }
            $string.= ">\n";
          }
      }
    $string.= $t->entity_list->text if( $t->entity_list);
    $string.= "\n]>\n";
    return $string;
  }
        
# prints the DTD from the stored (possibly updated) data
sub dtd_print
  { my $t= shift;
    my $fh=  _is_fh( $_[0])  ? shift : undef;
    if( $fh) { print $fh $t->dtd_text; }
    else     { print $t->dtd_text; }
  }

# build the subs that call directly expat
BEGIN
  { my @expat_methods= qw( depth in_element within_element context
                           current_line current_column current_byte
                           recognized_string original_string 
                           xpcroak xpcarp 
                           xml_escape
                           base current_element element_index 
                           position_in_context);
    foreach my $method (@expat_methods)
      { no strict 'refs';
        *{$method}= sub { my $t= shift;
                          croak "calling $method after parsing is finished" 
                                 unless( $t->{twig_parsing}); 
                          return $t->{twig_parser}->$method(@_); 
                        };
      }
  }

sub path
  { my( $t, $gi)= @_;
    if( $t->{twig_map_xmlns})
      { return "/" . join( "/", map { $t->_replace_prefix( $_)} ($t->{twig_parser}->context, $gi)); }
    else
      { return "/" . join( "/", ($t->{twig_parser}->context, $gi)); }
  }

sub finish
  { my $t= shift;
    return $t->{twig_parser}->finish;
  }

# just finish the parse by printing the rest of the document
sub finish_print
  { my( $t, $fh)= @_;
    my $old_fh;
    unless( defined $fh)
      { $t->_set_fh_to_twig_output_fh(); }
    elsif( defined $fh)
      { $old_fh= select $fh; 
        $t->{twig_original_selected_fh}= $old_fh if( $old_fh); 
      }
    
    my $p=$t->{twig_parser};
    if( $t->{twig_keep_encoding})
      { $p->setHandlers( %twig_handlers_finish_print); }
    else
      { $p->setHandlers( %twig_handlers_finish_print_original); }
  }

sub set_remove_cdata { return XML::Twig::Elt::set_remove_cdata( @_); }

sub output_filter     { return XML::Twig::Elt::output_filter( @_);     }
sub set_output_filter { return XML::Twig::Elt::set_output_filter( @_); }

sub output_text_filter { return XML::Twig::Elt::output_text_filter( @_); }
sub set_output_text_filter { return XML::Twig::Elt::set_output_text_filter( @_); }

sub set_input_filter
  { my( $t, $input_filter)= @_;
    my $old_filter= $t->{twig_input_filter};
      if( !$input_filter || isa( $input_filter, 'CODE') )
        { $t->{twig_input_filter}= $input_filter; }
      elsif( $input_filter eq 'latin1')
        {  $t->{twig_input_filter}= latin1(); }
      elsif( $filter{$input_filter})
        {  $t->{twig_input_filter}= $filter{$input_filter}; }
      else
        { croak "invalid input filter: $input_filter"; }
      
      return $old_filter;
    }

sub set_empty_tag_style
  { return XML::Twig::Elt::set_empty_tag_style( @_); }

sub set_pretty_print
  { return XML::Twig::Elt::set_pretty_print( @_); }

sub set_quote
  { return XML::Twig::Elt::set_quote( @_); }

sub set_indent
  { return XML::Twig::Elt::set_indent( @_); }

sub set_keep_atts_order
  { shift; return XML::Twig::Elt::set_keep_atts_order( @_); }

sub keep_atts_order
  { return XML::Twig::Elt::keep_atts_order( @_); }

sub set_do_not_escape_amp_in_atts
  { return XML::Twig::Elt::set_do_not_escape_amp_in_atts( @_); }

# save and restore package globals (the ones in XML::Twig::Elt)
sub save_global_state
  { my $t= shift;
    $t->{twig_saved_state}= XML::Twig::Elt::global_state();
  }

sub restore_global_state
  { my $t= shift;
    XML::Twig::Elt::set_global_state( $t->{twig_saved_state});
  }

sub global_state
  { return XML::Twig::Elt::global_state(); }

sub set_global_state
  {  return XML::Twig::Elt::set_global_state( $_[1]); }

sub dispose
  { my $t= shift;
    $t->DESTROY;
  }
  
sub DESTROY
  { my $t= shift;
    if( $t->{twig_root} && isa(  $t->{twig_root}, 'XML::Twig')) 
      { $t->{twig_root}->delete } 

    # added to break circular references
    undef $t->{twig};
    undef $t->{twig_root}->{twig} if( $t->{twig_root});
    undef $t->{twig_parser};
    
    $t={}; # prevents memory leaks (especially when using mod_perl)
    undef $t;
  }        


#
#  non standard handlers
#

# kludge: expat 1.95.2 calls both Default AND Doctype handlers
# so if the default handler finds '<!DOCTYPE' then it must 
# unset itself (_twig_print_doctype will reset it)
sub _twig_print_check_doctype
  { 
    my $p= shift;
    my $string= $p->recognized_string();
    if( $string eq '<!DOCTYPE') 
      { $p->setHandlers( Default => undef); 
        $p->{twig}->{expat_1_95_2}=1; 
      }
    else                        
      { print $string; }
    
  }

sub _twig_print
  { print $_[0]->recognized_string(); }

# recognized_string does not seem to work for entities, go figure!
# so this handler is not used 
sub _twig_print_entity
  { my $p= shift; }

# kludge: expat 1.95.2 calls both Default AND Doctype handlers
# so if the default handler finds '<!DOCTYPE' then it must 
# unset itself (_twig_print_doctype will reset it)
sub _twig_print_original_check_doctype
  { 
    my $p= shift;
    my $string= $p->original_string();
    if( $string eq '<!DOCTYPE') 
      { $p->setHandlers( Default => undef); 
        $p->{twig}->{expat_1_95_2}=1; 
      }
    else                        
      { print $string; }
    
  }

sub _twig_print_original
  { print $_[0]->original_string(); }


sub _twig_print_original_doctype
  { 
    my(  $p, $name, $sysid, $pubid, $internal)= @_;
    if( $name)
      { # with recent versions of XML::Parser original_string does not work,
        # hence we need to rebuild the doctype declaration
        my $doctype='';
        $doctype .= qq{<!DOCTYPE $name}    if( $name);
        $doctype .=  qq{ PUBLIC  "$pubid"}  if( $pubid);
        $doctype .=  qq{ SYSTEM}            if( $sysid && !$pubid);
        $doctype .=  qq{ "$sysid"}          if( $sysid); 
        $doctype .=  qq{>} unless( $p->{twig}->{expat_1_95_2});
        print $doctype;
      }
    $p->setHandlers( Default => \&_twig_print_original);
  }

sub _twig_print_doctype
  { 
    my(  $p, $name, $sysid, $pubid, $internal)= @_;
    if( $name)
      { # with recent versions of XML::Parser original_string does not work,
        # hence we need to rebuild the doctype declaration
        my $doctype='';
        $doctype .= qq{<!DOCTYPE $name}    if( $name);
        $doctype .=  qq{ PUBLIC  "$pubid"}  if( $pubid);
        $doctype .=  qq{ SYSTEM}            if( $sysid && !$pubid);
        $doctype .=  qq{ "$sysid"}          if( $sysid); 
        $doctype .=  qq{>} unless( $p->{twig}->{expat_1_95_2});
        print $doctype;
      }
    $p->setHandlers( Default => \&_twig_print_original);
  }


sub _twig_print_original_default
  { 
    my $p= shift;
    print $p->original_string();
  }

# account for the case where the element is empty
sub _twig_print_end_original
  { my $p= shift;
    print $p->original_string();
  }

sub _twig_start_check_roots
  { 
    my( $p, $gi, %att)= @_;
    my $t= $p->{twig};

    
    # $tag will always be true if it needs to be printed (the tag string is never empty)
    my $tag= $t->{twig_default_print} ? $t->{twig_keep_encoding} ? $p->original_string
                                                                 : $p->recognized_string
                                      : '';
    my $fh= $t->{twig_output_fh} || select() || \*STDOUT;

    if( _handler( $t, $t->{twig_roots}, $gi, \%att))
      { $p->setHandlers( %twig_handlers); # restore regular handlers
        $t->{twig_root_depth}= $p->depth; 
        _twig_start( $p, $gi, %att);
      }
    elsif( $p->depth == 0)
      { no strict 'refs';
        print {$fh} $tag if( $tag);
        _twig_start( $p, $gi, %att);
      }
    elsif( $t->{twig_starttag_handlers})
      { # look for start tag handlers

        if( $t->{twig_map_xmlns})
          { my @att= splice( @_, 2);
            _replace_ns( $t, \$gi, \@att);
            %att= @att;
          }

        my @handlers= _handler( $t, $t->{twig_starttag_handlers}, $gi, \%att);
        my $last_handler_res;
        foreach my $handler ( @handlers)
          { $last_handler_res= $handler->($t, $gi, %att);
            last unless $last_handler_res;
          }
        no strict 'refs';
        print {$fh} $tag if( $tag && (!@handlers || $last_handler_res));   
      }
    else
      { no strict 'refs';
        print {$fh} $tag if( $tag); 
      }  
  }

sub _twig_end_check_roots
  { 
    my( $p, $gi, %att)= @_;
    my $t= $p->{twig};
    # $tag can be empty (<elt/>), hence the undef and the tests for defined
    my $tag= $t->{twig_default_print} ? $t->{twig_keep_encoding} ? $p->original_string
                                                                 : $p->recognized_string
                                      : undef;
    my $fh= $t->{twig_output_fh} || select() || \*STDOUT;
    
    if( $t->{twig_endtag_handlers})
      { # look for start tag handlers
        my @handlers= _handler( $t, $t->{twig_endtag_handlers}, $gi, {});
        my $last_handler_res=1;
        foreach my $handler ( @handlers)
          { $last_handler_res= $handler->($t, $gi) || last; }
        return unless $last_handler_res;
      }
    {
      no strict 'refs';
      print {$fh} $tag if( defined( $tag));
    }
    if( $p->depth == 0)
      { _twig_end( $p, $gi);  }
  }

sub _twig_pi_check_roots
  { my( $p, $target, $data)= @_;
    my $t= $p->{twig};
    my $pi= $t->{twig_default_print} ? $t->{twig_keep_encoding} ? $p->original_string
                                                                : $p->recognized_string
                                    : undef;
    my $fh= $t->{twig_output_fh} || select() || \*STDOUT;
    
    if( my $handler=    $t->{twig_handlers}->{pi_handlers}->{$target}
                     || $t->{twig_handlers}->{pi_handlers}->{''}
      )
      { # if handler is called on pi, then it needs to be processed as a regular node
        my @flags= qw( twig_process_pi twig_keep_pi);
        my @save= @{$t}{@flags}; # save pi related flags
        @{$t}{@flags}= (1, 0);   # override them, pi needs to be processed
        _twig_pi( @_);           # call handler on the pi
        @{$t}{@flags}= @save;;   # restore flag
      }
    else
      { no strict 'refs';
        print  {$fh} $pi if( defined( $pi));
      }
  }


sub _twig_ignore_start
  { 
    my( $p, $gi)= @_;
    my $t= $p->{twig};
    return unless( $gi eq $t->{twig_ignore_gi});
    $t->{twig_ignore_level}++;
    my $action= $t->{twig_ignore_action};
    if( $action eq 'print' )
      { _twig_print_original( @_); }
#    elsif( $action eq 'string' )
#      { $t->{twig_buffered_string} .= $p->original_string(); }
  }

sub _twig_ignore_end
  { 
    my( $p, $gi)= @_;
    my $t= $p->{twig};

    my $action= $t->{twig_ignore_action};

    if( $action eq 'print')
      { _twig_print_original( $p, $gi); }
#    elsif( $action eq 'string')
#      { $t->{twig_buffered_string} .= $p->original_string(); }

    return unless( $gi eq $t->{twig_ignore_gi});

    $t->{twig_ignore_level}--;

    unless( $t->{twig_ignore_level})
      { $t->{twig_ignore_elt}->delete; 
        $p->setHandlers( @{$t->{twig_saved_handlers}});
        # test for handlers
        if( $t->{twig_endtag_handlers})
          { # look for end tag handlers
            my @handlers= _handler( $t, $t->{twig_endtag_handlers}, $gi, {});
            my $last_handler_res=1;
            foreach my $handler ( @handlers)
              { $last_handler_res= $handler->($t, $gi) || last; }
          }
      };
  }
    
sub ignore
  { my $t= shift;
    my $elt;

    # get the element (default: current elt)
    if( $_[0] && isa( $_[0], 'XML::Twig::Elt'))
      { $elt= shift; }
    else
      { $elt = $t->{twig_current}; }

    $t->{twig_current}= $elt->{parent};
    $t->{twig_current}->set_twig_current;

    my $action= shift || 1; 
    $t->{twig_ignore_action}= $action;

    $t->{twig_ignore_elt}= $elt;     # save it
    $t->{twig_ignore_gi}= $XML::Twig::index2gi[$elt->{'gi'}];  # save its gi
    $t->{twig_ignore_level}++;
    my $p= $t->{twig_parser};
    my @saved_handlers= $p->setHandlers( %twig_handlers_ignore); # set handlers
    if( $action eq 'print')
      { $p->setHandlers( Default => \&_twig_print_original); }
#    elsif( $action eq 'string')
#      { # not used at the moment
#        $t->{twig_buffered_string}='';
#        $p->setHandlers( Default => \&twig_buffer_original);
#      }

    $t->{twig_saved_handlers}= \@saved_handlers;        # save current handlers
  }

# select $t->{twig_output_fh} and store the current selected fh 
sub _set_fh_to_twig_output_fh
  { my $t= shift;
    my $output_fh= $t->{twig_output_fh};
    if( $output_fh && !$t->{twig_output_fh_selected})
      { # there is an output fh
        $t->{twig_selected_fh}= select(); # store the currently selected fh
        $t->{twig_output_fh_selected}=1;
        select $output_fh;                # select the output fh for the twig
      }
  }

# select the fh that was stored in $t->{twig_selected_fh} 
# (before $t->{twig_output_fh} was selected)
sub _set_fh_to_selected_fh
  { my $t= shift;
    return unless( $t->{twig_output_fh});
    my $selected_fh= $t->{twig_selected_fh};
    $t->{twig_output_fh_selected}=0;
    select $selected_fh;
    return;
  }
  

sub encoding
  { return $_[0]->{twig_xmldecl}->{encoding} if( $_[0]->{twig_xmldecl}); }

sub set_encoding
  { my( $t, $encoding)= @_;
    $t->{twig_xmldecl} ||={};
    $t->set_xml_version( "1.0") unless( $t->xml_version);
    $t->{twig_xmldecl}->{encoding}= $encoding;
    return $t;
  }

sub output_encoding
  { return $_[0]->{output_encoding}; }
  
sub set_output_encoding
  { my( $t, $encoding)= @_;
    $t->set_output_filter( _encoding_filter( $encoding)) if( $encoding);
    return $t->{output_encoding}= $encoding;
  }

sub xml_version
  { return $_[0]->{twig_xmldecl}->{version} if( $_[0]->{twig_xmldecl}); }

sub set_xml_version
  { my( $t, $version)= @_;
    $t->{twig_xmldecl} ||={};
    return $t->{twig_xmldecl}->{version}= $version;
  }

sub standalone
  { return $_[0]->{twig_xmldecl}->{standalone} if( $_[0]->{twig_xmldecl}); }

sub set_standalone
  { my( $t, $standalone)= @_;
    $t->{twig_xmldecl} ||={};
    $t->set_xml_version( "1.0") unless( $t->xml_version);
    return $t->{twig_xmldecl}->{standalone}= $standalone;
  }


# SAX methods

sub toSAX1
  { croak "cannot use toSAX1 while parsing (use flush_toSAX1)" if (defined $_[0]->{twig_parser});
    shift(@_)->_toSAX(@_, \&XML::Twig::Elt::_start_tag_data_SAX1,
                          \&XML::Twig::Elt::_end_tag_data_SAX1
             ); }

sub toSAX2
  { croak "cannot use toSAX2 while parsing (use flush_toSAX2)" if (defined $_[0]->{twig_parser});
    shift(@_)->_toSAX(@_, \&XML::Twig::Elt::_start_tag_data_SAX2,
                          \&XML::Twig::Elt::_end_tag_data_SAX2
             ); }


sub _toSAX
  { my( $t, $handler, $start_tag_data, $end_tag_data) = @_;

    if( my $start_document =  $handler->can( 'start_document'))
      { $start_document->( $handler); }
    
    $t->_prolog_toSAX( $handler);
    
    $t->root->_toSAX( $handler, $start_tag_data, $end_tag_data)  if( $t->root);
    if( my $end_document =  $handler->can( 'end_document'))
      { $end_document->( $handler); }
  }


sub flush_toSAX1
  { shift(@_)->_flush_toSAX(@_, \&XML::Twig::Elt::_start_tag_data_SAX1,
                               \&XML::Twig::Elt::_end_tag_data_SAX1
             ); }

sub flush_toSAX2
  { shift(@_)->_flush_toSAX(@_, \&XML::Twig::Elt::_start_tag_data_SAX2,
                               \&XML::Twig::Elt::_end_tag_data_SAX2
             ); }

sub _flush_toSAX
  { my( $t, $handler, $start_tag_data, $end_tag_data, $up_to)= @_;

    # the "real" last element processed, as _twig_end has closed it
    my $last_elt;
    if( $up_to)
      { $last_elt= $up_to; }
    elsif( $t->{twig_current})
      { $last_elt= $t->{twig_current}->_last_child; }
    else
      { $last_elt= $t->{twig_root}; }

    my $elt= $t->{twig_root};
    unless( $elt->_flushed)
      { # init unless already done (ie root has been flushed)
        if( my $start_document =  $handler->can( 'start_document'))
          { $start_document->( $handler); }
        # flush the DTD
        $t->_prolog_toSAX( $handler) 
      }

    while( $elt)
      { my $next_elt; 
        if( $last_elt && $last_elt->in( $elt))
          { 
            unless( $elt->_flushed) 
              { # just output the front tag
                if( my $start_element = $handler->can( 'start_element'))
                 { if( my $tag_data= $start_tag_data->( $elt))
                     { $start_element->( $handler, $tag_data); }
                 }
                $elt->_set_flushed;
              }
            $next_elt= $elt->{first_child};
          }
        else
          { # an element before the last one or the last one,
            $next_elt= $elt->{next_sibling};  
            $elt->_toSAX( $handler, $start_tag_data, $end_tag_data);
            $elt->delete; 
            last if( $last_elt && ($elt == $last_elt));
          }
        $elt= $next_elt;
      }
    if( !$t->{twig_parsing}) 
      { if( my $end_document =  $handler->can( 'end_document'))
          { $end_document->( $handler); }
      }
  }


sub _prolog_toSAX
  { my( $t, $handler)= @_;
    $t->_xmldecl_toSAX( $handler);
    $t->_DTD_toSAX( $handler);
  }

sub _xmldecl_toSAX
  { my( $t, $handler)= @_;
    my $decl= $t->{twig_xmldecl};
    my $data= { Version    => $decl->{version},
                Encoding   => $decl->{encoding},
                Standalone => $decl->{standalone},
          };
    if( my $xml_decl= $handler->can( 'xml_decl'))
      { $xml_decl->( $handler, $data); }
  }
                
sub _DTD_toSAX
  { my( $t, $handler)= @_;
    my $doctype= $t->{twig_doctype};
    return unless( $doctype);
    my $data= { Name     => $doctype->{name},
                PublicId => $doctype->{pub},
                SystemId => $doctype->{sysid},
              };

    if( my $start_dtd= $handler->can( 'start_dtd'))
      { $start_dtd->( $handler, $data); }

    # I should call code to export the internal subset here 
    
    if( my $end_dtd= $handler->can( 'end_dtd'))
      { $end_dtd->( $handler); }
  }

# input/output filters

sub latin1 
  { local $SIG{__DIE__};
    if( eval 'require Encode')
      { import Encode; 
        return encode_convert( 'ISO-8859-15');
      }
    elsif( eval 'require Text::Iconv;')
      { 
        return iconv_convert( 'ISO-8859-15');
      }
    elsif( eval 'require Unicode::Map8 && require Unicode::String;')
      { 
        return unicode_convert( 'ISO-8859-15'); 
      }
    else
      { return \&regexp2latin1; }
  }

sub _encoding_filter
  { 
      { local $SIG{__DIE__};
        my $encoding= $_[1] || $_[0];
        if( eval 'require Encode')
          { import Encode; 
            my $sub= encode_convert( $encoding);
            return $sub;
          }
        elsif( eval 'require Text::Iconv;')
          { return iconv_convert( $encoding); }
        elsif( eval 'require Unicode::Map8 && require Unicode::String;')
          { return unicode_convert( $encoding); }
        }
    croak "Encode, Text::Iconv or Unicode::Map8 and Unicode::String need to be installed ",
          "in order to use encoding options";
  }

# shamelessly lifted from XML::TyePYX (works only with XML::Parse 2.27)
sub regexp2latin1
  { my $text=shift;
    $text=~s{([\xc0-\xc3])(.)}{ my $hi = ord($1);
                                my $lo = ord($2);
                                chr((($hi & 0x03) <<6) | ($lo & 0x3F))
                              }ge;
    return $text;
  }


sub html_encode
  { require HTML::Entities;
    return HTML::Entities::encode_entities($_[0] );
  }

sub safe_encode
  {   my $str= shift;
      if( $] < 5.008)
        { $str =~ s{([\xC0-\xDF].|[\xE0-\xEF]..|[\xF0-\xFF]...)}
                   {_XmlUtf8Decode($1)}egs; 
        }
      else
        { $str= encode( ascii => $str, $FB_HTMLCREF); }
      return $str;
  }

sub safe_encode_hex
  {   my $str= shift;
      if( $] < 5.008)
        { $str =~ s{([\xC0-\xDF].|[\xE0-\xEF]..|[\xF0-\xFF]...)}
                   {_XmlUtf8Decode($1, 1)}egs; 
        }
      else
        { $str= encode( ascii => $str, $FB_XMLCREF); }
      return $str;
  }

# this one shamelessly lifted from XML::DOM
# does NOT work on 5.8.0
sub _XmlUtf8Decode
  { my ($str, $hex) = @_;
    my $len = length ($str);
    my $n;

    if ($len == 2)
      { my @n = unpack "C2", $str;
        $n = (($n[0] & 0x3f) << 6) + ($n[1] & 0x3f);
      }
    elsif ($len == 3)
      { my @n = unpack "C3", $str;
        $n = (($n[0] & 0x1f) << 12) + (($n[1] & 0x3f) << 6) + ($n[2] & 0x3f);
      }
    elsif ($len == 4)
      { my @n = unpack "C4", $str;
        $n = (($n[0] & 0x0f) << 18) + (($n[1] & 0x3f) << 12) 
           + (($n[2] & 0x3f) << 6) + ($n[3] & 0x3f);
      }
    elsif ($len == 1)    # just to be complete...
      { $n = ord ($str); }
    else
      { croak "bad value [$str] for _XmlUtf8Decode"; }

    my $char= $hex ? sprintf ("&#x%x;", $n) : "&#$n;";
    return $char;
}


sub unicode_convert
  { my $enc= $_[1] ? $_[1] : $_[0]; # so the method can be called on the twig or directly
    require Unicode::Map8;
    require Unicode::String;
    import Unicode::String qw(utf8);
    my $sub= eval q{
            { my $cnv;
              BEGIN {  $cnv= Unicode::Map8->new($enc) 
                           or croak "Can't create converter to $enc";
                    }
              sub { return  $cnv->to8 (utf8($_[0])->ucs2); } 
            } 
                   };
    unless( $sub) { croak $@; }
    return $sub;
  }

sub iconv_convert
  { my $enc= $_[1] ? $_[1] : $_[0]; # so the method can be called on the twig or directly
    require Text::Iconv;
    my $sub= eval q{
            { my $cnv;
              BEGIN { $cnv = Text::Iconv->new( 'utf8', $enc) 
                           or croak "Can't create iconv converter to $enc";
                    }
              sub { return  $cnv->convert( $_[0]); } 
            }       
                   };
    unless( $sub)
      { if( $@=~ m{^Unsupported conversion: Invalid argument})
          { croak "Unsupported encoding: $enc"; }
        else
          { croak $@; }
      }

    return $sub;
  }

sub encode_convert
  { my $enc= $_[1] ? $_[1] : $_[0]; # so the method can be called on the twig or directly
    my $sub=  eval qq{sub { return encode( "$enc", \$_[0]); } };
    croak "can't create Encode-based filter: $@" unless( $sub);
    return $sub;
  }


# XML::XPath compatibility
sub getRootNode        { return $_[0]; }
sub getParentNode      { return undef; }
sub getChildNodes      { my @children= ($_[0]->root); return wantarray ? @children : \@children; }


1;

######################################################################
package XML::Twig::Entity_list;
######################################################################
*isa = \&UNIVERSAL::isa;

sub new
  { my $class = shift;
    my $self={ entities => {}, updated => 0};

    bless $self, $class;
    return $self;

  }

sub add_new_ent
  { my $ent_list= shift;
    my $ent= XML::Twig::Entity->new( @_);
    $ent_list->add( $ent);
    return $ent_list;
  }

sub _add_list
  { my( $ent_list, $to_add)= @_;
    my $ents_to_add= $to_add->{entities};
    return $ent_list unless( $ents_to_add && %$ents_to_add);
    @{$ent_list->{entities}}{keys %$ents_to_add}= values %$ents_to_add;
    $ent_list->{updated}=1;
    return $ent_list;
  }

sub add
  { my( $ent_list, $ent)= @_;
    $ent_list->{entities}->{$ent->{name}}= $ent;
    $ent_list->{updated}=1;
    return $ent_list;
  }

sub ent
  { my( $ent_list, $ent_name)= @_;
    return $ent_list->{entities}->{$ent_name};
  }

# can be called with an entity or with an entity name
sub delete
  { my $ent_list= shift;
    if( isa( ref $_[0], 'XML::Twig::Entity'))
      { # the second arg is an entity
        my $ent= shift;
        delete $ent_list->{entities}->{$ent->{name}};
      }
    else
      { # the second arg was not entity, must be a string then
        my $name= shift;
        delete $ent_list->{entities}->{$name};
      }
    $ent_list->{updated}=1;
    return $ent_list;
  }

sub print
  { my ($ent_list, $fh)= @_;
    my $old_select= defined $fh ? select $fh : undef;

    foreach my $ent_name ( sort keys %{$ent_list->{entities}})
      { my $ent= $ent_list->{entities}->{$ent_name};
        # we have to test what the entity is or un-defined entities can creep in
        $ent->print() if( isa( $ent, 'XML::Twig::Entity'));
      }
    select $old_select if( defined $old_select);
    return $ent_list;
  }

sub text
  { my ($ent_list)= @_;
    return join "\n", map { $ent_list->{entities}->{$_}->text} sort keys %{$ent_list->{entities}};
  }

# return the list of entity names 
sub entity_names($)
  { my $ent_list= shift;
    return sort keys %{$ent_list->{entities}} ;
  }


sub list
  { my ($ent_list)= @_;
    return map { $ent_list->{entities}->{$_} } sort keys %{$ent_list->{entities}};
  }

1;

######################################################################
package XML::Twig::Entity;
######################################################################
*isa = \&UNIVERSAL::isa;

sub new
  { my( $ent, $name, $val, $sysid, $pubid, $ndata)= @_;

    my $self={};

    $self->{name}= $name;
    if( $val)
      { $self->{val}= $val; }
    else
      { $self->{sysid}= $sysid;
        $self->{pubid}= $pubid;
        $self->{ndata}= $ndata;
      }
    bless $self;
    return $self;
  }

sub name  { return $_[0]->{name}; }
sub val   { return $_[0]->{val}; }
sub sysid { return $_[0]->{sysid}; }
sub pubid { return $_[0]->{pubid}; }
sub ndata { return $_[0]->{ndata}; }

sub print
  { my ($ent, $fh)= @_;
    if( $fh) { print $fh $ent->text . "\n"; }
    else     { print $ent->text . "\n"; }
  }


sub text
  { my ($ent)= @_;
    if( exists $ent->{'val'})
      { if( $ent->{'val'}=~ /"/)
          { return "<!ENTITY $ent->{'name'} '$ent->{'val'}'>"; }
        return "<!ENTITY $ent->{'name'} \"$ent->{'val'}\">";
      }
    elsif( $ent->{'sysid'})
      { my $text= "<!ENTITY $ent->{'name'} ";
        $text .= "SYSTEM \"$ent->{'sysid'}\" " if( $ent->{'sysid'});
        $text .= "PUBLIC \"$ent->{'pubid'}\" " if( $ent->{'pubid'});
        $text .= "NDATA $ent->{'ndata'}"        if( $ent->{'ndata'});
        $text .= ">";
        return $text;
      }
  }

                
1;

######################################################################
package XML::Twig::Elt;
######################################################################
use Carp;

*isa = \&UNIVERSAL::isa;

use constant  PCDATA  => '#PCDATA'; 
use constant  CDATA   => '#CDATA'; 
use constant  PI      => '#PI'; 
use constant  COMMENT => '#COMMENT'; 
use constant  ENT     => '#ENT'; 

use constant  ASIS    => '#ASIS';    # pcdata elements not to be XML-escaped

use constant  ELT     => '#ELT'; 
use constant  TEXT    => '#TEXT'; 
use constant  EMPTY   => '#EMPTY'; 

use constant CDATA_START    => "<![CDATA[";
use constant CDATA_END      => "]]>";
use constant PI_START       => "<?";
use constant PI_END         => "?>";
use constant COMMENT_START  => "<!--";
use constant COMMENT_END    => "-->";

use constant XMLNS_URI      => 'http://www.w3.org/2000/xmlns/';
my $XMLNS_URI               = XMLNS_URI;


BEGIN
  { # set some aliases for methods
    *tag           = *gi; 
    *name          = *gi; 
    *set_tag       = *set_gi; 
    *set_name      = *set_gi; 
    *find_nodes    = *get_xpath; # as in XML::DOM
    *findnodes     = *get_xpath; # as in XML::LibXML
    *field         = *first_child_text;
    *trimmed_field = *first_child_trimmed_text;
    *is_field      = *contains_only_text;
    *is            = *passes;
    *matches       = *passes;
    *has_child     = *first_child;
    *has_children  = *first_child;
    *all_children_pass = *all_children_are;
    *all_children_match= *all_children_are;
    *getElementsByTagName= *descendants;
    *find_by_tag_name= *descendants_or_self;
  
    *first_child_is  = *first_child_matches;
    *last_child_is   = *last_child_matches;
    *next_sibling_is = *next_sibling_matches;
    *prev_sibling_is = *prev_sibling_matches;
    *next_elt_is     = *next_elt_matches;
    *prev_elt_is     = *prev_elt_matches;
    *parent_is       = *parent_matches;
    *child_is        = *child_matches;
    *inherited_att   = *inherit_att;

    *sort_children_by_value= *sort_children_on_value;

    *has_atts= *att_nb;

    # imports from XML::Twig
    *_is_fh= *XML::Twig::_is_fh;

    # XML::XPath compatibility
    *string_value       = *text;
    *toString           = *sprint;
    *getName            = *gi;
    *getRootNode        = *twig;  
    *getNextSibling     = *_next_sibling;
    *getPreviousSibling = *_prev_sibling;
    *isElementNode      = *is_elt;
    *isTextNode         = *is_text;
    *isPI               = *is_pi;
    *isPINode           = *is_pi;
    *isProcessingInstructionNode= *is_pi;
    *isComment          = *is_comment;
    *isCommentNode      = *is_comment;
    *getTarget          = *target;

    # try using weak references
    # test whether we can use weak references
    { local $SIG{__DIE__};
      if( eval 'require Scalar::Util' && defined( &Scalar::Util::weaken) )
        { import Scalar::Util qw(weaken); }
      elsif( eval 'require WeakRef')
        { import WeakRef; }
    }
}

 
# can be called as XML::Twig::Elt->new( [[$gi, $atts, [@content]])
# - gi is an optional gi given to the element
# - $atts is a hashref to attributes for the element
# - @content is an optional list of text and elements that will
#   be inserted under the element 
sub new 
  { my $class= shift;
    $class= ref $class || $class;
    my $elt  = {};
    bless ($elt, $class);

    return $elt unless @_;

    # if a gi is passed then use it
    my $gi= shift;
    $elt->set_gi( $gi);


    my $atts= ref $_[0] eq 'HASH' ? shift : undef;

    if( $gi eq PCDATA)
      { $elt->{pcdata}=  shift; }
    elsif( $gi eq ENT)
      { $elt->{ent}=  shift; }
    elsif( $gi eq CDATA)
      { $elt->{cdata}=  shift; }
    elsif( $gi eq COMMENT)
      { $elt->{comment}=  shift; }
    elsif( $gi eq PI)
      { $elt->set_pi( shift, shift); }
    else
      { # the rest of the arguments are the content of the element
        if( @_)
          { $elt->set_content( @_); }
        else
          { $elt->{empty}=  1;    }
      }

    if( $atts)
      { # the attribute hash can be used to pass the asis status 
        if( defined $atts->{'#ASIS'})  { $elt->set_asis(  $atts->{'#ASIS'} ); delete $atts->{'#ASIS'};  }
        if( defined $atts->{'#EMPTY'}) { $elt->{empty}=  $atts->{'#EMPTY'}; delete $atts->{'#EMPTY'}; }
        $elt->set_atts( $atts) if( keys %$atts);
        $elt->_set_id( $atts->{$ID}) if( $atts->{$ID});
      }

    return $elt;
  }

# this function creates an XM:::Twig::Elt from a string
# it is quite clumsy at the moment, as it just creates a
# new twig then returns its root
# there might also be memory leaks there
# additional arguments are passed to new XML::Twig
sub parse
  { my $class= shift;
    my $string= shift;
    my %args= @_;
    my $t= XML::Twig->new(%args);
    $t->parse( $string);
    my $elt= $t->root;
    # clean-up the node 
    delete $elt->{twig};         # get rid of the twig data
    delete $elt->{twig_current}; # better get rid of this too
 		if( $t->{twig_id_list}) { $elt->{twig_id_list}= $t->{twig_id_list}; }
    return $elt;
  }
    

sub set_gi 
  { my ($elt, $gi)= @_;
    unless( defined $XML::Twig::gi2index{$gi})
      { # new gi, create entries in %gi2index and @index2gi
        push  @XML::Twig::index2gi, $gi;
        $XML::Twig::gi2index{$gi}= $#XML::Twig::index2gi;
      }
    $elt->{gi}= $XML::Twig::gi2index{$gi};
    return $elt; 
  }

sub gi  { return $XML::Twig::index2gi[$_[0]->{gi}]; }

sub local_name 
  { my $elt= shift;
    return _local_name( $XML::Twig::index2gi[$elt->{'gi'}]);
  }

sub ns_prefix
  { my $elt= shift;
    return _ns_prefix( $XML::Twig::index2gi[$elt->{'gi'}]);
  }

# namespace prefix for any qname (can be used for elements or attributes)
sub _ns_prefix
  { my $qname= shift;
    if( $qname=~ m{^([^:]*):})
      { return $1; }
    else
      { return( ''); } # should it be '' ?
  }

# local name for any qname (can be used for elements or attributes)
sub _local_name
  { my $qname= shift;
    (my $local= $qname)=~ s{^[^:]*:}{};
    return $local;
  }

BEGIN 
  { my %DEFAULT_NS= ( xml   => "http://www.w3.org/XML/1998/namespace",
                      xmlns => "http://www.w3.org/2000/xmlns/",
                    );
 
    #sub get_namespace
    sub namespace
      { my $elt= shift;
        my $prefix= defined $_[0] ? shift() : $elt->ns_prefix;
        my $ns_att= $prefix ? "xmlns:$prefix" : "xmlns";
        my $expanded= $DEFAULT_NS{$prefix} || $elt->inherit_att( $ns_att) || '';
        return $expanded;
      }
  }


# return #ELT for an element and #PCDATA... for others
sub get_type
  { my $gi_nb= $_[0]->{gi}; # the number, not the string
    return ELT if( $gi_nb >= $XML::Twig::SPECIAL_GI);
    return $_[0]->gi;
  }

# return the gi if it's a "real" element, 0 otherwise
sub is_elt
  { return $_[0]->gi if(  $_[0]->{gi} >=  $XML::Twig::SPECIAL_GI);
    return 0;
  }


sub is_pcdata
  { my $elt= shift;
    return (exists $elt->{'pcdata'});
  }

sub is_cdata
  { my $elt= shift;
    return (exists $elt->{'cdata'});
  }

sub is_pi
  { my $elt= shift;
    return (exists $elt->{'target'});
  }

sub is_comment
  { my $elt= shift;
    return (exists $elt->{'comment'});
  }

sub is_ent
  { my $elt= shift;
    return (exists $elt->{ent} || $elt->{ent_name});
  }


sub is_text
  { my $elt= shift;
    return (exists( $elt->{'pcdata'}) || (exists $elt->{'cdata'}));
  }

sub is_empty
  { return $_[0]->{empty} || 0; }

sub set_empty
  { $_[0]->{empty}= defined( $_[1]) ? $_[1] : 1; return $_[0]; }

sub set_not_empty
  { delete $_[0]->{empty} if( ($_[0]->{'empty'} || 0)); return $_[0]; }


sub set_asis
  { my $elt=shift;

    foreach my $descendant ($elt, $elt->_descendants )
      { $descendant->{asis}= 1;
        if( (exists $descendant->{'cdata'}))
          { $descendant->set_gi( PCDATA);
            $descendant->{pcdata}=  $descendant->{cdata};
          }

      }
    return $elt;
  }

sub set_not_asis
  { my $elt=shift;
    foreach my $descendant ($elt, $elt->descendants)
      { delete $descendant->{asis} if $descendant->{asis};}
    return $elt;
  }

sub is_asis
  { return $_[0]->{asis}; }

sub closed 
  { my $elt= shift;
    my $t= $elt->twig || return;
    my $curr_elt= $t->{twig_current};
    return unless( $curr_elt);
    return $curr_elt->in( $elt);
  }

sub set_pcdata 
  { delete $_[0]->{empty};
    $_[0]->{'pcdata'}= $_[1];
    if( $_[0]->{extra_data_in_pcdata})
      { undef $_[0]->{extra_data_in_pcdata}; # TODO: be smarter!
      }
    return $_[0]; 
  }

sub append_pcdata
  { delete $_[0]->{empty};
    $_[0]->{'pcdata'}.= $_[1];
    return $_[0]; 
  }

sub pcdata        { return $_[0]->{pcdata}; }


sub append_extra_data 
  {  $_[0]->{extra_data}.= $_[1];
     return $_[0]; 
  }
  
sub set_extra_data 
  { $_[0]->{extra_data}= $_[1];
    return $_[0]; 
  }
sub extra_data { return $_[0]->{extra_data}; }

sub set_target 
  { $_[0]->{'target'}= $_[1];
    return $_[0]; 
     
  }
sub target { return $_[0]->{target}; }

sub set_data 
  { $_[0]->{'data'}= $_[1]; 
    return $_[0];
  }
sub data { return $_[0]->{data}; }

sub set_pi
  { $_[0]->{target}=  $_[1];
    $_[0]->{data}=  $_[2];
    return $_[0]; 
  }

sub pi_string { return PI_START . $_[0]->{target} . " " . $_[0]->{data} . PI_END; }

sub set_comment    { $_[0]->{comment}= $_[1]; return $_[0]; }
sub comment        { return $_[0]->{comment}; }
sub comment_string { return COMMENT_START . $_[0]->{comment} . COMMENT_END; }

sub set_ent  { $_[0]->{ent}= $_[1]; return $_[0]; }
sub ent      { return $_[0]->{ent}; }
sub ent_name { return substr( $_[0]->{ent}, 1, -1);}

sub set_cdata 
  { delete $_[0]->{empty};
    $_[0]->{cdata}= $_[1]; 
    return $_[0];
  }

sub append_cdata
  { $_[0]->{cdata}.= $_[1]; 
    return $_[0];
  }
sub cdata { return $_[0]->{cdata}; }

#start-extract twig_node
sub contains_only_text
  { my $elt= shift;
    return 0 unless $elt->is_elt;
    foreach my $child ($elt->children)
      { return 0 if $child->is_elt; }
    return $elt;
  } 
  
sub contains_only
  { my( $elt, $exp)= @_;
    my @children= $elt->children;
    foreach my $child (@children)
      { return 0 unless $child->is( $exp); }
    return @children;
  } 

sub contains_a_single
  { my( $elt, $exp)= @_;
    my $child= $elt->{first_child} or return 0;
    return 0 unless $child->matches( $exp);
    return 0 if( $child->{next_sibling});
    return $child;
  } 



sub root 
  { my $elt= shift;
    while( $elt->{parent}) { $elt= $elt->{parent}; }
    return $elt;
  }
#end-extract twig_node

sub twig 
  { my $elt= shift;
    my $root= $elt->root;
    return $root->{twig};
  }


#start-extract twig_node

# returns undef or the element, depending on whether $elt passes $cond
# $cond can be
# - empty: the element passes the condition
# - ELT ('#ELT'): the element passes the condition if it is a "real" element
# - TEXT ('#TEXT'): the element passes if it is a CDATA or PCDATA element
# - a string with an XPath condition (only a subset of XPath is actually
#   supported).
# - a regexp: the element passes if its gi matches the regexp
# - a code ref: the element passes if the code, applied on the element,
#   returns true

my %cond_cache; # expression => coderef

sub reset_cond_cache { %cond_cache=(); }

{ 
   sub _install_cond
    { my $cond= shift;
      my $sub;
      my $test;

      my $original_cond= $cond;

      my $not= ($cond=~ s{^\s*!}{}) ? '!' : '';

      if( ref $cond eq 'CODE') { return $cond; }
    
      if( ref $cond eq 'Regexp')
        { $test = qq{(\$_[0]->gi=~ /$cond/)}; }
      else
        { # the condition is a string
          if( $cond eq ELT)     
            { $test = qq{\$_[0]->is_elt}; }
          elsif( $cond eq TEXT) 
            { $test = qq{\$_[0]->is_text}; }
          elsif( $cond=~ m{^\s*($REG_NAME_W)\s*$}o)                  
            { # gi
              if( $1 ne '*')
                { # 2 options, depending on whether the gi exists in gi2index
                  # start optimization
                  my $gi= $XML::Twig::gi2index{$1};
                  if( $gi)
                    { # the gi exists, use its index as a faster shortcut
                      $test = qq{ \$_[0]->{gi} eq "$XML::Twig::gi2index{$1}"};
                    }
                  else
                  # end optimization
                    { # it does not exist (but might be created later), compare the strings
                      $test = qq{ \$_[0]->gi eq "$1"}; 
                    }
                }
              else
                { $test = qq{ (1) } }
            }
          elsif( $cond=~ m{^\s*($REG_REGEXP)\s*$}o)
            { # /regexp/
              $test = qq{ \$_[0]->gi=~ $1 }; 
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*\[\s*(\!\s*)?\@($REG_NAME)\s*\]\s*$}o)
            { # gi[@att]
              my( $gi, $not, $att)= ($1, $2, $3);
              $not||='';
              if( $gi && ($gi ne '*'))
                { $test = qq{    (\$_[0]->gi eq "$gi") 
                              && $not(defined \$_[0]->{'att'}->{"$att"})
                            };
                }
              else
                { $test = qq{ $not (defined \$_[0]->{'att'}->{"$att"})}; }
             }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*  # $1
                           \[\s*(-?)\s*(\d+)\s*\] #   [$2]
                           \s*$}xo
               )
            { my( $gi, $neg, $index)= ($1, $2, $3);
              my $siblings= $neg ? q{$_[0]->_next_siblings} : q{$_[0]->_prev_siblings};
              if( $gi && ($gi ne '*')) 
                { $test= qq{((\$_[0]->gi eq "$gi") && (scalar( grep { \$_->gi eq "$gi" } $siblings) + 1 == $index))}; }
              else
                { $test= qq{(scalar( $siblings) + 1 == $index)}; }
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*  # $1
                           \[ \s* \@($REG_NAME)   #   [@$2
                           \s*($REG_OP)\s*        #        = (or other op) $3
                          ($REG_VALUE)            #          "$4" or '$4'
                          \s*\]\s*$}xo)           #                       ]
            { # gi[@att="val"]
              my( $gi, $att, $op, $string)= ($1, $2, _op( $3), $4);
              if( $gi && ($gi ne '*'))
                { $test = qq{    (\$_[0]->gi eq "$gi") 
                              && ( (\$_[0]->{'att'}->{"$att"}||'') $op $string) 
                            }; 
                }
              else
                { $test = qq{    (defined \$_[0]->{'att'}->{"$att"}) 
                              && ( (\$_[0]->{'att'}->{"$att"}||'') $op $string) 
                            };
                }
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*  # $1
                           \[ \s* \@($REG_NAME)   #   [@$2
                           \s*($REG_OP)\s*        #        = (or other op) $3
                           ($REG_VALUE)           #          "$4" or '$4'
                           \s*(and|or)\s*         #              and or or ($5)
                           \@($REG_NAME)          #                  @$6
                           \s*($REG_OP)\s*        #                      = (or other op) $7
                           ($REG_VALUE)           #                        "$8" or '$8'
                           \s*\]\s*$}xo)          #                       ]
            { # gi[@att="val"]
              my( $gi, $att1, $op1, $string1, $connector, $att2, $op2, $string2)= ($1, $2, _op( $3), $4, $5, $6, _op( $7), $8);
              if( $gi && ($gi ne '*'))
                { $test = qq{ (   (\$_[0]->gi eq "$gi") 
                                and ( ( (\$_[0]->{'att'}->{"$att1"}||'') $op1 $string1 )
                                        $connector 
                                      ( (\$_[0]->{'att'}->{"$att2"}||'') $op2 $string2 )
                                    )
                              )
                            }; 
                }
              else
                { $test = qq{ ( ( (\$_[0]->{'att'}->{"$att1"}||'') $op1 $string1 )
                               $connector 
                                ( (\$_[0]->{'att'}->{"$att2"}||'') $op2 $string2 )
                              )
                            };
                }
            }
          elsif( $cond=~ m{^\s*\.([\w-]+)\s*$}o)
            { # .class
              my $class= $1;
              $test = qq{(\$_[0]->in_class( "$class")) }; 
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*  # $1
                           \[ \s* \@($REG_NAME)   #   [@$2
                           \s*($REG_MATCH)\s*     #        =~ or !~ ($3)
                           ($REG_REGEXP)          #           /$4/
                           \s*\]\s*$}xo)          #                ]
            { # gi[@att=~ /regexp/] or gi[@att!~/regexp/]
              my( $gi, $att, $match, $regexp)= ($1, $2, $3, $4);
              if( $gi && ($gi ne '*'))
                { $test = qq{    (\$_[0]->gi eq "$gi") 
                              && ( (\$_[0]->{'att'}->{"$att"}||'') $match $regexp)
                            }; 
                }
              else
                { # *[@att=~/regexp/ or *[@att!~/regexp/
                  $test = qq{( ( \$_[0]->{'att'}->{"$att"}||'') $match $regexp) };
                }
            }
          elsif( $cond=~ m{^\s*\@($REG_NAME)\s*$}o)
            { # @att (or !@att)
              my( $att)= ($1);
              $test = qq{ (defined \$_[0]->{'att'}->{"$att"})}; 
            }
          elsif( $cond=~ m{^\s*                   
                           \@($REG_NAME)        #   @$1
                           \s*($REG_OP)\s*      #       = (or other op) $2
                           ($REG_VALUE)         #         "$3" or '$3'
                           \s*$}xo)                                 
            { # @att="val"
              my( $att, $op, $string)= ( $1, _op( $2), $3);
              $test = qq{( (\$_[0]->{'att'}->{"$att"}||'') $op $string) };
             }
          elsif( $cond=~ m{^\s*                   
                           \@($REG_NAME)        #   @$1
                           \s*($REG_OP)\s*      #       = (or other op) $2
                           ($REG_VALUE)         #         "$3" or '$3'
                           \s*(and|or)\s*       #              and or or ($4)
                           \@($REG_NAME)        #   @$5
                           \s*($REG_OP)\s*      #       = (or other op) $6
                           ($REG_VALUE)         #         "$7" or '$7'
                           \s*$}xo)                                 
            { # @att="val"
              my( $att1, $op1, $string1, $connector, $att2, $op2, $string2 )= ( $1, _op( $2), $3, $4, $5, _op( $6), $7);
              $test = qq{ ( ( (\$_[0]->{'att'}->{"$att1"}||'') $op1 $string1) 
                            $connector
                            ( (\$_[0]->{'att'}->{"$att2"}||'') $op2 $string2) 
                          )
                        };
             }
          elsif( $cond=~ m{^\s*
                           \@($REG_NAME)        #   [@$1
                           \s*(=~|!~)\s*        #        =~ or !~ ($2)
                          ($REG_REGEXP)         #           /$3/
                          \s*\s*$}xo)           #                ]
            { # @att=~ /regexp/ or @att!~/regexp/
              my( $att, $match, $regexp)= ( $1, $2, $3);
              $test = qq{( (\$_[0]->{'att'}->{"$att"}||'') $match $regexp) };
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*            # $1
                           \[\s*(?:text|string)(?:\(\s*\))? #   [string()
                           \s*($REG_OP)\s*                  #            = or other op ($2)
                           ($REG_VALUE)                     #              "$3" or '$3'
                           \s*\]\s*$}xo)                    #                          ]
            { # gi[string()= "val"]
              my ($gi, $op, $text)= ($1, _op( $2), $3);
              if( $gi && ($gi ne '*'))
                { $test = qq{(\$_[0]->gi eq "$gi") && ( \$_[0]->text eq $text)}; }
              else
                { $test = qq{ \$_[0]->text eq $text }; }
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*            # $1
                           \[\s*(?:text|string)(?:\(\s*\))? #   [string()
                           \s*($REG_MATCH)\s*               #             =~ or !~ ($2)
                           ($REG_REGEXP)                    #               /$3/
                           \s*\]\s*$}xo)                    #                   ]
            { # gi[string()=~ /regexp/]
              my( $gi, $match, $regexp)= ($1, $2, $3);
              if( $gi && ($gi ne '*'))
                { $test = qq{(\$_[0]->gi eq "$gi") && ( \$_[0]->text $match $regexp) }; }
              else
                { $test = qq{ \$_[0]->text $match $regexp }; }
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*            # $1
                           \[\s*(?:text|string)\s*\(\s*     #   [string(
                           ($REG_NAME)\s*\)                 #            $2)
                           \s*($REG_OP)\s*                  #                = or other op $3
                           ($REG_VALUE)                     #                  "$4" or '$4'
                           \s*\]\s*$}xo)                    #                      ]
            { # gi[string(gi2)= "text"]
              my ($gi, $gi2, $op, $text)= ($1, $2, _op($3), $4);
              $text=~ s/([{}])/\\$1/g;
              if( $gi && ($gi ne '*'))
                { $test = qq{    (\$_[0]->gi eq "$gi") 
                              && ( \$_[0]->first_child( qq{$gi2\[text() $op $text]}))
                            };
                }
              else
                { $test = qq{ \$_[0]->first_child(qq{$gi2\[text() $op $text]}) } ; }
            }
          elsif( $cond=~ m{^\s*($REG_NAME_W)?\s*     # $1
                           \[\s*(?:text|string)\(\s* #   [string(
                           ($REG_NAME)\s*\)          #         $2)
                           \s*(=~|!~)\s*             #            =~ or !~ ($3)
                           ($REG_REGEXP)             #              /$4/
                           \s*\]\s*$}xo)             #                  ]
            { # gi[string(gi2)=~ /regexp/]
              my( $gi, $gi2, $match, $regexp)= ($1, $2, $3, $4);
              if( $gi && ($gi ne '*'))
                { $test = qq{   (\$_[0]->gi eq "$gi") 
                             && ( \$_[0]->field( "$gi2") $match $regexp)
                            };
                }
              else
                { $test = qq{\$_[0]->field( "$gi2") $match $regexp}; }
            }
          else
            { croak "wrong condition '$original_cond'"; }
        }

      my $s= eval "sub { return \$_[0] if( $not($test)) }";
      if( $@) 
        { croak "wrong navigation condition '$original_cond' ($@);" }
      return $s;
    }

  sub _op
    { my $op= shift;
      if(    $op eq '=')  { $op= 'eq'; }
      elsif( $op eq '!=') { $op= 'ne'; }
      return $op;
    }
 
  sub passes
    { my( $elt, $cond)= @_;
      return $elt unless $cond;
      my $sub= ($cond_cache{$cond} ||= _install_cond( $cond));
      return $sub->( $elt);
    }
}
# end-extract twig_nodes

sub set_parent 
  { $_[0]->{parent}= $_[1];
    weaken( $_[0]->{parent}) if( $XML::Twig::weakrefs);
  }

#start-extract twig_node
sub parent
  { my $elt= shift;
    my $cond= shift || return $elt->{parent};
    do { $elt= $elt->{parent} || return; } until (!$elt || $elt->passes( $cond));
    return $elt;
  }
#end-extract twig_node

sub set_first_child 
  { delete $_[0]->{empty};
    $_[0]->{'first_child'}= $_[1]; 
  }

#start-extract twig_node
sub first_child
  { my $elt= shift;
    my $cond= shift || return $elt->{first_child};
    my $child= $elt->{first_child};
    my $test_cond= ($cond_cache{$cond} ||= _install_cond( $cond));
    while( $child && !$test_cond->( $child)) 
       { $child= $child->{next_sibling}; }
    return $child;
  }
#end-extract twig_node
  
sub _first_child   { return $_[0]->{first_child};  }
sub _last_child    { return $_[0]->{last_child};   }
sub _next_sibling  { return $_[0]->{next_sibling}; }
sub _prev_sibling  { return $_[0]->{prev_sibling}; }
sub _parent        { return $_[0]->{parent};       }
sub _next_siblings { my $elt= shift; my @siblings; while( $elt= $elt->{next_sibling}) { push @siblings, $elt; } return @siblings; }
sub _prev_siblings { my $elt= shift; my @siblings; while( $elt= $elt->{prev_sibling}) { push @siblings, $elt; } return @siblings; }

# sets a field
# arguments $record, $cond, @content
sub set_field
  { my $record = shift;
    my $cond = shift;
    my $child= $record->first_child( $cond);
    if( $child)
      { $child->set_content( @_); }
    else
      { if( $cond=~ m{^\s*($REG_NAME)})
          { my $gi= $1;
            $child= $record->insert_new_elt( last_child => $gi, @_); 
          }
        else
          { croak "can't create a field name from $cond"; }
      } 
    return $child;
  }

sub set_last_child 
  { delete $_[0]->{empty};
    $_[0]->{'last_child'}= $_[1];
    weaken( $_[0]->{'last_child'}) if( $XML::Twig::weakrefs);
  }

#start-extract twig_node
sub last_child
  { my $elt= shift;
    my $cond= shift || return $elt->{last_child};
    my $test_cond= ($cond_cache{$cond} ||= _install_cond( $cond));
    my $child= $elt->{last_child};
    while( $child && !$test_cond->( $child) )
      { $child= $child->{prev_sibling}; }
    return $child
  }
#end-extract twig_node


sub set_prev_sibling 
  { $_[0]->{'prev_sibling'}= $_[1]; 
    weaken( $_[0]->{'prev_sibling'}) if( $XML::Twig::weakrefs); 
  }

#start-extract twig_node
sub prev_sibling
  { my $elt= shift;
    my $cond= shift || return $elt->{prev_sibling};
    my $test_cond= ($cond_cache{$cond} ||= _install_cond( $cond));
    my $sibling= $elt->{prev_sibling};
    while( $sibling && !$test_cond->( $sibling) )
          { $sibling= $sibling->{prev_sibling}; }
    return $sibling;
  }
#end-extract twig_node

sub set_next_sibling { $_[0]->{'next_sibling'}= $_[1]; }

#start-extract twig_node
sub next_sibling
  { my $elt= shift;
    my $cond= shift || return $elt->{next_sibling};
    my $test_cond= ($cond_cache{$cond} ||= _install_cond( $cond));
    my $sibling= $elt->{next_sibling};
    while( $sibling && !$test_cond->( $sibling) )
          { $sibling= $sibling->{next_sibling}; }
    return $sibling;
  }

# methods dealing with the class attribute, convenient if you work with xhtml
sub class     { my( $elt)= @_; return $elt->{'att'}->{'class'}; }
sub set_class { my( $elt, $class)= @_; $elt->set_att( class => $class); }

# adds a class to an element
sub add_to_class
  { my( $elt, $new_class)= @_;
    return $elt unless $new_class;
    my $class= $elt->class;
    my %class= $class ? map { $_ => 1 } split /\s+/, $class : ();
    $class{$new_class}= 1;
    $elt->set_class( join( ' ', sort keys %class));
  }

sub att_to_class      { my( $elt, $att)= @_; $elt->set_class( $elt->{'att'}->{$att}); }
sub add_att_to_class  { my( $elt, $att)= @_; $elt->add_to_class( $elt->{'att'}->{$att}); }
sub move_att_to_class { my( $elt, $att)= @_; $elt->add_to_class( $elt->{'att'}->{$att});
                        $elt->del_att( $att); 
                      }
sub tag_to_class      { my( $elt)= @_; $elt->set_class( $elt->tag);    }
sub add_tag_to_class  { my( $elt)= @_; $elt->add_to_class( $elt->tag); }
sub set_tag_class     { my( $elt, $new_tag)= @_; $elt->add_tag_to_class; $elt->set_tag( $new_tag); }

sub in_class          
  { my( $elt, $class)= @_;
    my $elt_class= $elt->class;
    return unless( defined $elt_class);
    return $elt->class=~ m{(?:^|\s)\Q$class\E(?:\s|$)} ? $elt : 0;
  }

#end-extract twig_node

# get or set all attributes
# argument can be a hash or a hasref
sub set_atts 
  { my $elt= shift;
    my %atts;
    tie %atts, 'Tie::IxHash' if( keep_atts_order());
    %atts= ( isa( $_[0] || '', 'HASH')) ? %{$_[0]} : @_;
    $elt->{att}= \%atts;
 		if( exists $atts{$ID}) { $elt->_set_id( $atts{$ID}); }
    return $elt;
  }

sub atts      { return $_[0]->{att};           }
sub att_names { return sort keys %{$_[0]->{att}};   }
sub del_atts  { $_[0]->{att}={}; return $_[0]; }

# get or set a single attribute (set works for several atts)
sub set_att 
  { my $elt= shift;
    
    unless( $elt->{att})
      { $elt->{att}={};
        tie %{$elt->{att}}, 'Tie::IxHash' if( keep_atts_order());
      }

    while(@_) 
		  { my( $att, $val)= (shift, shift);
        $elt->{att}->{$att}= $val;
		    if( $att eq $ID) { $elt->_set_id( $val); } 
      }
    return $elt;
  }
 
sub att { return $_[0]->{att}->{$_[1]}; }
sub del_att 
  { my $elt= shift;
    while( @_) { delete $elt->{'att'}->{shift()}; }
    return $elt;
  }

# delete an attribute from all descendants of an element
sub strip_att
  { my( $elt, $att)= @_;
    $_->del_att( $att) foreach ($elt->descendants_or_self( qq{*[\@$att]}));
  }

sub change_att_name
  { my( $elt, $old_name, $new_name)= @_;
    my $value= $elt->{'att'}->{$old_name};
    return $elt unless( defined $value);
    $elt->del_att( $old_name)
        ->set_att( $new_name => $value);
    return $elt;
  }

sub set_twig_current { $_[0]->{twig_current}=1; }
sub del_twig_current { delete $_[0]->{twig_current}; }


# get or set the id attribute
sub set_id 
  { my( $elt, $id)= @_;
    $elt->del_id() if( exists $elt->{att}->{$ID});
    $elt->set_att($ID, $id); 
    $elt->_set_id( $id);
    return $elt;
  }

# only set id, does not update the attribute value
sub _set_id
  { my( $elt, $id)= @_;
    my $t= $elt->twig || $elt;
    $t->{twig_id_list}->{$id}= $elt;
    weaken(  $t->{twig_id_list}->{$id}) if( $XML::Twig::weakrefs);
    return $elt;
  }

sub id { return $_[0]->{att}->{$ID}; }

# methods used to add ids to elements that don't have one
BEGIN 
{ my $id_nb   = "0001";
  my $id_seed = "twig_id_";

  sub set_id_seed
    { $id_seed= $_[1]; $id_nb=1; }

  sub add_id
    { my $elt= shift;
      $elt->set_id( $id_seed . $id_nb++) unless( $elt->{'att'}->{$ID});
    }

}



# delete the id attribute and remove the element from the id list
sub del_id 
  { my $elt= shift;
    unless( exists $elt->{'att'}) { return $elt }; 
    unless( exists $elt->{'att'}->{$ID}) { return $elt }; 
    my $id= $elt->{'att'}->{$ID};

    delete $elt->{'att'}->{$ID}; 

    my $t= shift || $elt->twig;
    unless( $t) { return $elt; }
    if( exists $t->{twig_id_list}->{$id}) { delete $t->{twig_id_list}->{$id}; }

    return $elt;
  }

# return the list of children
#start-extract twig_node
sub children
  { my $elt= shift;
    my @children;
    my $child= $elt->first_child( @_);
    while( $child) 
      { push @children, $child;
        $child= $child->next_sibling( @_);
      } 
    return @children;
  }

sub _children
  { my $elt= shift;
    my @children=();
    my $child= $elt->_first_child();
    while( $child) 
      { push @children, $child;
        $child= $child->{next_sibling};
      } 
    return @children;
  }

sub children_copy
  { my $elt= shift;
    my @children;
    my $child= $elt->first_child( @_);
    while( $child) 
      { push @children, $child->copy;
        $child= $child->next_sibling( @_);
      } 
    return @children;
  }


sub children_count
  { my $elt= shift;
    my $cond= shift;
    my $count=0;
    my $child= $elt->{first_child};
    while( $child)
      { $count++ if( $child->passes( $cond)); 
        $child= $child->{next_sibling};
      }
    return $count;
  }

sub children_text
  { my $elt= shift;
    return wantarray() ? map { $_->text} $elt->children( @_)
                       : join( '', map { $_->text} $elt->children( @_) )
                       ;
  }

sub children_trimmed_text
  { my $elt= shift;
    return wantarray() ? map { $_->trimmed_text} $elt->children( @_)
                       : join( '', map { $_->trimmed_text} $elt->children( @_) )
                       ;
  }

sub all_children_are
  { my( $parent, $cond)= @_;
    foreach my $child ($parent->children)
      { return 0 unless( $child->passes( $cond)); }
    return 1;
  }


sub ancestors
  { my( $elt, $cond)= @_;
    my @ancestors;
    while( $elt->{parent})
      { $elt= $elt->{parent};
        push @ancestors, $elt
          if( $elt->passes( $cond));
      }
    return @ancestors;
  }

sub ancestors_or_self
  { my( $elt, $cond)= @_;
    my @ancestors;
    while( $elt)
      { push @ancestors, $elt
          if( $elt->passes( $cond));
        $elt= $elt->{parent};
      }
    return @ancestors;
  }


sub _ancestors
  { my( $elt, $include_self)= @_;
    my @ancestors= $include_self ? ($elt) : ();
    while( $elt= $elt->{parent})
      { push @ancestors, $elt;
      }
    return @ancestors;
  }


sub inherit_att
  { my $elt= shift;
    my $att= shift;
    my %tags= map { ($_, 1) } @_;

    do 
      { if(   (defined $elt->{'att'}->{$att})
           && ( !%tags || $tags{$XML::Twig::index2gi[$elt->{'gi'}]})
          )
          { return $elt->{'att'}->{$att}; }
      } while( $elt= $elt->{parent});
    return undef;
  }


sub current_ns_prefixes
  { my $elt= shift;
    my %prefix;
    $prefix{''}=1 if( $elt->namespace( ''));
    while( $elt)
      { my @ns= grep { !m{^xml} } map { m{^([^:]+):} } ($XML::Twig::index2gi[$elt->{'gi'}], $elt->att_names);
        $prefix{$_}=1 foreach (@ns);
        $elt= $elt->{parent};
      }

    return sort keys %prefix;
  }

# kinda counter-intuitive actually:
# the next element is found by looking for the next open tag after from the
# current one, which is the first child, if it exists, or the next sibling
# or the first next sibling of an ancestor
# optional arguments are: 
#   - $subtree_root: a reference to an element, when the next element is not 
#                    within $subtree_root anymore then next_elt returns undef
#   - $cond: a condition, next_elt returns the next element matching the condition
                 
sub next_elt
  { my $elt= shift;
    my $subtree_root= 0;
    $subtree_root= shift if( defined $_[0] and (isa( $_[0], 'XML::Twig::Elt')));
    my $cond= shift;
    my $next_elt;

    my $ind;                                                              # optimization
    my $test_cond;
    if( $cond)                                                            # optimization
      { unless( defined( $ind= $XML::Twig::gi2index{$cond}) )             # optimization
          { $test_cond= ($cond_cache{$cond} ||= _install_cond( $cond)); } # optimization
      }                                                                   # optimization
    
    do
      { if( $next_elt= $elt->{first_child})
          { # simplest case: the elt has a child
          }
         elsif( $next_elt= $elt->{next_sibling}) 
          { # no child but a next sibling (just check we stay within the subtree)
          
            # case where elt is subtree_root, is empty and has a sibling
            return undef if( $subtree_root && ($elt == $subtree_root));
            
          }
        else
          { # case where the element has no child and no next sibling:
            # get the first next sibling of an ancestor, checking subtree_root 
          
            # case where elt is subtree_root, is empty and has no sibling
            return undef if( $subtree_root && ($elt == $subtree_root));
             
            $next_elt= $elt->{parent};

            until( $next_elt->{next_sibling})
              { return undef if( $subtree_root && ($subtree_root == $next_elt));
                $next_elt= $next_elt->{parent} || return undef;
              }
            return undef if( $subtree_root && ($subtree_root == $next_elt)); 
            $next_elt= $next_elt->{next_sibling};   
          }  
      $elt= $next_elt;                   # just in case we need to loop
    } until(    ! defined $elt 
             || ! defined $cond 
         || (defined $ind       && ($elt->{gi} eq $ind))   # optimization
         || (defined $test_cond && ($test_cond->( $elt)))
               );
    
      return $elt;
      }

# return the next_elt within the element
# just call next_elt with the element as first and second argument
sub first_descendant { return $_[0]->next_elt( @_); }

# get the last descendant, # then return the element found or call prev_elt with the condition
sub last_descendant
  { my( $elt, $cond)= @_;
    my $last_descendant= $elt->_last_descendant;
    if( !$cond || $last_descendant->matches( $cond))
      { return $last_descendant; }
    else
      { return $last_descendant->prev_elt( $elt, $cond); }
  }

# no argument allowed here, just go down the last_child recursively
sub _last_descendant
  { my $elt= shift;
    while( my $child= $elt->{last_child}) { $elt= $child; }
    return $elt;
  }

# counter-intuitive too:
# the previous element is found by looking
# for the first open tag backwards from the current one
# it's the last descendant of the previous sibling 
# if it exists, otherwise it's simply the parent
sub prev_elt
  { my $elt= shift;
    my $subtree_root= 0;
    if( defined $_[0] and (isa( $_[0], 'XML::Twig::Elt')))
      { $subtree_root= shift ;
        return undef if( $elt == $subtree_root);
      }
    my $cond= shift;
    # get prev elt
    my $prev_elt;
    do
      { return undef if( $elt == $subtree_root);
        if( $prev_elt= $elt->{prev_sibling})
          { while( $prev_elt->{last_child})
              { $prev_elt= $prev_elt->{last_child}; }
          }
        else
          { $prev_elt= $elt->{parent} || return undef; }
        $elt= $prev_elt;     # in case we need to loop 
      } until( $elt->passes( $cond));

    return $elt;
  }


sub next_n_elt
  { my $elt= shift;
    my $offset= shift || return undef;
    foreach (1..$offset)
      { $elt= $elt->next_elt( @_) || return undef; }
    return $elt;
  }

# checks whether $elt is included in $ancestor, returns 1 in that case
sub in
  { my ($elt, $ancestor)= @_;
    if( isa( $ancestor, 'XML::Twig::Elt'))
      { # element
        while( $elt= $elt->{parent}) { return $elt if( $elt ==  $ancestor); } 
      }
    else
      { # condition
        while( $elt= $elt->{parent}) { return $elt if( $elt->matches( $ancestor)); } 
      }
    return 0;           
  }

sub first_child_text  
  { my $elt= shift;
    my $dest=$elt->first_child(@_) or return '';
    return $dest->text;
  }
  
sub first_child_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->first_child(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub first_child_matches
  { my $elt= shift;
    my $dest= $elt->{first_child} or return undef;
    return $dest->passes( @_);
  }
  
sub last_child_text  
  { my $elt= shift;
    my $dest=$elt->last_child(@_) or return '';
    return $dest->text;
  }
  
sub last_child_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->last_child(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub last_child_matches
  { my $elt= shift;
    my $dest= $elt->{last_child} or return undef;
    return $dest->passes( @_);
  }
  
sub child_text
  { my $elt= shift;
    my $dest=$elt->child(@_) or return '';
    return $dest->text;
  }
  
sub child_trimmed_text
  { my $elt= shift;
    my $dest=$elt->child(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub child_matches
  { my $elt= shift;
    my $nb= shift;
    my $dest= $elt->child( $nb) or return undef;
    return $dest->passes( @_);
  }

sub prev_sibling_text  
  { my $elt= shift;
    my $dest=$elt->prev_sibling(@_) or return '';
    return $dest->text;
  }
  
sub prev_sibling_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->prev_sibling(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub prev_sibling_matches
  { my $elt= shift;
    my $dest= $elt->{prev_sibling} or return undef;
    return $dest->passes( @_);
  }
  
sub next_sibling_text  
  { my $elt= shift;
    my $dest=$elt->next_sibling(@_) or return '';
    return $dest->text;
  }
  
sub next_sibling_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->next_sibling(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub next_sibling_matches
  { my $elt= shift;
    my $dest= $elt->{next_sibling} or return undef;
    return $dest->passes( @_);
  }
  
sub prev_elt_text  
  { my $elt= shift;
    my $dest=$elt->prev_elt(@_) or return '';
    return $dest->text;
  }
  
sub prev_elt_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->prev_elt(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub prev_elt_matches
  { my $elt= shift;
    my $dest= $elt->prev_elt or return undef;
    return $dest->passes( @_);
  }
  
sub next_elt_text  
  { my $elt= shift;
    my $dest=$elt->next_elt(@_) or return '';
    return $dest->text;
  }
  
sub next_elt_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->next_elt(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub next_elt_matches
  { my $elt= shift;
    my $dest= $elt->next_elt or return undef;
    return $dest->passes( @_);
  }
  
sub parent_text  
  { my $elt= shift;
    my $dest=$elt->parent(@_) or return '';
    return $dest->text;
  }
  
sub parent_trimmed_text  
  { my $elt= shift;
    my $dest=$elt->parent(@_) or return '';
    return $dest->trimmed_text;
  }
  
sub parent_matches
  { my $elt= shift;
    my $dest= $elt->{parent} or return undef;
    return $dest->passes( @_);
  }
 
sub is_first_child
  { my $elt= shift;
    my $parent= $elt->{parent} or return 0;
    my $first_child= $parent->first_child( @_) or return 0;
    return ($first_child == $elt) ? $elt : 0;
  }
 
sub is_last_child
  { my $elt= shift;
    my $parent= $elt->{parent} or return 0;
    my $last_child= $parent->last_child( @_) or return 0;
    return ($last_child == $elt) ? $elt : 0;
  }

# returns the depth level of the element
# if 2 parameter are used then counts the 2cd element name in the
# ancestors list
sub level
  { my( $elt, $cond)= @_;
   
    my $level=0;
    my $name=shift || '';
    while( $elt= $elt->{parent}) { $level++ if( !$cond || $elt->matches( $cond)); }
    return $level;           
  }

# checks whether $elt has an ancestor that satisfies $cond, returns the ancestor
sub in_context
  { my ($elt, $cond, $level)= @_;
    $level= -1 unless( $level) ;  # $level-- will never hit 0

    while( $level)
      { $elt= $elt->{parent} or return 0;
        if( $elt->matches( $cond)) { return $elt; }
        $level--;
      }
    return 0;
  }


sub _descendants
  { my( $subtree_root, $include_self)= @_;
    my @descendants= $include_self ? ($subtree_root) : ();

    my $elt= $subtree_root; 
    my $next_elt;   
 
    MAIN: while( 1)  
      { if( $next_elt= $elt->{first_child})
          { # simplest case: the elt has a child
          }
        elsif( $next_elt= $elt->{next_sibling}) 
          { # no child but a next sibling (just check we stay within the subtree)
          
            # case where elt is subtree_root, is empty and has a sibling
            last MAIN if( $elt == $subtree_root);
          }
        else
          { # case where the element has no child and no next sibling:
            # get the first next sibling of an ancestor, checking subtree_root 
                
            # case where elt is subtree_root, is empty and has no sibling
            last MAIN if( $elt == $subtree_root);
               
            # backtrack until we find a parent with a next sibling
            $next_elt= $elt->{parent} || last;
            until( $next_elt->{next_sibling})
              { last MAIN if( $subtree_root == $next_elt);
                $next_elt= $next_elt->{parent} || last MAIN;
              }
            last MAIN if( $subtree_root == $next_elt); 
            $next_elt= $next_elt->{next_sibling};   
          }  
        $elt= $next_elt || last MAIN;
        push @descendants, $elt;
      }
    return @descendants;
  }


sub descendants
  { my( $subtree_root, $cond)= @_;
    my @descendants=(); 
    my $elt= $subtree_root;
    
    # this branch is pure optimisation for speed: if $cond is a gi replace it
    # by the index of the gi and loop here 
    # start optimization
    my $ind;
    if( !$cond || ( defined ( $ind= $XML::Twig::gi2index{$cond})) )
      {
        my $next_elt;

        while( 1)  
          { if( $next_elt= $elt->{first_child})
                { # simplest case: the elt has a child
                }
             elsif( $next_elt= $elt->{next_sibling}) 
              { # no child but a next sibling (just check we stay within the subtree)
           
                # case where elt is subtree_root, is empty and has a sibling
                last if( $subtree_root && ($elt == $subtree_root));
              }
            else
              { # case where the element has no child and no next sibling:
                # get the first next sibling of an ancestor, checking subtree_root 
                
                # case where elt is subtree_root, is empty and has no sibling
                last if( $subtree_root && ($elt == $subtree_root));
               
                # backtrack until we find a parent with a next sibling
                $next_elt= $elt->{parent} || last undef;
                until( $next_elt->{next_sibling})
                  { last if( $subtree_root && ($subtree_root == $next_elt));
                    $next_elt= $next_elt->{parent} || last;
                  }
                last if( $subtree_root && ($subtree_root == $next_elt)); 
                $next_elt= $next_elt->{next_sibling};   
              }  
            $elt= $next_elt || last;
            push @descendants, $elt if( !$cond || ($elt->{gi} eq $ind));
          }
      }
    else
    # end optimization
      { # branch for a complex condition: use the regular (slow but simple) way
        while( $elt= $elt->next_elt( $subtree_root, $cond))
          { push @descendants, $elt; }
      }
    return @descendants;
  }

 
sub descendants_or_self
  { my( $elt, $cond)= @_;
    my @descendants= $elt->passes( $cond) ? ($elt) : (); 
    push @descendants, $elt->descendants( $cond);
    return @descendants;
  }
  
sub sibling
  { my $elt= shift;
    my $nb= shift;
    if( $nb > 0)
      { foreach( 1..$nb)
          { $elt= $elt->next_sibling( @_) or return undef; }
      }
    elsif( $nb < 0)
      { foreach( 1..(-$nb))
          { $elt= $elt->prev_sibling( @_) or return undef; }
      }
    else # $nb == 0
      { return $elt->passes( $_[0]); }
    return $elt;
  }

sub sibling_text
  { my $elt= sibling( @_);
    return $elt ? $elt->text : undef;
  }


sub child
  { my $elt= shift;
    my $nb= shift;
    if( $nb >= 0)
      { $elt= $elt->first_child( @_) or return undef;
        foreach( 1..$nb)
          { $elt= $elt->next_sibling( @_) or return undef; }
      }
    else
      { $elt= $elt->last_child( @_) or return undef;
        foreach( 2..(-$nb))
          { $elt= $elt->prev_sibling( @_) or return undef; }
      }
    return $elt;
  }

sub prev_siblings
  { my $elt= shift;
    my @siblings=();
    while( $elt= $elt->prev_sibling( @_))
      { unshift @siblings, $elt; }
    return @siblings;
  }

sub pos
  { my $elt= shift;
    return 0 if ($_[0] && !$elt->matches( @_));
    my $pos=1;
    $pos++ while( $elt= $elt->prev_sibling( @_));
    return $pos;
  }


sub next_siblings
  { my $elt= shift;
    my @siblings=();
    while( $elt= $elt->next_sibling( @_))
      { push @siblings, $elt; }
    return @siblings;
  }

# used by get_xpath: parses the xpath expression and generates a sub that performs the
# search
sub _install_xpath
  { my( $xpath_exp, $type)= @_;
    my $original_exp= $xpath_exp;
    my $sub= 'my $elt= shift; my @results;';
    
    # grab the root if expression starts with a /
    if( $xpath_exp=~ s{^/}{})
      { $sub .= '@results= ($elt->twig);'; }
    elsif( $xpath_exp=~ s{^\./}{})
      { $sub .= '@results= ($elt);'; }
    else
      { $sub .= '@results= ($elt);'; }


    while( $xpath_exp &&
           $xpath_exp=~s{^\s*(/?)                            
                          # the xxx=~/regexp/ is a pain as it includes /  
                          (\s*((\*|$REG_NAME|\.)\s*)?\[\s*(string\(\s*\)|\@$REG_NAME)\s*$REG_MATCH  
                            \s*$REG_REGEXP\s*\]\s* 
                          # or a regular condition, with no / excepts \/
                          |([^\\/]|\\.)*
                          )
                          (/|$)}{}xo)

      { my $wildcard= $1;
        my $sub_exp= $2; 
        
        # grab a parent
        if( $sub_exp eq '..')
          { croak "error in xpath expression $original_exp" if( $wildcard);
            $sub .= '@results= map { $_->{parent}} @results;';
          }
    # test the element itself
    elsif( $sub_exp=~ m{^\.(.*)$}s)
      { $sub .= "\@results= grep { \$_->matches( q{$1}) } \@results;" }
        # grab children
        elsif( $sub_exp=~ m{($REG_NAME_W)?\s*                # * or a gi    ($1)
                            (?:
                \[\s*                             #  [
                  (
                (?:string\(\s*\)|\@$REG_NAME) #    regexp condition 
                   \s*$REG_MATCH\s*$REG_REGEXP\s*  # or
                        |[^\]]*                       #    regular condition 
                  )
                            \]                                #   ]
                             )?\s*$}xs)        
          { my $gi= $1; 
            if( !$1 or $1 eq '*') { $gi=''; }
            my $cond= $2; 
            if( $cond) { $cond=~ s{^\s*}{}; $cond=~ s{\s*$}{}; }
            my $function;

            # "special" conditions, that return just one element
            if( $cond && ($cond =~ m{^((-\s*)?\d+)$}) )
              { my $offset= $1;
                $offset-- if( $offset > 0);
                $function=  $wildcard ? "next_n_elt( $offset, '$gi')" 
                                      : "child( $offset, '$gi')";
                $sub .= "\@results= map { \$_->$function } \@results;"
              }
            elsif( $cond && ($cond =~ m{^last\s*\(\s*\)$}) )
              { croak "error in xpath expression $original_exp, cant use // and last()"
                  if( $wildcard);
                 $sub .= "\@results= map { \$_->last_child( '$gi') } \@results;";
              }
            else
              { # go down and get the children or descendants
                unless ( defined $gi)
                  { if( $wildcard)
                      { $sub .= '@results= map { $_->descendants  } @results;' }
                    else
                      { $sub .= '@results= map { $_->children } @results;'; }
                  }
                else
                  { if( $wildcard)
                      { $sub .= "\@results= map { \$_->descendants( '$gi')  } \@results;";  }            
                    else
                      { $sub .= "\@results= map { \$_->children( '$gi')  } \@results;"; }
                  } 
                # now filter using the condition
                if( $cond)
                  { my $op='';
                    my $test="";
                    do
                      { if( $op)
                          { $cond=~ s{^\s*$op\s*}{};
                            $op= lc( $op);
                            $test .= $op;
                          }
                       if( $cond =~ s{^string\(\s*\)\s*=\s*($REG_STRING)\s*}{}o)  # string()="string" cond
                          { $test .= "\$_->text eq $1"; 
                          }
                       elsif( $cond =~ s{^string\(\s*\)\s*($REG_MATCH)\s*($REG_REGEXP)\s*}{}o)  # string()=~/regex/ cond
                          { my( $match, $regexp)= ($1, $2);
                            $test .= "\$_->text $match $regexp"; 
                          }
                       elsif( $cond=~ s{^@($REG_NAME)\s*($REG_OP)\s*($REG_STRING|$REG_NUMBER)}{}o)  # @att="val" cond
                          { my( $att, $oper, $val)= ($1, _op( $2), $3);
                            $test .= qq{((defined \$_->{'att'}->{"$att"})  && (\$_->{'att'}->{"$att"} $oper $val))};
                          }
                       elsif( $cond =~ s{^@($REG_NAME)\s*($REG_MATCH)\s*($REG_REGEXP)\s*}{}o)  # @att=~/regex/ cond XXX
                          { my( $att, $match, $regexp)= ($1, $2, $3);
                            $test .= qq{((defined \$_->{'att'}->{"$att"})  && (\$_->{'att'}->{"$att"} $match $regexp))};; 
                          }
                       elsif( $cond=~ s{^@($REG_NAME)\s*}{}o)                      # @att cond
                          { 
                            $test .= qq{(defined \$_->{'att'}->{"$1"})};
                          }
                       elsif( $cond=~ s{^\s*(\d+)\s*}{}o)                          # positive number condition
                          { 
                            $test .= qq{(\$_->prev_siblings( \$_->gi) == $1)};
                          }
                       elsif( $cond=~ s{^\s*-\s*(\d+)\s*}{}o)                      # negative number condition
                          { 
                            $test .= qq{(\$_->next_siblings( \$_->gi) == $1)};
                          }
                       } while( ($op)=($cond=~ m{^\s*(and|or)\s*}i));
                     croak "error in xpath expression $original_exp at $cond" if( $cond);
                     $sub .= "\@results= grep { $test } \@results;";
                   }
              }
          }
      }

    if( $xpath_exp)
      { croak "error in xpath expression $original_exp around $xpath_exp"; }
      
    $sub .= "return \@results; ";
    my $s= eval "sub { $sub }";
    if( $@) { croak "error in xpath expression $original_exp ($@);" }
    return( $s); 
   }
        
{ # extremely elaborate caching mechanism
  my %xpath; # xpath_expression => subroutine_code;  
  sub get_xpath
    { my( $elt, $xpath_exp, $offset)= @_;
      my $sub= ($xpath{$xpath_exp} ||= _install_xpath( $xpath_exp));
      return $sub->( $elt) unless( defined $offset); 
      my @res= $sub->( $elt);
      return $res[$offset];
    }
    1; # so the module returns 1 as this is the last BEGIN block in the file
}


sub findvalue
  { my $elt= shift;
    return join '', map { $_->text } $elt->get_xpath( @_);
  }

#end-extract twig_node


# XML::XPath compatibility
sub getElementById     { return $_[0]->twig->elt_id( $_[1]); }
sub getChildNodes      { my @children= $_[0]->children; return wantarray ? @children : \@children; }

sub _flushed     { return $_[0]->{flushed}; }
sub _set_flushed { $_[0]->{flushed}=1;      }
sub _del_flushed { delete $_[0]->{flushed}; }


sub cut
  { my $elt= shift;
    my( $parent, $prev_sibling, $next_sibling, $last_elt);

    # you can't cut the root, sorry
    unless( $parent= $elt->{parent}) 
      { return; }
    # it we cut the current element then its parent becomes the current elt
    if( $elt->{twig_current})
      { my $twig_current= $elt->{parent};
        my $t= $elt->twig;
        $t->{twig_current}= $twig_current;
        $twig_current->{'twig_current'}=1;
        delete $elt->{'twig_current'};
      }

    if( $parent->{first_child} == $elt)
      { $parent->{first_child}=  $elt->{next_sibling};
        $parent->{empty}= 1 unless( $elt->{next_sibling});
      }
    $parent->set_last_child( $elt->{prev_sibling}) 
      if( $parent->{last_child} == $elt);

    if( $prev_sibling= $elt->{prev_sibling})
      { $prev_sibling->{next_sibling}=  $elt->{next_sibling}; }
    if( $next_sibling= $elt->{next_sibling})
      { $next_sibling->set_prev_sibling( $elt->{prev_sibling}); }


    $elt->set_parent( undef);
    $elt->set_prev_sibling( undef);
    $elt->{next_sibling}=  undef;

    return $elt;
  }

sub cut_children
  { my( $elt, $exp)= @_;
    my @children= $elt->children( $exp);
    foreach (@children) { $_->cut; }
    return @children;
  }

sub erase
  { my $elt= shift;
    #you cannot erase the current element
    if( $elt->{twig_current})
      { croak "trying to erase an element before it has been completely parsed"; }
    unless( $elt->{parent})
      { # trying to erase the root (of a twig or of a cut/new element)
        my @children= $elt->children;
        unless( @children == 1)
          { croak "can only erase an element with no parent if it has a single child"; }
        $elt->_move_extra_data_after_erase;
        my $child= shift @children;
        $child->set_parent( undef);
        my $twig= $elt->twig;
        $twig->set_root( $child);
      }
    else     
      { # normal case
        $elt->_move_extra_data_after_erase;
        my @children= $elt->children;
        if( @children)
          { # elt has children, move them up
            if( $elt->{prev_sibling})
              { # connect first child to previous sibling
                $elt->{first_child}->set_prev_sibling( $elt->{prev_sibling});      
                $elt->{prev_sibling}->set_next_sibling( $elt->{first_child}); 
              }
            else
              { # elt was the first child
                $elt->{parent}->set_first_child( $elt->{first_child});
              }
            if( $elt->{next_sibling})
              { # connect last child to next sibling
                $elt->{last_child}->set_next_sibling( $elt->{next_sibling});      
                $elt->{next_sibling}->set_prev_sibling( $elt->{last_child}); 
              }
            else
              { # elt was the last child
                $elt->{parent}->set_last_child( $elt->{last_child});
              }
            # update parent for all siblings
            foreach my $child (@children)
              { $child->set_parent( $elt->{parent}); }
            # elt is not referenced any more, so it will be DESTROYed
            # so we'd better break the links to its children
            undef $elt->{'first_child'};
            undef $elt->{'last_child'};
            undef $elt->{'parent'};
            undef $elt->{'prev_sibling'};
            undef $elt->{'next_sibling'};
          }
          { # elt had no child, delete it
             $elt->delete;
          }
              
      }
    return $elt;

  }

sub _move_extra_data_after_erase
  { my( $elt)= @_;
    # extra_data
    if( my $extra_data= $elt->{extra_data}) 
      { my $target= $elt->{first_child} || $elt->{next_sibling};
        if( $target)
          {
            if( $target->is( '#ELT'))
              { $target->set_extra_data( $extra_data . ($target->extra_data || '')); }
            elsif( $target->is( '#TEXT'))
              { $target->{extra_data_in_pcdata} ||=[];
                unshift @{$target->{extra_data_in_pcdata}}, { text => $extra_data, offset => 0 };
             }
          }
        else
          { my $parent= $elt->{parent}; # always exists or the erase cannot be performed
            $parent->{extra_data_before_end_tag}= $extra_data . ($parent->{extra_data_before_end_tag}||''); 
          }
      }
      
     # extra_data_before_end_tag
    if( my $extra_data= $elt->{extra_data_before_end_tag}) 
      { if( my $target= $elt->{next_sibling})
          { if( $target->is( '#ELT'))
              { $target->set_extra_data( $extra_data . ($target->extra_data || '')); }
            elsif( $target->is( '#TEXT'))
              { $target->{extra_data_in_pcdata} ||=[];
                unshift @{$target->{extra_data_in_pcdata}}, { text => $extra_data, offset => 0 };
             }
          }
        elsif( my $parent= $elt->{parent})
          { $parent->{extra_data_before_end_tag}= $extra_data . ($parent->{extra_data_before_end_tag}||''); }
       }

    return $elt;

  }
BEGIN
  { my %method= ( before      => \&paste_before,
                  after       => \&paste_after,
                  first_child => \&paste_first_child,
                  last_child  => \&paste_last_child,
                  within      => \&paste_within,
        );
    
    # paste elt somewhere around ref
    # pos can be first_child (default), last_child, before, after or within
    sub paste
      { my $elt= shift;
        if( $elt->{parent}) 
          { croak "cannot paste an element that belongs to a tree"; }
        my $pos;
        my $ref;
        if( ref $_[0]) 
          { $pos= 'first_child'; 
            croak "wrong argument order in paste, should be $_[1] first" if($_[1]); 
          }
        else
          { $pos= shift; }

        if( my $method= $method{$pos})
          {
            unless( isa( $_[0], "XML::Twig::Elt"))
              { if( ! defined( $_[0]))
                  { croak "missing target in paste"; }
                elsif( ! ref( $_[0]))
                  { croak "wrong target type in paste (not a reference), should be XML::Twig::Elt or a subclass"; }
                else
                  { my $ref= ref $_[0];
                    croak "wrong target type in paste: '$ref', should be XML::Twig::Elt or a subclass";
                  }
              }
            $ref= $_[0];
            # check here so error message lists the caller file/line
            if( !$ref->{parent} && ($method=~ m{^(before|after)$}) ) { croak "cannot paste $1 root"; }
            $elt->$method( @_); 
          }
        else
          { croak "tried to paste in wrong position '$pos', allowed positions " . 
              " are 'first_child', 'last_child', 'before', 'after' and "    .
              "'within'";
          }
				if( (my $ids= $elt->{twig_id_list}) && (my $t= $ref->twig) )
 				  { $t->{twig_id_list}||={};
            @{$t->{twig_id_list}}{keys %$ids}= values %$ids;
          }
        return $elt;
      }
  

    sub paste_before
      { my( $elt, $ref)= @_;
        my( $parent, $prev_sibling, $next_sibling );
        unless( $ref->{parent}) { croak "cannot paste before root"; }
        $parent= $ref->{parent};
        $prev_sibling= $ref->{prev_sibling};
        $next_sibling= $ref;

        $elt->set_parent( $parent);
        $parent->{first_child}=  $elt if( $parent->{first_child} == $ref);

        $prev_sibling->{next_sibling}=  $elt if( $prev_sibling);
        $elt->set_prev_sibling( $prev_sibling);

        $next_sibling->set_prev_sibling( $elt);
        $elt->{next_sibling}=  $ref;
        return $elt;
      }
     
     sub paste_after
      { my( $elt, $ref)= @_;
        my( $parent, $prev_sibling, $next_sibling );
        unless( $ref->{parent}) { croak "cannot paste after root"; }
        $parent= $ref->{parent};
        $prev_sibling= $ref;
        $next_sibling= $ref->{next_sibling};

        $elt->set_parent( $parent);
        $parent->set_last_child( $elt) if( $parent->{last_child}== $ref);

        $prev_sibling->{next_sibling}=  $elt;
        $elt->set_prev_sibling( $prev_sibling);

        $next_sibling->set_prev_sibling( $elt) if( $next_sibling);
        $elt->{next_sibling}=  $next_sibling;
        return $elt;

      }

    sub paste_first_child
      { my( $elt, $ref)= @_;
        my( $parent, $prev_sibling, $next_sibling );
        $parent= $ref;
        $next_sibling= $ref->{first_child};
        delete $ref->{empty};

        $elt->set_parent( $parent);
        $parent->{first_child}=  $elt;
        $parent->set_last_child( $elt) unless( $parent->{last_child});

        $elt->set_prev_sibling( undef);

        $next_sibling->set_prev_sibling( $elt) if( $next_sibling);
        $elt->{next_sibling}=  $next_sibling;
        return $elt;
      }
      
    sub paste_last_child
      { my( $elt, $ref)= @_;
        my( $parent, $prev_sibling, $next_sibling );
        $parent= $ref;
        $prev_sibling= $ref->{last_child};
        delete $ref->{empty};

        $elt->set_parent( $parent);
        $parent->set_last_child( $elt);
        $parent->{first_child}=  $elt unless( $parent->{first_child});

        $elt->set_prev_sibling( $prev_sibling);
        $prev_sibling->{next_sibling}=  $elt if( $prev_sibling);

        $elt->{next_sibling}=  undef;
        return $elt;
      }

    sub paste_within
      { my( $elt, $ref, $offset)= @_;
        my $text= $ref->is_text ? $ref : $ref->next_elt( '#TEXT', $ref);
        my $new= $text->split_at( $offset);
        $elt->paste_before( $new);
        return $elt;
      }
  }

# load an element into a structure similar to XML::Simple's
sub simplify
  { my $elt= shift;

    # normalize option names
    my %options= @_;
    %options= map { my ($key, $val)= ($_, $options{$_});
                       $key=~ s{(\w)([A-Z])}{$1_\L$2}g;
                       $key => $val
                     } keys %options;

    # check options
    my @allowed_options= qw( keyattr forcearray noattr content_key
                             var var_regexp variables var_attr 
                             group_tags forcecontent
                             normalise_space normalize_space
                   );
    my %allowed_options= map { $_ => 1 } @allowed_options;
    foreach my $option (keys %options)
      { warn "invalid option $option\n" unless( $allowed_options{$option}); }

    $options{normalise_space} ||= $options{normalize_space} || 0;
    
    $options{content_key} ||= 'content';
    if( $options{content_key}=~ m{^-})
      { # need to remove the - and to activate extra folding
        $options{content_key}=~ s{^-}{};
        $options{extra_folding}= 1;
      }
    else
      { $options{extra_folding}= 0; }
   
    $options{forcearray} ||=0; 
    if( isa( $options{forcearray}, 'ARRAY'))
      { my %forcearray_tags= map { $_ => 1 } @{$options{forcearray}};
        $options{forcearray_tags}= \%forcearray_tags;
        $options{forcearray}= 0;
      }

    $options{keyattr}     ||= ['name', 'key', 'id'];
    if( ref $options{keyattr} eq 'ARRAY')
      { foreach my $keyattr (@{$options{keyattr}})
          { my( $prefix, $att)= ($keyattr=~ m{^([+-])?(.*)});
            $prefix ||= '';
            $options{key_for_all}->{$att}= 1;
            $options{remove_key_for_all}->{$att}=1 unless( $prefix eq '+');
            $options{prefix_key_for_all}->{$att}=1 if( $prefix eq '-');
          }
      }
    elsif( ref $options{keyattr} eq 'HASH')
      { while( my( $elt, $keyattr)= each %{$options{keyattr}})
         { my( $prefix, $att)= ($keyattr=~ m{^([+-])?(.*)});
           $prefix ||='';
           $options{key_for_elt}->{$elt}= $att;
           $options{remove_key_for_elt}->{"$elt#$att"}=1 unless( $prefix);
           $options{prefix_key_for_elt}->{"$elt#$att"}=1 if( $prefix eq '-');
         }
      }
       

    $options{var}||= $options{var_attr}; # for compat with XML::Simple
    if( $options{var}) { $options{var_values}= {}; }
    else               { $options{var}='';         }

    if( $options{variables}) 
      { $options{var}||= 1;
        $options{var_values}= $options{variables};
      }

    if( $options{var_regexp} and !$options{var})
      { warn "var option not used, var_regexp option ignored\n"; }
    $options{var_regexp} ||= '\$\{?(\w+)\}?';
      
    $elt->_simplify( \%options);
 
 }

sub _simplify
  { my( $elt, $options)= @_;

    my $data;

    my $gi= $XML::Twig::index2gi[$elt->{'gi'}];
    my @children= $elt->children;
    my %atts= $options->{noattr} ? () : %{$elt->atts};
    my $nb_atts= keys %atts;
    my $nb_children= $elt->children_count + $nb_atts;

    my %nb_children;
    foreach (@children)   { $nb_children{$_->tag}++; }
    foreach (keys %atts)  { $nb_children{$_}++;      }

    my $arrays; # tag => array where elements are stored


    # store children
    foreach my $child (@children)
      { if( $child->is_text)
          { # generate with a content key
            my $text= $elt->_text_with_vars( $options);
            $text= _normalize_space( $text) if( $options->{normalise_space} >= 2);
            if(    $options->{force_content}
                || $nb_atts 
                || (scalar @children > 1)
              )
              { $data->{$options->{content_key}}= $text; }
            else
              { $data= $text; }
          }
        else
          { # element with sub elements
            my $child_gi= $XML::Twig::index2gi[$child->{'gi'}];

            my $child_data= $child->_simplify( $options);

            # first see if we need to simplify further the child data
            # simplify because of grouped tags
            if( my $grouped_tag= $options->{group_tags}->{$child_gi})
              { # check that the child data is a hash with a single field
                unless(    (ref( $child_data) eq 'HASH')
                        && (keys %$child_data == 1)
                        && defined ( my $grouped_child_data= $child_data->{$grouped_tag})
                      )
                  { die "error in grouped tag $child_gi"; }
                else
                  { $child_data=  $grouped_child_data; }
              }
            # simplify because of extra folding
            if( $options->{extra_folding})
              { if(    (ref( $child_data) eq 'HASH')
                    && (keys %$child_data == 1)
                    && defined( my $content= $child_data->{$options->{content_key}})
                  )
                  { $child_data= $content; }
              }


            if( my $keyatt= $child->_key_attr( $options))
              { # simplify element with key
                my $key= $child->{'att'}->{$keyatt};
                $key= _normalize_space( $key) if( $options->{normalise_space} >= 1);
                $data->{$child_gi}->{$key}= $child_data;
              }
            elsif(      $options->{forcearray}
                   ||   $options->{forcearray_tags}->{$child_gi}
                   || ( $nb_children{$child_gi} > 1)
                 )
              { # simplify element to store in an array
                $data->{$child_gi} ||= [];
                push @{$data->{$child_gi}}, $child_data;
              }
            else
              { # simplify element to store as a hash field
                $data->{$child_gi}= $child_data;
              }

          }
    }

    # store atts
    # TODO: deal with att that already have an element by that name
    foreach my $att (keys %atts)
      { # do not store if the att is a key that needs to be removed
        if(    $options->{remove_key_for_all}->{$att}
            || $options->{remove_key_for_elt}->{"$gi#$att"}
          )
          { next; }

        my $att_text= _replace_vars_in_text( $atts{$att}, $options);
        $att_text= _normalize_space( $att_text) if( $options->{normalise_space} >= 2);
        
        if(    $options->{prefix_key_for_all}->{$att}
            || $options->{prefix_key_for_elt}->{"$gi#$att"}
          )
          { # prefix the att
            $data->{"-$att"}= $att_text;
          } 
        else
          { # normal case
            $data->{$att}= $att_text; 
          }
      }
    
    return $data;
  }

sub _key_attr
  { my( $elt, $options)=@_;
    return if( $options->{noattr});
    if( $options->{key_for_all})
      { foreach my $att ($elt->att_names)
          { if( $options->{key_for_all}->{$att})
              { return $att; }
          }
      }
    elsif( $options->{key_for_elt})
      { if( my $key_for_elt= $options->{key_for_elt}->{$XML::Twig::index2gi[$elt->{'gi'}]} )
          { return $key_for_elt if( defined( $elt->{'att'}->{$key_for_elt})); }
      }
    return;
  }

sub _text_with_vars
  { my( $elt, $options)= @_;
    my $text;
    if( $options->{var}) 
      { $text= _replace_vars_in_text( $elt->text, $options); 
        $elt->_store_var( $options);
      }
     else
      { $text= $elt->text; }
    return $text;
  }


sub _normalize_space
  { my $text= shift;
    $text=~ s{\s+}{ }sg;
    $text=~ s{^\s}{};
    $text=~ s{\s$}{};
    return $text;
  }


sub att_nb
  { return 0 unless( my $atts= $_[0]->atts);
    return scalar keys %$atts;
  }
    
sub has_no_atts
  { return 1 unless( my $atts= $_[0]->atts);
    return scalar keys %$atts ? 0 : 1;
 }
    
sub _replace_vars_in_text
  { my( $text, $options)= @_;

    $text=~ s{($options->{var_regexp})}
             { if( defined( my $value= $options->{var_values}->{$2}))
                 { $value }
               else
                 { warn "unknown variable $2\n";
                   $1
                 }
             }gex;
    return $text;
  }

sub _store_var
  { my( $elt, $options)= @_;
    if( defined (my $var_name= $elt->{'att'}->{$options->{var}}))
       { $options->{var_values}->{$var_name}= $elt->text; 
       }
  }


# split a text element at a given offset
sub split_at
  { my( $elt, $offset)= @_;
    my $text_elt= $elt->is_text ? $elt : $elt->first_child( TEXT) || return '';
    my $string= $text_elt->text; 
    my $left_string= substr( $string, 0, $offset);
    my $right_string= substr( $string, $offset);
    $text_elt->{pcdata}=  $left_string;
    my $new_elt= XML::Twig::Elt->new( $XML::Twig::index2gi[$elt->{'gi'}], $right_string);
    $new_elt->paste( after => $elt);
    return $new_elt;
  }

    
# split an element or its text descendants into several, in place
# all elements (new and untouched) are returned
sub split    
  { my $elt= shift;
    my @text_chunks;
    my @result;
    if( $elt->is_text) { @text_chunks= ($elt); }
    else               { @text_chunks= $elt->descendants( '#TEXT'); }
    foreach my $text_chunk (@text_chunks)
      { push @result, $text_chunk->_split( 1, @_); }
    return @result;
  }

# split an element or its text descendants into several, in place
# created elements (those which match the regexp) are returned
sub mark
  { my $elt= shift;
    my @text_chunks;
    my @result;
    if( $elt->is_text) { @text_chunks= ($elt); }
    else               { @text_chunks= $elt->descendants( '#TEXT'); }
    foreach my $text_chunk (@text_chunks)
      { push @result, $text_chunk->_split( 0, @_); }
    return @result;
  }

# split a single text element
# return_all defines what is returned: if it is true 
# only returns the elements created by matches in the split regexp
# otherwise all elements (new and untouched) are returned

{ my $encode_is_loaded=0;   # so we only load Encode once in 5.8.0+
 
  sub _split
    { my $elt= shift;
      my $return_all= shift;
      my $regexp= shift;
      my @tags;

      while( my $tag= shift())
        { if( ref $_[0]) 
            { push @tags, { tag => $tag, atts => shift }; }
          else
            { push @tags, { tag => $tag }; }
        }

      unless( @tags) { @tags= { tag => $elt->parent( '#ELT')->gi }; }
          
      my @result;                                 # the returned list of elements
      my $text= $elt->text;
      my $gi= $XML::Twig::index2gi[$elt->{'gi'}];
  
      # 2 uses: if split matches then the first substring reuses $elt
      #         once a split has occured then the last match needs to be put in
      #         a new element      
      my $previous_match= 0;

      while( my( $pre_match, @matches)= $text=~ /^(.*?)$regexp(.*)$/gcs)
        { $text= pop @matches;
          if( $previous_match)
            { # match, not the first one, create a new text ($gi) element
              $pre_match= _utf8_ify( $pre_match);
              $elt= $elt->insert_new_elt( after => $gi, $pre_match);
              push @result, $elt if( $return_all);
            }
          else
            { # first match in $elt, re-use $elt for the first sub-string
              $elt->set_text( _utf8_ify( $pre_match));
              $previous_match++;                # store the fact that there was a match
              push @result, $elt if( $return_all);
            }

          # now deal with matches captured in the regexp
          if( @matches)
            { # match, with capture
              my $i=0;
              foreach my $match (@matches)
                { # create new element, text is the match
                  $match= _utf8_ify( $match);
                  my $tag  = $tags[$i]->{tag};
                  my $atts = \%{$tags[$i]->{atts}} || {};
                  $elt= $elt->insert_new_elt( after => $tag, $atts, $match);
                  push @result, $elt;
                  $i= ($i + 1) % @tags;
                }
            }
          else
            { # match, no captures
              my $tag  = $tags[0]->{tag};
              my $atts = \%{$tags[0]->{atts}} || {};
              $elt=  $elt->insert_new_elt( after => $tag, $atts);
              push @result, $elt;
            }
        }
      if( $previous_match && $text)
        { # there was at least 1 match, and there is text left after the match
          $elt= $elt->insert_new_elt( after => $gi, $text);
        }

      push @result, $elt if( $return_all);

      return @result; # return all elements
   }

  # evil hack needed in 5.8.0, the utf flag is not set on $<n>...
  sub _utf8_ify
    { my $string= shift;
      if( $] == 5.008 and !_keep_encoding()) 
        { unless( $encode_is_loaded) { require Encode; import Encode; $encode_is_loaded++; }
          Encode::_utf8_on( $string); # the flag should be set but is not
        }
      return $string;
    }


}

{ my %replace_sub; # cache for complex expressions (expression => sub)

  sub subs_text
    { my( $elt, $regexp, $replace)= @_;
  
      my $replacement_string;
      my $is_string= _is_string( $replace);
      foreach my $text_elt ($elt->descendants_or_self( '#TEXT'))
        { if( $is_string)
            { my $text= $text_elt->text;
              $text=~ s{$regexp}{ _replace_var( $replace, $1, $2, $3, $4, $5, $6, $7, $8, $9)}egx;
              $text_elt->set_text( $text);
           }
          else
            { my $replace_sub= ( $replace_sub{$replace} ||= _install_replace_sub( $replace)); 
              my $text= $text_elt->text;
              my $pos=0;  # used to skip text that was previously matched
              while( my( $pre_match_string, $match_string, @var)= ($text=~ m{(.*?)($regexp)}sg))
                { my $match_start  = length( $pre_match_string);
                  my $match        = $text_elt->split_at( $match_start + $pos);
                  my $match_length = length( $match_string);
                  my $post_match   = $match->split_at( $match_length);
                  $replace_sub->( $match, @var);
                  # merge previous text with current one
                  my $next_sibling;
                  if(    ($next_sibling= $text_elt->{next_sibling})
                      && ($XML::Twig::index2gi[$text_elt->{'gi'}] eq $XML::Twig::index2gi[$next_sibling->{'gi'}])
                    )
                    { $text_elt->merge_text( $next_sibling); }
                  # go to next 
                  $text_elt= $post_match;
                  $text= $post_match->text;
                  # merge last text element with next one if needed,
                  # the match will be against the non-matched text,
                  # so $pos is used to skip the merged part
                  my $prev_sibling;
                  if(    ($prev_sibling=  $post_match->{prev_sibling})
                      && ($XML::Twig::index2gi[$post_match->{'gi'}] eq $XML::Twig::index2gi[$prev_sibling->{'gi'}])
                    )
                    { $pos= length( $prev_sibling->text);
                      $prev_sibling->merge_text( $post_match); 
                    }
                }
            }
        }
      return $elt;
    }


  sub _is_string
    { return ($_[0]=~ m{&e[ln]t}) ? 0: 1 }

  sub _replace_var
    { my( $string, @var)= @_;
      unshift @var, undef;
      $string=~ s{\$(\d)}{$var[$1]}g;
      return $string;
    }

  sub _install_replace_sub
    { my $replace_exp= shift;
      my @item= split m{(&e[ln]t\s*\([^)]*\))}, $replace_exp;
      my $sub= q{ my( $match, @var)= @_; unshift @var, undef; my $new; };
      my( $gi, $exp);
      foreach my $item (@item)
        { if(    $item=~ m{^&elt\s*\(([^)]*)\)})
            { $exp= $1;
            }
          elsif( $item=~ m{^&ent\s*\(\s*([^\s)]*)\s*\)})
            { $exp= " '#ENT' => $1"; }
          else
            { $exp= qq{ '#PCDATA' => "$item"}; }
          $exp=~ s{\$(\d)}{\$var[$1]}g; # replace references to matches
          $sub.= qq{ \$new= XML::Twig::Elt->new( $exp); };
          $sub .= q{ $new->paste( before => $match); };
        }
      $sub .= q{ $match->delete; };
      #$sub=~ s/;/;\n/g;
      my $coderef= eval "sub { $sub }";
      if( $@) { croak( "invalid replacement expression $replace_exp: ",$@); }
      return $coderef;
    }

  }


sub merge_text
  { my( $e1, $e2)= @_;
    croak "invalid merge: can only merge 2 elements" 
        unless( isa( $e2, 'XML::Twig::Elt'));
    croak "invalid merge: can only merge 2 text elements" 
        unless( $e1->is_text && $e2->is_text && ($e1->gi eq $e2->gi));
    $e1->set_text( $e1->text . $e2->text);
    $e2->delete;
    return $e1;
  }


# recursively copy an element and returns the copy (can be huge and long)
sub copy
  { my $elt= shift;
    my $copy= XML::Twig::Elt->new( $XML::Twig::index2gi[$elt->{'gi'}]);

    $copy->set_extra_data( $elt->extra_data) if( $elt->extra_data);
    $copy->{extra_data_before_end_tag}= $elt->{extra_data_before_end_tag} if( $elt->{extra_data_before_end_tag});

    $copy->set_asis                          if( $elt->is_asis);
    if( ($elt->{'empty'} || 0)) { $copy->{empty}= 1; } # do not swap or speedup will mess up this                         

    if( (exists $elt->{'pcdata'}))
      { $copy->{pcdata}=  $elt->{pcdata}; 
        $copy->{extra_data_in_pcdata}= $elt->{extra_data_in_pcdata} if( $elt->{extra_data_in_pcdata});
      }
    elsif( (exists $elt->{'cdata'}))
      { $copy->{cdata}=  $elt->{cdata}; 
        $copy->{extra_data_in_pcdata}= $elt->{extra_data_in_pcdata} if( $elt->{extra_data_in_pcdata});
      }
    elsif( (exists $elt->{'target'}))
      { $copy->set_pi( $elt->{target}, $elt->{data}); }
    elsif( (exists $elt->{'comment'}))
      { $copy->{comment}=  $elt->{comment}; }
    elsif( (exists $elt->{'ent'}))
      { $copy->{ent}=  $elt->{ent}; }
    else
      { my @children= $elt->children;
        if( my $atts= $elt->atts)
          { my %atts= %{$atts}; # we want to do a real copy of the attributes
            $copy->set_atts( \%atts);
          }
        foreach my $child (@children)
          { my $child_copy= $child->copy;
            $child_copy->paste( 'last_child', $copy);
          }
      }
    return $copy;
  }

sub delete
  { my $elt= shift;
    $elt->cut;
    $elt->DESTROY unless( $XML::Twig::weakrefs);
    return undef;
  }

{ 
  sub DESTROY
    { my $elt= shift;
      my $t= shift || $elt->twig; # optional argument, passed in recursive calls
      return if( $XML::Twig::weakrefs);

      foreach( @{[$elt->children]}) { $_->DESTROY( $t); }

      # the id reference needs to be destroyed
      # lots of tests to avoid warnings during the cleanup phase
      $elt->del_id( $t) if( $ID && $t && defined( $elt->{att}) && exists( $elt->{att}->{$ID}));
      undef $elt;
    }
}


# to be called only from a start_tag_handler: ignores the current element
sub ignore
  { my $elt= shift;
    my $t= $elt->twig;
    $t->ignore( $elt, @_);
  }

BEGIN {
  my $pretty                    = 0;
  my $quote                     = '"';
  my $INDENT                    = '  ';
  my $empty_tag_style           = 0;
  my $remove_cdata              = 0;
  my $keep_encoding             = 0;
  my $expand_external_entities  = 0;
  my $keep_atts_order           = 0;
  my $do_not_escape_amp_in_atts = 0;

  my ($NSGMLS, $NICE, $INDENTED, $INDENTEDC, $RECORD1, $RECORD2)= (1..6);

  my %pretty_print_style=
    ( none       => 0,          # no added \n
      nsgmls     => $NSGMLS,    # nsgmls-style, \n in tags
      # below this line styles are UNSAFE (the generated XML can be invalid)
      nice       => $NICE,      # \n after open/close tags except when the 
                                # element starts with text
      indented   => $INDENTED,  # nice plus idented
      indented_c => $INDENTEDC, # slightly more compact than indented (closing
                                # tags are on the same line)
      record_c   => $RECORD1,   # for record-like data (compact)
      record     => $RECORD2,   # for record-like data  (not so compact)
    );

  my ($HTML, $EXPAND)= (1..2);
  my %empty_tag_style=
    ( normal => 0,        # <tag/>
      html   => $HTML,    # <tag />
      xhtml  => $HTML,    # <tag />
      expand => $EXPAND,  # <tag></tag>
    );

  my %quote_style=
    ( double  => '"',    
      single  => "'", 
      # smart  => "smart", 
    );

  my $xml_space_preserve; # set when an element includes xml:space="preserve"

  my $output_filter;      # filters the entire output (including < and >)
  my $output_text_filter; # filters only the text part (tag names, attributes, pcdata)


  # returns those pesky "global" variables so you can switch between twigs 
  sub global_state
    { return
       { pretty                    => $pretty,
         quote                     => $quote,
         indent                    => $INDENT,
         empty_tag_style           => $empty_tag_style,
         remove_cdata              => $remove_cdata,
         keep_encoding             => $keep_encoding,
         expand_external_entities  => $expand_external_entities,
         output_filter             => $output_filter,
         output_text_filter        => $output_text_filter,
         keep_atts_order           => $keep_atts_order,
         do_not_escape_amp_in_atts => $do_not_escape_amp_in_atts,
        };
    }

  # restores the global variables
  sub set_global_state
    { my $state= shift;
      $pretty                    = $state->{pretty};
      $quote                     = $state->{quote};
      $INDENT                    = $state->{indent};
      $empty_tag_style           = $state->{empty_tag_style};
      $remove_cdata              = $state->{remove_cdata};
      $keep_encoding             = $state->{keep_encoding};
      $expand_external_entities  = $state->{expand_external_entities};
      $output_filter             = $state->{output_filter};
      $output_text_filter        = $state->{output_text_filter};
      $keep_atts_order           = $state->{keep_atts_order};
      $do_not_escape_amp_in_atts = $state->{do_not_escape_amp_in_atts};
    }

  # sets global state to defaults
  sub init_global_state
    { set_global_state(
       { pretty                    => 0,
         quote                     => '"',
         indent                    => $INDENT,
         empty_tag_style           => 0,
         remove_cdata              => 0,
         keep_encoding             => 0,
         expand_external_entities  => 0,
         output_filter             => undef,
         output_text_filter        => undef,
         keep_atts_order           => undef,
         do_not_escape_amp_in_atts => 0,
        });
    }


  # set the pretty_print style (in $pretty) and returns the old one
  # can be called from outside the package with 2 arguments (elt, style)
  # or from inside with only one argument (style)
  # the style can be either a string (one of the keys of %pretty_print_style
  # or a number (presumably an old value saved)
  sub set_pretty_print
    { my $style= lc( defined $_[1] ? $_[1] : $_[0]); # so we cover both cases 
      my $old_pretty= $pretty;
      if( $style=~ /^\d+$/)
        { croak "invalid pretty print style $style"
        unless( $style < keys %pretty_print_style);
        $pretty= $style;
    }
      else
        { croak "invalid pretty print style '$style'"
            unless( exists $pretty_print_style{$style});
          $pretty= $pretty_print_style{$style};
    }
      return $old_pretty;
    }
  
  
  # set the empty tag style (in $empty_tag_style) and returns the old one
  # can be called from outside the package with 2 arguments (elt, style)
  # or from inside with only one argument (style)
  # the style can be either a string (one of the keys of %empty_tag_style
  # or a number (presumably an old value saved)
  sub set_empty_tag_style
    { my $style= lc( defined $_[1] ? $_[1] : $_[0]); # so we cover both cases 
      my $old_style= $empty_tag_style;
      if( $style=~ /^\d+$/)
        { croak "invalid empty tag style $style"
        unless( $style < keys %empty_tag_style);
        $empty_tag_style= $style;
        }
      else
        { croak "invalid empty tag style '$style'"
            unless( exists $empty_tag_style{$style});
          $empty_tag_style= $empty_tag_style{$style};
        }
      return $old_style;
    }
      
  sub set_quote
    { my $style= $_[1] || $_[0];
      my $old_quote= $quote;
      croak "invalid quote '$style'" unless( exists $quote_style{$style});
      $quote= $quote_style{$style};
      return $old_quote;
    }
    
  sub set_remove_cdata
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $remove_cdata;
      $remove_cdata= $new_value;
      return $old_value;
    }
      
      
  sub set_indent
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $INDENT;
      $INDENT= $new_value;
      return $old_value;
    }
       
  sub set_keep_encoding
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $keep_encoding;
      $keep_encoding= $new_value;
      return $old_value;
   }

  sub _keep_encoding { return $keep_encoding; } # so I can use elsewhere in the module

  sub set_do_not_escape_amp_in_atts
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $do_not_escape_amp_in_atts;
      $do_not_escape_amp_in_atts= $new_value;
      return $old_value;
   }

  sub output_filter      { return $output_filter; }
  sub output_text_filter { return $output_text_filter; }

  sub set_output_filter
    { my $new_value= defined $_[1] ? $_[1] : $_[0]; # can be called in object/non-object mode
      # if called in object mode with no argument, the filter is undefined
      if( isa( $new_value, 'XML::Twig::Elt') || isa( $new_value, 'XML::Twig')) { undef $new_value; }
      my $old_value= $output_filter;
      if( !$new_value || isa( $new_value, 'CODE') )
        { $output_filter= $new_value; }
      elsif( $new_value eq 'latin1')
        { $output_filter= XML::Twig::latin1();
        }
      elsif( $XML::Twig::filter{$new_value})
        {  $output_filter= $XML::Twig::filter{$new_value}; }
      else
        { croak "invalid output filter '$new_value'"; }
      
      return $old_value;
    }
       
  sub set_output_text_filter
    { my $new_value= defined $_[1] ? $_[1] : $_[0]; # can be called in object/non-object mode
      # if called in object mode with no argument, the filter is undefined
      if( isa( $new_value, 'XML::Twig::Elt') || isa( $new_value, 'XML::Twig')) { undef $new_value; }
      my $old_value= $output_text_filter;
      if( !$new_value || isa( $new_value, 'CODE') )
        { $output_text_filter= $new_value; }
      elsif( $new_value eq 'latin1')
        { $output_text_filter= XML::Twig::latin1();
        }
      elsif( $XML::Twig::filter{$new_value})
        {  $output_text_filter= $XML::Twig::filter{$new_value}; }
      else
        { croak "invalid output text filter '$new_value'"; }
      
      return $old_value;
    }
       
  sub set_expand_external_entities
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $expand_external_entities;
      $expand_external_entities= $new_value;
      return $old_value;
    }
       
  sub set_keep_atts_order
    { my $new_value= defined $_[1] ? $_[1] : $_[0];
      my $old_value= $keep_atts_order;
      $keep_atts_order= $new_value;
      return $old_value;
    
   }

  sub keep_atts_order { return $keep_atts_order; } # so I can use elsewhere in the module

  # $elt is an element to print
  # $pretty is an optional value, if true a \n is printed after the <

  my %empty_should_be_expanded= ( script => 1);

  sub start_tag
    { my $elt= shift;
  
      return if( $elt->{gi}<$XML::Twig::SPECIAL_GI);

      my $extra_data= $elt->{extra_data} || '';

      my $gi= $XML::Twig::index2gi[$elt->{'gi'}];

      my $ns_map= $elt->{'att'}->{'#original_gi'};
      if( $ns_map) { $gi= _restore_original_prefix( $ns_map, $gi); }
      $gi=~ s{^#default:}{}; # remove default prefix
 
      if( $output_text_filter) { $gi= $output_text_filter->( $gi); }

      my $tag="<" . $gi;
  
      # get the attribute and their values
      my $att= $elt->atts;
      if( $att)
        { foreach my $att_name ( $keep_atts_order ?  keys %{$att} : sort keys %{$att}) 
           { # skip private attributes (they start with #)
             next if( ( (substr( $att_name, 0, 1) eq '#') && (substr( $att_name, 0, 9) ne '#default:') ));

             $tag .=  $pretty==$NSGMLS ? "\n" : ' ';

             my $output_att_name= $ns_map ? _restore_original_prefix( $ns_map, $att_name) : $att_name;
             if( $output_text_filter) { $output_att_name=  $output_text_filter->( $output_att_name); }

             $tag .= $output_att_name . '=' . $quote . $elt->att_xml_string( $att_name, $quote) . $quote; 
           }
        } 
  
      $tag .= "\n" if($pretty==$NSGMLS);

      if( $elt->{empty} && !$elt->{extra_data_before_end_tag})
        { if( !$empty_tag_style)
            { $tag .= "/>";     }
          elsif( ($empty_tag_style eq $HTML) && ! $empty_should_be_expanded{$XML::Twig::index2gi[$elt->{'gi'}]})
            { $tag .= " />";  }
          else #  $empty_tag_style eq $EXPAND
            { $tag .= "></" . $XML::Twig::index2gi[$elt->{'gi'}] .">";  }
        }
      else
        { $tag .= ">"; }

      if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') )) { $tag= ''; }

      unless( $pretty) { return $extra_data . $tag  }

      my $prefix='';
      my $return='';   # '' or \n is to be printed before the tag
      my $indent=0;    # number of indents before the tag

      if( $pretty==$RECORD1)
        { my $level= $elt->level;
          $return= "\n" if( $level < 2);
          $indent= 1 if( $level == 1);
        }

     elsif( $pretty==$RECORD2)
        { $return= "\n";
          $indent= $elt->level;
        }

      elsif( $pretty==$NICE)
        { my $parent= $elt->{parent};
          unless( !$parent || $parent->{contains_text}) 
            { $return= "\n"; }
          $elt->{contains_text}= 1 if( ($parent && $parent->{contains_text})
                                     || $elt->contains_text);
        }

      elsif( ($pretty==$INDENTED) || ($pretty==$INDENTEDC))
        { my $parent= $elt->{parent};
          unless( !$parent || $parent->{contains_text}) 
            { $return= "\n"; 
              $indent= $elt->level; 
            }
          $elt->{contains_text}= 1 if( ($parent && $parent->{contains_text})
                                     || $elt->contains_text);
        }

      if( $return || $indent)
        { # check for elements in which spaces should be kept
          my $t= $elt->twig;
          return $extra_data . $tag if( $xml_space_preserve);
          if( $t && $t->{twig_keep_spaces_in})
            { foreach my $ancestor ($elt->ancestors)
                { return $extra_data . $tag if( $t->{twig_keep_spaces_in}->{$XML::Twig::index2gi[$ancestor->{'gi'}]}) }
            }
        
          $prefix= $INDENT x $indent;
          if( $extra_data)
            { $extra_data=~ s{\s+$}{};
              $extra_data=~ s{^\s+}{};
              $extra_data= $prefix .  $extra_data . $return;
            }
        }


      return $return . $extra_data . $prefix . $tag;
    }
  
  sub end_tag
    { my $elt= shift;
      return  '' if(    ($elt->{gi}<$XML::Twig::SPECIAL_GI) 
                     || (($elt->{'empty'} || 0) && !$elt->{extra_data_before_end_tag})
                   );
      my $tag= "<";
      my $gi= $XML::Twig::index2gi[$elt->{'gi'}];

      if( my $map= $elt->{'att'}->{'#original_gi'}) { $gi= _restore_original_prefix( $map, $gi); }
      $gi=~ s{^#default:}{}; # remove default prefix

      if( $output_text_filter) { $gi= $output_text_filter->( $XML::Twig::index2gi[$elt->{'gi'}]); } 
      $tag .=  "/$gi>";

      $tag = ($elt->{extra_data_before_end_tag} || '') . $tag;

      if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') )) { $tag= ''; }

      return $tag unless $pretty;

      my $prefix='';
      my $return=0;    # 1 if a \n is to be printed before the tag
      my $indent=0;    # number of indents before the tag

      if( $pretty==$RECORD1)
        { $return= 1 if( $elt->level == 0);
        }

     elsif( $pretty==$RECORD2)
        { unless( $elt->contains_text)
            { $return= 1 ;
              $indent= $elt->level;
            }
        }

      elsif( $pretty==$NICE)
        { my $parent= $elt->{parent};
          if( (    ($parent && !$parent->{contains_text}) || !$parent )
            && ( !$elt->{contains_text}  
             && ($elt->{has_flushed_child} || $elt->_first_child())           
           )
         )
            { $return= 1; }
        }

      elsif( $pretty==$INDENTED)
        { my $parent= $elt->{parent};
          if( (    ($parent && !$parent->{contains_text}) || !$parent )
            && ( !$elt->{contains_text}  
             && ($elt->{has_flushed_child} || $elt->_first_child())           
           )
         )
            { $return= 1; 
              $indent= $elt->level; 
            }
        }

      if( $return || $indent)
        { # check for elements in which spaces should be kept
          my $t= $elt->twig;
          return $tag if( $xml_space_preserve);
          if( $t && $t->{twig_keep_spaces_in})
            { foreach my $ancestor ($elt, $elt->ancestors)
                { return $tag if( $t->{twig_keep_spaces_in}->{$XML::Twig::index2gi[$ancestor->{'gi'}]}) }
            }
      
          $prefix= "\n" if( $return);
          $prefix.= $INDENT x $indent;
    }

      # add a \n at the end of the document (after the root element)
      $tag .= "\n" unless( $elt->{parent});
  
      return $prefix . $tag;
    }

  sub _restore_original_prefix
    { my( $map, $name)= @_;
      my $prefix= _ns_prefix( $name);
      if( my $original_prefix= $map->{$prefix})
        { if( $original_prefix eq '#default')
            { $name=~ s{^$prefix:}{}; }
          else
            { $name=~ s{^$prefix(?=:)}{$original_prefix}; }
        }
      return $name;
    }

  # $elt is an element to print
  # $fh is an optional filehandle to print to
  # $pretty is an optional value, if true a \n is printed after the < of the
  # opening tag
  sub print
    { my $elt= shift;
  
      my $pretty;
      my $fh= _is_fh( $_[0]) ? shift : undef;
      my $old_select= defined $fh ? select $fh : undef;
      my $old_pretty= defined ($pretty= shift) ? set_pretty_print( $pretty) : undef;

      $xml_space_preserve= 1 if( ($elt->inherit_att( 'xml:space') || '') eq 'preserve');
 
      #$elt->_print;
      print $elt->sprint;

      $xml_space_preserve= 0;
    
      select $old_select if( defined $old_select);
      set_pretty_print( $old_pretty) if( defined $old_pretty);
    }
      
  
  # same as output but does not output the start tag if the element
  # is marked as flushed
  sub flush
    { my $elt= shift;
      $elt->twig->flush( @_);
    }
  
  sub _flush
    { my $elt= shift;
  
      my $pretty;
      my $fh=  _is_fh( $_[0]) ? shift : undef;
      my $old_select= defined $fh ? select $fh : undef;
      my $old_pretty= defined ($pretty= shift) ? set_pretty_print( $pretty) : undef;

      $xml_space_preserve= 1 if( ($elt->inherit_att( 'xml:space') || '') eq 'preserve');

      $elt->__flush();

      $xml_space_preserve= 0;

      select $old_select if( defined $old_select);
      set_pretty_print( $old_pretty) if( defined $old_pretty);
    }

  sub __flush
    { my $elt= shift;
  
      # in case there's some comments or PI's piggybacking
      #if( $elt->{extra_data})
      #  { print $output_filter ? $output_filter->($elt->{extra_data}) 
      #                         : $elt->{extra_data};
      #  }

      if( $elt->{gi} >= $XML::Twig::SPECIAL_GI)
        { my $preserve= ($elt->{'att'}->{'xml:space'} || '') eq 'preserve';
          $xml_space_preserve++ if $preserve;
          unless( $elt->_flushed)
            { print $elt->start_tag();
            }
      
          # flush the children
          my @children= $elt->children;
          foreach my $child (@children)
            { $child->_flush( $pretty); 
        }
          print $elt->end_tag;
          $xml_space_preserve-- if $preserve;
          # used for pretty printing
          if( my $parent= $elt->{parent}) { $parent->{has_flushed_child}= 1; }
        }
      else # text or special element
        { my $text;
          if( (exists $elt->{'pcdata'}))     { $text= $elt->pcdata_xml_string; 
                                     if( my $parent= $elt->{parent}) 
                                       { $parent->{contains_text}= 1; }
                                   }
          elsif( (exists $elt->{'cdata'}))   { $text= $elt->cdata_string;        
                                     if( my $parent= $elt->{parent}) 
                                       { $parent->{contains_text}= 1; }
                                   }
          elsif( (exists $elt->{'target'}))      { $text= $elt->pi_string;          }
          elsif( (exists $elt->{'comment'})) { $text= $elt->comment_string;     }
          elsif( (exists $elt->{'ent'}))     { $text= $elt->ent_string;         }

          print $output_filter ? $output_filter->( $text) : $text;
        }
    }
  

  sub xml_text
    { my $elt= shift;
      my $string='';

      if( $elt->{gi} >= $XML::Twig::SPECIAL_GI)
        { # sprint the children
          my $child= $elt->{first_child}||'';
          while( $child)
            { $string.= $child->xml_text;
              $child= $child->{next_sibling};
            }
        }
      elsif( (exists $elt->{'pcdata'}))  { $string .= $output_filter ?  $output_filter->($elt->pcdata_xml_string) 
                                                           : $elt->pcdata_xml_string; 
                               }
      elsif( (exists $elt->{'cdata'}))   { $string .= $output_filter ?  $output_filter->($elt->cdata_xml_string)  
                                                           : $elt->cdata_string;      
                               }
      elsif( (exists $elt->{'ent'}))     { $string .= $elt->ent_string; }

      return $string;
    }


  # same as print but except... it does not print but rather returns the string
  # if the second parameter is set then only the content is returned, not the
  # start and end tags of the element (but the tags of the included elements are
  # returned)
  sub sprint
    { my $elt= shift;
      $xml_space_preserve= 1 if( ($elt->inherit_att( 'xml:space') || '') eq 'preserve');
      my $sprint= $output_filter ? $output_filter->( $elt->_sprint( @_)) : $elt->_sprint( @_);
      $xml_space_preserve= 0;
      return $sprint;
    }
  
  sub _sprint
    { my $elt= shift;
      my $no_tag= shift || 0;
      # in case there's some comments or PI's piggybacking
      my $string='';
      #if( $elt->{extra_data} && !$no_tag)
      #  { $string= $elt->{extra_data};
      #  }

      if( $elt->{gi} >= $XML::Twig::SPECIAL_GI)
        {
          my $preserve= ($elt->{'att'}->{'xml:space'} || '') eq 'preserve';
          $xml_space_preserve++ if $preserve;

          $string.= $elt->start_tag unless( $no_tag);
      
          # sprint the children
          my $child= $elt->{first_child}||'';
          while( $child)
            { $string.= $child->_sprint;
              $child= $child->{next_sibling};
            }
          $string.= $elt->end_tag unless( $no_tag);
          $xml_space_preserve-- if $preserve;
        }
      else
        { $string .= $elt->{extra_data} || '';
             if( (exists $elt->{'pcdata'}))  { $string .= $elt->pcdata_xml_string; }
          elsif( (exists $elt->{'cdata'}))   { $string .= $elt->cdata_string;      }
          elsif( (exists $elt->{'target'}))      { $string .= $elt->pi_string;         }
          elsif( (exists $elt->{'comment'})) { $string .= $elt->comment_string;    }
          elsif( (exists $elt->{'ent'}))     { $string .= $elt->ent_string;        }
        }

      return $string;
    }

  # just a shortcut to $elt->sprint( 1)
  sub xml_string
    { $_[0]->sprint( 1); }

  sub pcdata_xml_string 
    { my $elt= shift;
      if( defined( my $string= $elt->{pcdata}) )
        { 
          if( $elt->{extra_data_in_pcdata})
            { _gen_mark( $string); # used by _(un)?protect_extra_data
              foreach my $data (reverse @{$elt->{extra_data_in_pcdata}})
                { my $substr= substr( $string, $data->{offset});
                  if( $keep_encoding || $elt->{asis})
                    { substr( $string, $data->{offset}, 0, $data->{text}); }
                  else
                    { substr( $string, $data->{offset}, 0, _protect_extra_data( $data->{text})); }
                }
              unless( $keep_encoding || $elt->{asis})
                { $string=~ s/([&<])/$XML::Twig::base_ent{$1}/g ;
 								  $string=~ s{\Q]]>}{]]&gt;}g;
                  _unprotect_extra_data( $string);
                }
            }
          else
            { $string=~ s/([&<])/$XML::Twig::base_ent{$1}/g unless( $keep_encoding || $elt->{asis});  
 							$string=~ s{\Q]]>}{]]&gt;}g;
 						}
          return $output_text_filter ? $output_text_filter->( $string) : $string;
        }
      else
        { return ''; }
    }

  { my $mark;
    my( %char2ent, %ent2char);
    BEGIN
      { %char2ent= ( '<' => 'lt', '&' => 'amp');
        %ent2char= ( 'lt' => '<', 'amp' => '&');
      }

    # generate a unique mark (a string) not found in the string, 
    # used to mark < and & in the extra data
    sub _gen_mark
      { $mark="AAAA";
        $mark++ while( index( $_[0], $mark) > -1);
        return $mark;
      }
      
    sub _protect_extra_data
      { my( $extra_data)= @_;
        $extra_data=~ s{([&<])}{:$mark:$char2ent{$1}:}g;
        return $extra_data;
      }

    sub _unprotect_extra_data
      { $_[0]=~ s{:$mark:(\w+):}{$ent2char{$1}}g; }

  } 
  
  sub cdata_string
    { my $cdata= $_[0]->{cdata};
      unless( defined $cdata) { return ''; }
      if( $remove_cdata)
        { $cdata=~ s/([&<])/$XML::Twig::base_ent{$1}/g; }
      else
        { $cdata= CDATA_START . $cdata . CDATA_END; }
      return $cdata;
   }

  sub att_xml_string 
    { my $elt= shift;
      my $att= shift;
      my $quote= shift || '"';
      if( defined (my $string= $elt->{att}->{$att}))
        { unless( $keep_encoding)
            { if( $do_not_escape_amp_in_atts)
                { $string=~ s{([$quote<])}{$XML::Twig::base_ent{$1}}g; 
                  $string=~ s{&(?!(\w+|#\d+|[xX][0-9a-fA-F]+);)}{&amp;}g; # dodgy: escape & that do not start an entity
                }
              else
                { $string=~ s{([$quote<&])}{$XML::Twig::base_ent{$1}}g; 
                  $string=~ s{\Q]]>}{]]&gt;}g;
                }
            }
          return $output_text_filter ? $output_text_filter->( $string) : $string;
        }
      else
        { return ''; }
    }

  sub ent_string 
    { my $ent= shift;
      my $ent_text= $ent->{ent};
      my( $t, $el, $ent_string);
      if(    $expand_external_entities
          && ($t= $ent->twig) 
          && ($el= $t->entity_list)
          && ($ent_string= $el->{entities}->{$ent->ent_name}->{val})
        )
       { return $ent_string; }
  
       return $ent_text; 
    }

  # returns just the text, no tags, for an element
  sub text
    { my $elt= shift;
      my $string;
  
      if( (exists $elt->{'pcdata'}))     { return  $elt->{pcdata};   }
      elsif( (exists $elt->{'cdata'}))   { return  $elt->{cdata};    }
      elsif( (exists $elt->{'target'}))      { return  $elt->pi_string;}
      elsif( (exists $elt->{'comment'})) { return  $elt->{comment};  }
      elsif( (exists $elt->{'ent'}))     { return  $elt->{ent} ;     }
  
      my $child= $elt->{first_child} ||'';
      while( $child)
        { my $child_text= $child->text;
          $string.= defined( $child_text) ? $child_text : '';
          $child= $child->{next_sibling};
        }
      unless( defined $string) { $string=''; }
  
      return $output_text_filter ? $output_text_filter->( $string) : $string;
    }

  sub trimmed_text
    { my $elt= shift;
      my $text= $elt->text;
      $text=~ s{\s+}{ }sg;
      $text=~ s{^\s*}{};
      $text=~ s{\s*$}{};
      return $text;
    }

  sub trim
    { my( $elt)= @_;
      my $pcdata= $elt->first_descendant( '#TEXT');
      (my $pcdata_text= $pcdata->text)=~ s{^\s+}{}s;
      $pcdata->set_text( $pcdata_text);
      $pcdata= $elt->last_descendant( '#TEXT');
      ($pcdata_text= $pcdata->text)=~ s{\s+$}{};
      $pcdata->set_text( $pcdata_text);
      foreach $pcdata ($elt->descendants( '#TEXT'))
        { ($pcdata_text= $pcdata->text)=~ s{\s+}{ }g;
          $pcdata->set_text( $pcdata_text);
        }
      return $elt;
    }
  

  # remove cdata sections (turns them into regular pcdata) in an element 
  sub remove_cdata 
    { my $elt= shift;
      foreach my $cdata ($elt->descendants_or_self( CDATA))
        { if( $keep_encoding)
            { my $data= $cdata->{cdata};
              $data=~ s{([&<"'])}{$XML::Twig::base_ent{$1}}g;
              $cdata->{pcdata}=  $data;
            }
          else
            { $cdata->{pcdata}=  $cdata->{cdata}; }
          $cdata->set_gi( PCDATA);
          undef $cdata->{cdata};
        }
    }

sub _is_private      { return _is_private_name( $_[0]->gi); }
sub _is_private_name { return $_[0]=~ m{^#(?!default:)};                }


} # end of block containing package globals ($pretty_print, $quotes, keep_encoding...)


# SAX export methods
sub toSAX1
  { _toSAX(@_, \&_start_tag_data_SAX1, \&_end_tag_data_SAX1); }

sub toSAX2
  { _toSAX(@_, \&_start_tag_data_SAX2, \&_end_tag_data_SAX2); }

sub _toSAX
  { my( $elt, $handler, $start_tag_data, $end_tag_data)= @_;
    if( $elt->{gi} >= $XML::Twig::SPECIAL_GI)
      { my $data= $start_tag_data->( $elt);
        _start_prefix_mapping( $elt, $handler, $data);
        if( $data && (my $start_element = $handler->can( 'start_element')))
          { $start_element->( $handler, $data) unless( $elt->_flushed); }
      
        foreach my $child ($elt->children)
          { $child->_toSAX( $handler, $start_tag_data, $end_tag_data); }

        if( (my $data= $end_tag_data->( $elt)) && (my $end_element = $handler->can( 'end_element')) )
          { $end_element->( $handler, $data); }
        _end_prefix_mapping( $elt, $handler);
      }
    else # text or special element
      { if( (exists $elt->{'pcdata'}) && (my $characters= $handler->can( 'characters')))
          { $characters->( $handler, { Data => $elt->{pcdata} });  }
        elsif( (exists $elt->{'cdata'}))  
          { if( my $start_cdata= $handler->can( 'start_cdata'))
              { $start_cdata->( $handler); }
            if( my $characters= $handler->can( 'characters'))
              { $characters->( $handler, {Data => $elt->{cdata} });  }
            if( my $end_cdata= $handler->can( 'end_cdata'))
              { $end_cdata->( $handler); }
          }
        elsif( ((exists $elt->{'target'}))  && (my $pi= $handler->can( 'processing_instruction')))
          { $pi->( $handler, { Target =>$elt->{target}, Data => $elt->{data} });  }
        elsif( ((exists $elt->{'comment'}))  && (my $comment= $handler->can( 'comment')))
          { $comment->( $handler, { Data => $elt->{comment} });  }
        elsif( ((exists $elt->{'ent'})))
          { 
            if( my $se=   $handler->can( 'skipped_entity'))
              { $se->( $handler, { Name => $elt->ent_name });  }
            elsif( my $characters= $handler->can( 'characters'))
              { if( defined $elt->ent_string)
                  { $characters->( $handler, {Data => $elt->ent_string});  }
                else
                  { $characters->( $handler, {Data => $elt->ent_name});  }
              }
          }
      
      }
  }
  
sub _start_tag_data_SAX1
  { my( $elt)= @_;
    my $name= $XML::Twig::index2gi[$elt->{'gi'}];
    return if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') ));
    my $attributes={};
    my $atts= $elt->atts;
    while( my( $att, $value)= each %$atts)
      { $attributes->{$att}= $value unless( ( (substr( $att, 0, 1) eq '#') && (substr( $att, 0, 9) ne '#default:') )); }
    my $data= { Name => $name, Attributes => $attributes};
    return $data;
  }

sub _end_tag_data_SAX1
  { my( $elt)= @_;
    return if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') ));
    return  { Name => $XML::Twig::index2gi[$elt->{'gi'}] };
  } 
  
sub _start_tag_data_SAX2
  { my( $elt)= @_;
    my $data={};
    
    my $name= $XML::Twig::index2gi[$elt->{'gi'}];
    return if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') ));
    $data->{Name}         = $name;
    $data->{Prefix}       = $elt->ns_prefix; 
    $data->{LocalName}    = $elt->local_name;
    $data->{NamespaceURI} = $elt->namespace;

    # save a copy of the data so we can re-use it for the end tag
    my %sax2_data= %$data;
    $elt->{twig_elt_SAX2_data}= \%sax2_data;
   
    # add the attributes
    $data->{Attributes}= $elt->_atts_to_SAX2;

    return $data;
  }

sub _atts_to_SAX2
  { my $elt= shift;
    my $SAX2_atts= {};
    foreach my $att (keys %{$elt->atts})
      { 
        next if( ( (substr( $att, 0, 1) eq '#') && (substr( $att, 0, 9) ne '#default:') ));
        my $SAX2_att={};
        $SAX2_att->{Name}         = $att;
        $SAX2_att->{Prefix}       = _ns_prefix( $att); 
        $SAX2_att->{LocalName}    = _local_name( $att);
        $SAX2_att->{NamespaceURI} = $elt->namespace( $SAX2_att->{Prefix});
        $SAX2_att->{Value}        = $elt->{'att'}->{$att};
        my $SAX2_att_name= "{$SAX2_att->{NamespaceURI}}$SAX2_att->{LocalName}";

        $SAX2_atts->{$SAX2_att_name}= $SAX2_att;
      }
    return $SAX2_atts;
  }

sub _start_prefix_mapping
  { my( $elt, $handler, $data)= @_;
    if( my $start_prefix_mapping= $handler->can( 'start_prefix_mapping')
        and my @new_prefix_mappings= grep { /^\{[^}]*\}xmlns/ || /^\{$XMLNS_URI\}/ } keys %{$data->{Attributes}}
      )
      { foreach my $prefix (@new_prefix_mappings)
          { my $prefix_string= $data->{Attributes}->{$prefix}->{LocalName};
            if( $prefix_string eq 'xmlns') { $prefix_string=''; }
            my $prefix_data=
              {  Prefix       => $prefix_string,
                 NamespaceURI => $data->{Attributes}->{$prefix}->{Value}
              };
            $start_prefix_mapping->( $handler, $prefix_data);
            $elt->{twig_end_prefix_mapping} ||= [];
            push @{$elt->{twig_end_prefix_mapping}}, $prefix_string;
          }
      }
  }

sub _end_prefix_mapping
  { my( $elt, $handler)= @_;
    if( my $end_prefix_mapping= $handler->can( 'end_prefix_mapping'))
      { foreach my $prefix (@{$elt->{twig_end_prefix_mapping}})
          { $end_prefix_mapping->( $handler, { Prefix => $prefix} ); }
      }
  }
             
sub _end_tag_data_SAX2
  { my( $elt)= @_;
    return if( ( (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 1) eq '#') && (substr( $XML::Twig::index2gi[$elt->{'gi'}], 0, 9) ne '#default:') ));
    return $elt->{twig_elt_SAX2_data};
  } 



#start-extract twig_node
sub contains_text
  { my $elt= shift;
    my $child= $elt->{first_child};
    while ($child)
      { return 1 if( $child->is_text || (exists $child->{'ent'})); 
        $child= $child->{next_sibling};
      }
    return 0;
  }

#end-extract twig_node

# creates a single pcdata element containing the text as child of the element
# options: 
#   - force_pcdata: when set to a true value forces the text to be in a#PCDATA
#                   even if the original element was a #CDATA
sub set_text
  { my( $elt, $string, %option)= @_;

    if( $XML::Twig::index2gi[$elt->{'gi'}] eq PCDATA) 
      { return $elt->{pcdata}=  $string; }
    elsif( $XML::Twig::index2gi[$elt->{'gi'}] eq CDATA)  
      { if( $option{force_pcdata})
          { $elt->set_gi( PCDATA);
            $elt->{cdata}= '';
            return $elt->{pcdata}=  $string;
      }
    else
      { return $elt->{cdata}=  $string; }
      }

    foreach my $child (@{[$elt->children]})
      { $child->delete; }

    my $pcdata= XML::Twig::Elt->new( PCDATA, $string);
    $pcdata->paste( $elt);

    delete $elt->{empty};

    return $elt;
  }

# set the content of an element from a list of strings and elements
sub set_content
  { my $elt= shift;

    return $elt unless defined $_[0];

    # attributes can be given as a hash (passed by ref)
    if( ref $_[0] eq 'HASH')
      { my $atts= shift;
        $elt->del_atts; # usually useless but better safe than sorry
        $elt->set_atts( $atts);
        return  $elt unless defined $_[0];
      }

    # check next argument for #EMPTY
    if( !(ref $_[0]) && ($_[0] eq EMPTY) ) 
      { $elt->{empty}= 1; return $elt; }

    # case where we really want to do a set_text, the element is '#PCDATA'
    # and we only want to add text in it
    if( ($XML::Twig::index2gi[$elt->{'gi'}] eq PCDATA) && ($#_ == 0) && !( ref $_[0]))
      { $elt->{pcdata}=  $_[0];
        return $elt;
      }
    elsif( ($XML::Twig::index2gi[$elt->{'gi'}] eq CDATA) && ($#_ == 0) && !( ref $_[0]))
      { $elt->{cdata}=  $_[0];
        return $elt;
      }

    # delete the children
    # WARNING: potential problem here if the children are used
    # somewhere else (where?). Will be solved when I use weak refs
    foreach my $child (@{[$elt->children]})
      { $child->delete; }

    foreach my $child (@_)
      { if( isa( $child, 'XML::Twig::Elt'))
          { # argument is an element
            $child->paste( 'last_child', $elt);
          }
        else
          { # argument is a string
            if( (my $pcdata= $elt->{last_child}) && $elt->{last_child}->is_pcdata)
              { # previous child is also pcdata: just concatenate
                $pcdata->{pcdata}=  $pcdata->{pcdata} . $child 
              }
            else
              { # previous child is not a string: creat a new pcdata element
                $pcdata= XML::Twig::Elt->new( PCDATA, $child);
                $pcdata->paste( 'last_child', $elt);  
              }
          }
      }

    delete $elt->{empty};

    return $elt;
  }

# inserts an element (whose gi is given) as child of the element
# all children of the element are now children of the new element
# returns the new element
sub insert
  { my ($elt, @args)= @_;
    # first cut the children
    my @children= $elt->children;
    foreach my $child (@children)
      { $child->cut; }
    # insert elements
    while( my $gi= shift @args)
      { my $new_elt= XML::Twig::Elt->new( $gi);
        # add attributes if needed
        if( defined( $args[0]) && ( isa( $args[0], 'HASH')) )
          { $new_elt->set_atts( shift @args); }
        # paste the element
        $new_elt->paste( $elt);
        delete $elt->{empty};
        $elt= $new_elt;
      }
    # paste back the children
    foreach my $child (@children)
      { $child->paste( 'last_child', $elt); }
    return $elt;
  }

# insert a new element 
# $elt->insert_new_element( $opt_position, $gi, $opt_atts_hash, @opt_content); 
# the element is created with the same syntax as new
# position is the same as in paste, first_child by default
sub insert_new_elt
  { my $elt= shift;
    my $position= $_[0];
    if(     ($position eq 'before') || ($position eq 'after')
         || ($position eq 'first_child') || ($position eq 'last_child'))
      { shift; }
    else
      { $position= 'first_child'; }

    my $new_elt= $elt->new( @_);
    $new_elt->paste( $position, $elt);

    #if( defined $new_elt->{'att'}->{$ID}) { $new_elt->set_id( $new_elt->{'att'}->{$ID}); }
    
    return $new_elt;
  }

# wraps an element in elements which gi's are given as arguments
# $elt->wrap_in( 'td', 'tr', 'table') wraps the element as a single
# cell in a table for example
# returns the new element
sub wrap_in
  { my $elt= shift;
    while( my $gi = shift @_)
      { my $new_elt = XML::Twig::Elt->new( $gi);
        if( $elt->{twig_current})
          { my $t= $elt->twig;
            $t->{twig_current}= $new_elt;
            delete $elt->{'twig_current'};
            $new_elt->{'twig_current'}=1;
          }

        if( my $parent= $elt->{parent})
          { $new_elt->set_parent( $parent); 
            $parent->{first_child}=  $new_elt if( $parent->{first_child} == $elt);
            $parent->set_last_child( $new_elt)  if( $parent->{last_child} == $elt);
          }
        else
          { # wrapping the root
            my $twig= $elt->twig;
            if( $twig && $twig->root && ($twig->root eq $elt) )
              { $twig->{twig_root}= $new_elt; }
          }

        if( my $prev_sibling= $elt->{prev_sibling})
          { $new_elt->set_prev_sibling( $prev_sibling);
            $prev_sibling->{next_sibling}=  $new_elt;
          }

        if( my $next_sibling= $elt->{next_sibling})
          { $new_elt->{next_sibling}=  $next_sibling;
            $next_sibling->set_prev_sibling( $new_elt);
          }
        $new_elt->{first_child}=  $elt;
        $new_elt->set_last_child( $elt);

        $elt->set_parent( $new_elt);
        $elt->set_prev_sibling( undef);
        $elt->{next_sibling}=  undef;

        # add the attributes if the next argument is a hash ref
        if( defined( $_[0]) && (isa( $_[0], 'HASH')) )
          { $new_elt->set_atts( shift @_); }

        $elt= $new_elt;
      }
      
    return $elt;
  }

sub replace
  { my( $elt, $ref)= @_;
    if( my $parent= $ref->{parent})
      { $elt->set_parent( $parent);
        $parent->{first_child}=  $elt if( $parent->{first_child} == $ref);
        $parent->set_last_child( $elt)  if( $parent->{last_child} == $ref);
      }
    if( my $prev_sibling= $ref->{prev_sibling})
      { $elt->set_prev_sibling( $prev_sibling);
        $prev_sibling->{next_sibling}=  $elt;
      }
    if( my $next_sibling= $ref->{next_sibling})
      { $elt->{next_sibling}=  $next_sibling;
        $next_sibling->set_prev_sibling( $elt);
      }
   
    $ref->set_parent( undef);
    $ref->set_prev_sibling( undef);
    $ref->{next_sibling}=  undef;
    return $ref;
  }

sub replace_with
  { my $ref= shift;
    my $elt= shift;
    $elt->replace( $ref);
    foreach my $new_elt (reverse @_)
      { $new_elt->paste( after => $elt); }
    return $elt;
  }


#start-extract twig_node
# move an element, same syntax as paste, except the element is first cut
sub move
  { my $elt= shift;
    $elt->cut;
    $elt->paste( @_);
    return $elt;
  }
#end-extract twig_node


# adds a prefix to an element, creating a pcdata child if needed
sub prefix
  { my ($elt, $prefix, $option)= @_;
    my $asis= ($option && ($option eq 'asis')) ? 1 : 0;
    if( (exists $elt->{'pcdata'}) 
        && (($asis && $elt->{asis}) || (!$asis && ! $elt->{asis}))
      )
      { $elt->{pcdata}=  $prefix . $elt->{pcdata}; }
    elsif( $elt->{first_child} && $elt->{first_child}->is_pcdata
        && (   ($asis && $elt->{first_child}->{asis}) 
            || (!$asis && ! $elt->{first_child}->{asis}))
         )
      { $elt->{first_child}->set_pcdata( $prefix . $elt->{first_child}->pcdata); }
    else
      { my $new_elt= XML::Twig::Elt->new( PCDATA, $prefix);
        $new_elt->paste( $elt);
        $new_elt->set_asis if( $asis);
      }
    return $elt;
  }

# adds a suffix to an element, creating a pcdata child if needed
sub suffix
  { my ($elt, $suffix, $option)= @_;
    my $asis= ($option && ($option eq 'asis')) ? 1 : 0;
    if( (exists $elt->{'pcdata'})
        && (($asis && $elt->{asis}) || (!$asis && ! $elt->{asis}))
      )
      { $elt->{pcdata}=  $elt->{pcdata} . $suffix; }
    elsif( $elt->{last_child} && $elt->{last_child}->is_pcdata
        && (   ($asis && $elt->{last_child}->{asis}) 
            || (!$asis && ! $elt->{last_child}->{asis}))
         )
      { $elt->{last_child}->set_pcdata( $elt->{last_child}->pcdata . $suffix); }
    else
      { my $new_elt= XML::Twig::Elt->new( PCDATA, $suffix);
        $new_elt->paste( 'last_child', $elt);
        $new_elt->set_asis if( $asis);
      }
    return $elt;
  }

#start-extract twig_node
# create a path to an element ('/root/.../gi)
sub path
  { my $elt= shift;
    my @context= ( $elt, $elt->ancestors);
    return "/" . join( "/", reverse map {$_->gi} @context);
  }

sub xpath
  { my $elt= shift;
    my $xpath;
    foreach my $ancestor (reverse $elt->ancestors_or_self)
      { my $gi= $XML::Twig::index2gi[$ancestor->{'gi'}];
        $xpath.= "/$gi";
        my $index= $ancestor->prev_siblings( $gi) + 1;
        unless( ($index == 1) && !$ancestor->next_sibling( $gi))
          { $xpath.= "[$index]"; }
      }
    return $xpath;
  }

# methods used mainly by wrap_children

# return a string with the 
# for an element <foo><elt att="val">...</elt><elt2/><elt>...</elt></foo>
# returns '<elt att="val"><elt2><elt>'
sub _stringify_struct
  { my( $elt, %opt)= @_;
    my $string='';
    my $pretty_print= XML::Twig::Elt::set_pretty_print( 'none');
    foreach my $child ($elt->children)
      { $child->add_id; $string .= $child->start_tag ||''; }
    XML::Twig::Elt::set_pretty_print( $pretty_print);
    return $string;
  }

# wrap a series of elements in a new one
sub _wrap_range
  { my $elt= shift;
    my $gi= shift;
    my $atts= isa( $_[0], 'HASH') ? shift : undef;
    my $range= shift; # the string with the tags to wrap

    my $t= $elt->twig;

    # get the tags to wrap
    my @to_wrap;
    while( $range=~ m{<\w+\s+[^>]*id=("[^"]*"|'[^']*')[^>]*>}g)
      { push @to_wrap, $t->elt_id( substr( $1, 1, -1)); }

    return '' unless @to_wrap;
    
    my $to_wrap= shift @to_wrap;
    my %atts= %$atts;
    my $new_elt= $to_wrap->wrap_in( $gi, \%atts);
    $_->move( last_child => $new_elt) foreach (@to_wrap);

    return '';
  }
    
# wrap children matching a regexp in a new element
sub wrap_children
  { my( $elt, $regexp, $gi, $atts)= @_;

    $atts ||={};

    my $elt_as_string= $elt->_stringify_struct; # stringify the elt structure
    $regexp=~ s{(<[^>]*>)}{_match_expr( $1)}eg; # in the regexp, replace gi's by the proper regexp 
    $elt_as_string=~ s{($regexp)}{$elt->_wrap_range( $gi, $atts, $1)}eg; # then do the actual replace
  
    return $elt; 
  }

sub _match_expr
  { my $tag= shift;
    my( $gi, %atts)= XML::Twig::_parse_start_tag( $tag);
    return _match_tag( $gi, %atts);
  }


sub _match_tag
  { my( $elt, %atts)= @_;
    my $string= "<$elt\\b";
    foreach my $key (sort keys %atts)
      { my $val= $atts{$key};
        $val= "\Q$val\E";
       # my $val= qq{\Q$atts{$key}\E};
        $string.= qq{[^>]*$key=(?:"$val"|'$val')};
      }
    $string.=  qq{[^>]*>};
    return "(?:$string)";
  }

sub field_to_att
  { my( $elt, $cond, $att)= @_;
    $att ||= $cond;
    my $child= $elt->first_child( $cond) or return undef;
    $elt->set_att( $att => $child->text);
    $child->cut;
    return $elt;
  }

sub att_to_field
  { my( $elt, $att, $tag)= @_;
    $tag ||= $att;
    my $child= $elt->insert_new_elt( first_child => $tag, $elt->{'att'}->{$att});
    $elt->del_att( $att);
    return $elt;
  }

# sort children methods

sub sort_children_on_field
  { my $elt   = shift;
    my $field = shift;
    my $get_key= sub { return $_[0]->field( $field) };
    return $elt->sort_children( $get_key, @_); 
  }

sub sort_children_on_att
  { my $elt = shift;
    my $att = shift;
    my $get_key= sub { return $_[0]->{'att'}->{$att} };
    return $elt->sort_children( $get_key, @_); 
  }

sub sort_children_on_value
  { my $elt   = shift;
    #my $get_key= eval qq{ sub { return \$_[0]->text } };
    my $get_key= \&text;
    return $elt->sort_children( $get_key, @_); 
  }


sub sort_children
  { my( $elt, $get_key, %opt)=@_;
    $opt{order} ||= 'normal';
    $opt{type}  ||= 'alpha';
    my( $par_a, $par_b)= ($opt{order} eq 'reverse') ? qw( b a) : qw ( a b) ;
    my $op= ($opt{type} eq 'numeric') ? '<=>' : 'cmp' ;
    my @children= $elt->cut_children;
    if( $opt{type} eq 'numeric')
      {  @children= map  { $_->[1] }
                    sort { $a->[0] <=> $b->[0] }
                    map  { [ $get_key->( $_), $_] } @children;
      }
    elsif( $opt{type} eq 'alpha')
      {  @children= map  { $_->[1] }
                    sort { $a->[0] cmp $b->[0] }
                    map  { [ $get_key->( $_), $_] } @children;
      }
    else
      { croak "wrong sort type '$opt{type}', should be either 'alpha' or 'numeric'"; }

    @children= reverse @children if( $opt{order} eq 'reverse');
    $elt->set_content( @children);
  }


# comparison methods

sub before
  { my( $a, $b)=@_;
    if( $a->cmp( $b) == -1) { return 1; } else { return 0; }
  }

sub after
  { my( $a, $b)=@_;
    if( $a->cmp( $b) == 1) { return 1; } else { return 0; }
  }

sub lt
  { my( $a, $b)=@_;
    return 1 if( $a->cmp( $b) == -1);
    return 0;
  }

sub le
  { my( $a, $b)=@_;
    return 1 unless( $a->cmp( $b) == 1);
    return 0;
  }

sub gt
  { my( $a, $b)=@_;
    return 1 if( $a->cmp( $b) == 1);
    return 0;
  }

sub ge
  { my( $a, $b)=@_;
    return 1 unless( $a->cmp( $b) == -1);
    return 0;
  }


sub cmp
  { my( $a, $b)=@_;

    # easy cases
    return  0 if( $a == $b);    
    return  1 if( $a->in($b)); # a starts after b 
    return -1 if( $b->in($a)); # a starts before b

    # ancestors does not include the element itself
    my @a_pile= ($a, $a->ancestors); 
    my @b_pile= ($b, $b->ancestors);

    # the 2 elements are not in the same twig
    return undef unless( $a_pile[-1] == $b_pile[-1]);

    # find the first non common ancestors (they are siblings)
    my $a_anc= pop @a_pile;
    my $b_anc= pop @b_pile;

    while( $a_anc == $b_anc) 
      { $a_anc= pop @a_pile;
        $b_anc= pop @b_pile;
      }

    # from there move left and right and figure out the order
    my( $a_prev, $a_next, $b_prev, $b_next)= ($a_anc, $a_anc, $b_anc, $b_anc);
    while()
      { $a_prev= $a_prev->{prev_sibling} || return( -1);
        return 1 if( $a_prev == $b_next);
        $a_next= $a_next->{next_sibling} || return( 1);
        return -1 if( $a_next == $b_prev);
        $b_prev= $b_prev->{prev_sibling} || return( 1);
        return -1 if( $b_prev == $a_next);
        $b_next= $b_next->{next_sibling} || return( -1);
        return 1 if( $b_next == $a_prev);
      }
  }
    
#end-extract twig_node

1;

__END__

=head1 NAME

XML::Twig - A perl module for processing huge XML documents in tree mode.

=head1 SYNOPSIS

Note that this documentation is intended as a reference to the module.

Complete docs, including a tutorial, examples, an easier to use HTML version,
a quick reference card and a FAQ are available at http://www.xmltwig.com/xmltwig

Small documents (loaded in memory as a tree):

  my $twig=XML::Twig->new();    # create the twig
  $twig->parsefile( 'doc.xml'); # build it
  my_process( $twig);           # use twig methods to process it 
  $twig->print;                 # output the twig

Huge documents (processed in combined stream/tree mode):

  # at most one div will be loaded in memory
  my $twig=XML::Twig->new(   
    twig_handlers => 
      { title   => sub { $_->set_tag( 'h2') }, # change title tags to h2
        para    => sub { $_->set_tag( 'p')  }, # change para to p
        hidden  => sub { $_->delete;       },  # remove hidden elements
        list    => \&my_list_process,          # process list elements
        div     => sub { $_[0]->flush;     },  # output and free memory
      },
    pretty_print => 'indented',                # output will be nicely formatted
    empty_tags   => 'html',                    # outputs <empty_tag />
                         );
    $twig->flush;                              # flush the end of the document

See L<XML::Twig 101|XML::Twig 101> for other ways to use the module, as a 
filter for example


=head1 DESCRIPTION

This module provides a way to process XML documents. It is build on top
of C<XML::Parser>.

The module offers a tree interface to the document, while allowing you
to output the parts of it that have been completely processed.

It allows minimal resource (CPU and memory) usage by building the tree
only for the parts of the documents that need actual processing, through the 
use of the C<L<twig_roots|twig_roots> > and 
C<L<twig_print_outside_roots|twig_print_outside_roots> > options. The 
C<L<finish|finish> > and C<L<finish_print|finish_print> > methods also help 
to increase performances.

XML::Twig tries to make simple things easy so it tries its best to takes care 
of a lot of the (usually) annoying (but sometimes necessary) features that 
come with XML and XML::Parser.

=head1 XML::Twig 101

XML::Twig can be used either on "small" XML documents (that fit in memory)
or on huge ones, by processing parts of the document and outputting or
discarding them once they are processed.


=head2 Loading an XML document and processing it

  my $t= XML::Twig->new();
  $t->parse( '<d><title>title</title><para>p 1</para><para>p 2</para></d>');
  my $root= $t->root;
  $root->set_tag( 'html');              # change doc to html
  $title= $root->first_child( 'title'); # get the title
  $title->set_tag( 'h1');               # turn it into h1
  my @para= $root->children( 'para');   # get the para children
  foreach my $para (@para)
    { $para->set_tag( 'p'); }           # turn them into p
  $t->print;                            # output the document

Other useful methods include:

L<att|att>: C<< $elt->{'att'}->{'foo'} >> return the C<foo> attribute for an 
element,

L<set_att|set_att> : C<< $elt->set_att( foo => "bar") >> sets the C<foo> 
attribute to the C<bar> value,

L<next_sibling|next_sibling>: C<< $elt->{next_sibling} >> return the next sibling
in the document (in the example C<< $title->{next_sibling} >> is the first
C<para>, you can also (and actually should) use 
C<< $elt->next_sibling( 'para') >> to get it 

The document can also be transformed through the use of the L<cut|cut>, 
L<copy|copy>, L<paste|paste> and L<move|move> methods: 
C<< $title->cut; $title->paste( after => $p); >> for example

And much, much more, see L<Elt|"Elt">.

=head2 Processing an XML document chunk by chunk

One of the strengths of XML::Twig is that it let you work with files that do 
not fit in memory (BTW storing an XML document in memory as a tree is quite
memory-expensive, the expansion factor being often around 10).

To do this you can define handlers, that will be called once a specific 
element has been completely parsed. In these handlers you can access the
element and process it as you see fit, using the navigation and the
cut-n-paste methods, plus lots of convenient ones like C<L<prefix|prefix> >.
Once the element is completely processed you can then C<L<flush|flush> > it, 
which will output it and free the memory. You can also C<L<purge|purge> > it 
if you don't need to output it (if you are just extracting some data from 
the document for example). The handler will be called again once the next 
relevant element has been parsed.

  my $t= XML::Twig->new( twig_handlers => 
                          { section => \&section,
                            para   => sub { $_->set_tag( 'p');
                          },
                       );
  $t->parsefile( 'doc.xml');
  $t->flush; # don't forget to flush one last time in the end or anything
             # after the last </section> tag will not be output 
    
  # the handler is called once a section is completely parsed, ie when 
  # the end tag for section is found, it receives the twig itself and
  # the element (including all its sub-elements) as arguments
  sub section 
    { my( $t, $section)= @_;      # arguments for all twig_handlers
      $section->set_tag( 'div');  # change the tag name.4, my favourite method...
      # let's use the attribute nb as a prefix to the title
      my $title= $section->first_child( 'title'); # find the title
      my $nb= $title->{'att'}->{'nb'}; # get the attribute
      $title->prefix( "$nb - ");  # easy isn't it?
      $section->flush;            # outputs the section and frees memory
    }

        
There is of course more to it: you can trigger handlers on more elaborate 
conditions than just the name of the element, C<section/title> for example.

  my $t= XML::Twig->new( twig_handlers => 
                           { 'section/title' => sub { $_->print } }
                       )
                  ->parsefile( 'doc.xml');

Here C<< sub { $_->print } >> simply prints the current element (C<$_> is aliased
to the element in the handler).

You can also trigger a handler on a test on an attribute:

  my $t= XML::Twig->new( twig_handlers => 
                      { 'section[@level="1"]' => sub { $_->print } }
                       );
                  ->parsefile( 'doc.xml');

You can also use C<L<start_tag_handlers|start_tag_handlers> > to process an 
element as soon as the start tag is found. Besides C<L<prefix|prefix> > you
can also use C<L<suffix|suffix> >, 

=head2 Processing just parts of an XML document

The twig_roots mode builds only the required sub-trees from the document
Anything outside of the twig roots will just be ignored:

  my $t= XML::Twig->new( 
       # the twig will include just the root and selected titles 
           twig_roots   => { 'section/title' => \&print_n_purge,
                             'annex/title'   => \&print_n_purge
           }
                      );
  $t->parsefile( 'doc.xml');
  
  sub print_n_purge 
    { my( $t, $elt)= @_;
      print $elt->text;    # print the text (including sub-element texts)
      $t->purge;           # frees the memory
    }

You can use that mode when you want to process parts of a documents but are
not interested in the rest and you don't want to pay the price, either in
time or memory, to build the tree for the it.


=head2 Building an XML filter

You can combine the C<twig_roots> and the C<twig_print_outside_roots> options to 
build filters, which let you modify selected elements and will output the rest 
of the document as is.

This would convert prices in $ to prices in Euro in a document:

  my $t= XML::Twig->new( 
           twig_roots   => { 'price' => \&convert, },   # process prices 
           twig_print_outside_roots => 1,               # print the rest
                      );
  $t->parsefile( 'doc.xml');
 
  sub convert 
    { my( $t, $price)= @_;
      my $currency=  $price->{'att'}->{'currency'};          # get the currency
      if( $currency eq 'USD')
        { $usd_price= $price->text;                     # get the price
          # %rate is just a conversion table 
          my $euro_price= $usd_price * $rate{usd2euro};
          $price->set_text( $euro_price);               # set the new price
          $price->set_att( currency => 'EUR');          # don't forget this!
        }
      $price->print;                                    # output the price
    }

=head2 XML::Twig and various versions of Perl, XML::Parser and expat:

Before being uploaded to CPAN, XML::Twig 3.16 has been tested under the 
following environments:

=over 4

=item linux-x86

perl 5.6.2 to 5.9.1, expat 1.95.2 to 1.95.7, XML::Parser 2.31, 2.33 and 2.34
perl 5.6.2, XML::Parser 2.27 (which comes with its own version of expat)

=item Mac OS X (10.2/10.3)

Mac OS X: same as linux-x86, plus perl 5.5.4

=item Solaris

perl 5.6.1, expat 1.95.2, XML::Parser 2.31

=item Windows 98

perl 5.6.1 (Activestate build 635), XML::Parser 2.27
perl 5.8.2 (Activestate build 808), XML::Parser 2.34

Note that with Windows 98 and Perl 5.6.1 C<nmake> may freeze while trying to copy
the tools (xml_grep, xml_print and xml_spellcheck), so you have to answer no 
when asked if you want to install them.

=back

See L<http://testers.cpan.org/search?request=dist&dist=XML-Twig> for the
CPAN testers reports on XML::Twig

XML::Twig does B<NOT> work with expat 1.95.4
XML::Twig only works with XML::Parser 2.27 in perl 5.6.*  
XML::Parser 2.28 does not really work

When in doubt, upgrade expat, XML::Parser and Scalar::Util

=head1 Simplifying XML processing

=over 4

=item Whitespaces

Whitespaces that look non-significant are discarded, this behaviour can be 
controlled using the C<L<keep_spaces|keep_spaces> >, 
C<L<keep_spaces_in|keep_spaces_in> > and 
C<L<discard_spaces_in|discard_spaces_in> > options.

=item Encoding

You can specify that you want the output in the same encoding as the input
(provided you have valid XML, which means you have to specify the encoding
either in the document or when you create the Twig object) using the 
C<L<keep_encoding|keep_encoding> > option

You can also use C<L<output_encoding>> to convert the internal UTF-8 format
to the required encoding.

=item Comments and Processing Instructions (PI)

Comments and PI's can be hidden from the processing, but still appear in the
output (they are carried by the "real" element closer to them)

=item Pretty Printing

XML::Twig can output the document pretty printed so it is easier to read for
us humans.

=item Surviving an untimely death

XML parsers are supposed to react violently when fed improper XML. 
XML::Parser just dies.

XML::Twig provides the C<L<safe_parse|safe_parse> > and the 
C<L<safe_parsefile|safe_parsefile> > methods which wrap the parse in an eval
and return either the parsed twig or 0 in case of failure.

=item Private attributes

Attributes with a name starting with # (illegal in XML) will not be
output, so you can safely use them to store temporary values during
processing. Note that you can store anything in a private attribute, 
not just text, it's just a regular Perl variable, so a reference to
an object or a huge data structure is perfectly fine.

=back

=head1 CLASSES

XML::Twig uses a very limited number of classes. The ones you are most likely to use
are C<L<XML::Twig>> of course, which represents a complete XML document, including the 
document itself (the root of the document itself is C<L<root>>), its handlers, its
input or output filters... The other main class is C<L<XML::Twig::Elt>>, which models 
an XML element. Element here has a very wide definition: it can be a regular element, or
but also text, with an element C<L<tag>> of C<#PCDATA> (or C<#CDATA>), an entity (tag is
C<#ENT>), a Processing Instruction (C<#PI>), a comment (C<#COMMENT>). 

Those are the 2 commonly used classes.

You might want to look the C<L<elt_class>> option if you want to subclass C<XML::Twig::Elt>.

Attributes are just attached to their parent element, they are not objects per se. (Please
use the provided methods C<L<att>> and C<L<set_att>> to access them, if you access them
as a hash, then your code becomes implementaion deppndant and might break in the future).

Other classes that are seldom used are C<L<XML::Twig::Entity_list>> and C<L<XML::Twig::Entity>>.

If you use C<L<XML::Twig::XPath>> instead of C<XML::Twig>, elements are then created as
C<L<XML::Twig::XPath::Elt>>


=head1 METHODS

=head2 XML::Twig 

A twig is a subclass of XML::Parser, so all XML::Parser methods can be
called on a twig object, including parse and parsefile.
C<setHandlers> on the other hand cannot be used, see C<L<BUGS|BUGS> >


=over 4

=item new 

This is a class method, the constructor for XML::Twig. Options are passed
as keyword value pairs. Recognized options are the same as XML::Parser,
plus some XML::Twig specifics.

New Options:

=over 4

=item twig_handlers

This argument replaces the corresponding XML::Parser argument. It consists
of a hash C<{ expression => \&handler}> where expression is a 
I<generic_attribute_condition>, I<string_condition>,
an I<attribute_condition>,I<full_path>, a I<partial_path>, a I<tag>,
a I<tag_regexp>, I<_default_> or I<_all_>.

The idea is to support a usefull but efficient (thus limited) subset of
XPATH. A fuller expression set will be supported in the future, as users
ask for more and as I manage to implement it efficiently. This will never
encompass all of XPATH due to the streaming nature of parsing (no lookahead
after the element end tag).

A B<generic_attribute_condition> is a condition on an attribute, in the form
C<*[@att="val"]> or C<*[@att]>, simple quotes can be used instead of double 
quotes and the leading '*' is actually optional. No matter what the tag of the
element is, the handler will be triggered either if the attribute has the 
specified value or if it just exists. 

A B<string_condition> is a condition on the content of an element, in the form
C<tag[string()="foo"]>, simple quotes can be used instead of double quotes, at 
the moment you cannot escape the quotes (this will be added as soon as I
dig out my copy of Mastering Regular Expressions from its storage box).
The text returned is, as per what I (and Matt Sergeant!) understood from
the XPATH spec the concatenation of all the text in the element, excluding
all markup. Thus to call a handler on the elementC<< <p>text <b>bold</b></p> >>
the appropriate condition is C<p[string()="text bold"]>. Note that this is not
exactly conformant to the XPATH spec, it just tries to mimic it while being
still quite concise. 

A extension of that notation is C<tag[string(B<child_tag>)="foo"]> where the
handler will be called if a child of a C<tag> element has a text value of 
C<foo>.  At the moment only direct children of the C<tag> element are checked.
If you need to test on descendants of the element let me know. The fix is
trivial but would slow down the checks, so I'd like to keep it the way it is.

A B<regexp_condition> is a condition on the content of an element, in the form
C<tag[string()=~ /foo/"]>. This is the same as a string condition except that
the text of the element is matched to the regexp. The C<i>, C<m>, C<s> and C<o>
modifiers can be used on the regexp.

The C<< tag[string(B<child_tag>)=~ /foo/"] >> extension is also supported.

An B<attribute_condition> is a simple condition of an attribute of the
current element in the form C<tag[@att="val"]> (simple quotes can be used
instead of double quotes, you can escape quotes either). 
If several attribute_condition are true the same element all the handlers
can be called in turn (in the order in which they were first defined).
If the C<="val"> part is ommited ( the condition is then C<tag[@att]>) then
the handler is triggered if the attribute actually exists for the element,
no matter what it's value is.

A B<full_path> looks like C<'/doc/section/chapter/title'>, it starts with
a / then lists all the tags to the element. The handler will be called if
the path to the current element (in the input document) is exactly as
defined by the C<full_path>.

A B<partial_path> is like a full_path except it does not start with a /:
C<'chapter/title'> for example. The handler will be called if the path to
the element (in the input document) ends as defined in the C<partial_path>.

B<WARNING>: (hopefully temporary) at the moment C<string_condition>, 
C<regexp_condition> and C<attribute_condition> are only supported on a 
simple tag, not on a path.

A B<tag_regexp> is a regular expression (created with C<qr//>), applied to 
the tag name. For example C<qr/^h\d$/i> would match C<h1>, C<H1>, C<h2>, 
C<H2>... 

A B<tag>.

#CDATA can be used to call a handler for a CDATA.

A special tag B<_all_> is used to call a function for each element.
The special tag B<_default_> is used to call a handler for each element
that does NOT have a specific handler.

The order of precedence to trigger a handler is: 
I<generic_attribute_condition>, I<string_condition>, I<regexp_condition>, 
I<attribute_condition>, I<full_path>, longer I<partial_path>, shorter 
I<partial_path>, I<tag_regexp>, I<tag>, I<_default_> . 

B<Important>: once a handler has been triggered if it returns 0 then no other
handler is called, exept a C<_all_> handler which will be called anyway.

If a handler returns a true value and other handlers apply, then the next
applicable handler will be called. Repeat, rince, lather..; The exception
to that rule is when the C<L<do_not_chain_handlers|do_not_chain_handlers>>
option is set, in which case only the first handler will be called.

Note that it might be a good idea to explicitely return a short true value
(like 1) from handlers: this ensures that other applicable handlers are 
called even if the last statement for the handler happens to evaluate to
false. This might also speedup the code by avoiding the result of the last 
statement of the code to be copied and passed to the code managing handlers.
It can really pay to have 1 instead of a long string returned.

When an element is CLOSED the corresponding handler is called, with 2
arguments: the twig and the C<L</Element|/Element> >. The twig includes the 
document tree that has been built so far, the element is the complete sub-tree
for the element. This means that handlers for inner elements are called before
handlers for outer elements.

C<$_> is also set to the element, so it is easy to write inline handlers like

  para => sub { $_->set_tag( 'p'); }

Text is stored in elements whose tag is #PCDATA (due to mixed content, text
and sub-element in an element there is no way to store the text as just an
attribute of the enclosing element).

B<Warning>: if you have used purge or flush on the twig the element might not
be complete, some of its children might have been entirely flushed or purged,
and the start tag might even have been printed (by C<flush>) already, so changing
its tag might not give the expected result.

More generally, the I<full_path>, I<partial_path>, I<tag> and I<tag_regexp> 
expressions are
evaluated against the input document. Which means that even if you have changed
the tag of an element (changing the tag of a parent element from a handler for
example) the change will not impact the expression evaluation. Attributes in
I<attribute_condition> are different though. As the initial value of attribute
is not stored the handler will be triggered if the B<current> attribute/value
pair is found when the element end tag is found. Although this can be quite
confusing it should not impact most of users, and allow others to play clever
tricks with temporary attributes. Let me know if this is a problem for you.


=item twig_roots

This argument let's you build the tree only for those elements you are
interested in. 

  Example: my $t= XML::Twig->new( twig_roots => { title => 1, subtitle => 1});
           $t->parsefile( file);
           my $t= XML::Twig->new( twig_roots => { 'section/title' => 1});
           $t->parsefile( file);


return a twig containing a document including only C<title> and C<subtitle> 
elements, as children of the root element.

You can use I<generic_attribute_condition>, I<attribute_condition>,
I<full_path>, I<partial_path>, I<tag>, I<tag_regexp>, I<_default_> and 
I<_all_> to trigger the building of the twig. 
I<string_condition> and I<regexp_condition> cannot be used as the content 
of the element, and the string, have not yet been parsed when the condition
is checked.

B<WARNING>: path are checked for the document. Even if the C<twig_roots> option
is used they will be checked against the full document tree, not the virtual
tree created by XML::Twig


B<WARNING>: twig_roots elements should NOT be nested, that would hopelessly
confuse XML::Twig ;--(

Note: you can set handlers (twig_handlers) using twig_roots
  Example: my $t= XML::Twig->new( twig_roots => 
                                   { title    => sub { $_{1]->print;}, 
                                     subtitle => \&process_subtitle 
                                   }
                               );
           $t->parsefile( file);
 

=item twig_print_outside_roots

To be used in conjunction with the C<twig_roots> argument. When set to a true 
value this will print the document outside of the C<twig_roots> elements.

 Example: my $t= XML::Twig->new( twig_roots => { title => \&number_title },
                                twig_print_outside_roots => 1,
                               );
           $t->parsefile( file);
           { my $nb;
           sub number_title
             { my( $twig, $title);
               $nb++;
               $title->prefix( "$nb "; }
               $title->print;
             }
           }
               

This example prints the document outside of the title element, calls 
C<number_title> for each C<title> element, prints it, and then resumes printing
the document. The twig is built only for the C<title> elements. 

If the value is a reference to a file handle then the document outside the
C<twig_roots> elements will be output to this file handle:

  open( OUT, ">out_file") or die "cannot open out file out_file:$!";
  my $t= XML::Twig->new( twig_roots => { title => \&number_title },
                         # default output to OUT
                         twig_print_outside_roots => \*OUT, 
                       );

         { my $nb;
           sub number_title
             { my( $twig, $title);
               $nb++;
               $title->prefix( "$nb "; }
               $title->print( \*OUT);    # you have to print to \*OUT here
             }
           }


=item start_tag_handlers

A hash C<{ expression => \&handler}>. Sets element handlers that are called when
the element is open (at the end of the XML::Parser C<Start> handler). The handlers
are called with 2 params: the twig and the element. The element is empty at 
that point, its attributes are created though. 

You can use I<generic_attribute_condition>, I<attribute_condition>,
I<full_path>, I<partial_path>, I<tag>, I<tag_regexp>, I<_default_>  and I<_all_> 
to trigger the handler. 

I<string_condition> and I<regexp_condition> cannot be used as the content of 
the element, and the string, have not yet been parsed when the condition is 
checked.

The main uses for those handlers are to change the tag name (you might have to 
do it as soon as you find the open tag if you plan to C<flush> the twig at some
point in the element, and to create temporary attributes that will be used
when processing sub-element with C<twig_hanlders>. 

You should also use it to change tags if you use C<flush>. If you change the tag 
in a regular C<twig_handler> then the start tag might already have been flushed. 

B<Note>: C<start_tag> handlers can be called outside of C<twig_roots> if this 
argument is used, in this case handlers are called with the following arguments:
C<$t> (the twig), C<$tag> (the tag of the element) and C<%att> (a hash of the 
attributes of the element). 

If the C<twig_print_outside_roots> argument is also used, if the last handler
called returns  a C<true> value, then the the start tag will be output as it
appeared in the original document, if the handler returns a a C<false> value
then the start tag will B<not> be printed (so you can print a modified string 
yourself for example).

Note that you can use the L<ignore|ignore> method in C<start_tag_handlers> 
(and only there). 

=item end_tag_handlers

A hash C<{ expression => \&handler}>. Sets element handlers that are called when
the element is closed (at the end of the XML::Parser C<End> handler). The handlers
are called with 2 params: the twig and the tag of the element. 

I<twig_handlers> are called when an element is completely parsed, so why have 
this redundant option? There is only one use for C<end_tag_handlers>: when using
the C<twig_roots> option, to trigger a handler for an element B<outside> the roots.
It is for example very useful to number titles in a document using nested 
sections: 

  my @no= (0);
  my $no;
  my $t= XML::Twig->new( 
          start_tag_handlers => 
           { section => sub { $no[$#no]++; $no= join '.', @no; push @no, 0; } },
          twig_roots         => 
           { title   => sub { $_[1]->prefix( $no); $_[1]->print; } },
          end_tag_handlers   => { section => sub { pop @no;  } },
          twig_print_outside_roots => 1
                      );
   $t->parsefile( $file);

Using the C<end_tag_handlers> argument without C<twig_roots> will result in an
error.

=item do_not_chain_handlers

If this option is set to a true value, then only one handler will be called for
each element, even if several satisfy the condition

Note that the C<_all_> handler will still be called regardeless

=item ignore_elts

This option lets you ignore elements when building the twig. This is useful 
in cases where you cannot use C<twig_roots> to ignore elements, for example if
the element to ignore is a sibling of elements you are interested in.

Example:

  my $twig= XML::Twig->new( ignore_elts => { elt => 1 });
  $twig->parsefile( 'doc.xml');

This will build the complete twig for the document, except that all C<elt> 
elements (and their children) will be left out.


=item char_handler

A reference to a subroutine that will be called every time C<PCDATA> is found.

=item elt_class

The name of a class used to store elements. this class should inherit from
C<XML::Twig::Elt> (and by default it is C<XML::Twig::Elt>). This option is used
to subclass the element class and extend it with new methods.

This option is needed because during the parsing of the XML, elements are created
by C<XML::Twig>, without any control from the user code.

=item keep_atts_order

Setting this option to a true value causes the attribute hash to be tied to
a C<Tie::IxHash> object.
This means that C<Tie::IxHash> needs to be installed for this option to be 
available. It also means that the hash keeps its order, so you will get 
the attributes in order. This allows outputing the attributes in the same 
order as they were in the original document.

=item keep_encoding

This is a (slightly?) evil option: if the XML document is not UTF-8 encoded and
you want to keep it that way, then setting keep_encoding will use theC<Expat> 
original_string method for character, thus keeping the original encoding, as 
well as the original entities in the strings.

See the C<t/test6.t> test file to see what results you can expect from the 
various encoding options.

B<WARNING>: if the original encoding is multi-byte then attribute parsing will
be EXTREMELY unsafe under any Perl before 5.6, as it uses regular expressions
which do not deal properly with multi-byte characters. You can specify an 
alternate function to parse the start tags with the C<parse_start_tag> option 
(see below)

B<WARNING>: this option is NOT used when parsing with the non-blocking parser 
(C<parse_start>, C<parse_more>, parse_done methods) which you probably should 
not use with XML::Twig anyway as they are totally untested!

=item output_encoding

This option generates an output_filter using C<Encode>,  C<Text::Iconv> or 
C<Unicode::Map8> and C<Unicode::Strings>, and sets the encoding in the XML
declaration. This is the easiest way to deal with encodings, if you need 
more sophisticated features, look at C<output_filter> below


=item output_filter

This option is used to convert the character encoding of the output document.
It is passed either a string corresponding to a predefined filter or
a subroutine reference. The filter will be called every time a document or 
element is processed by the "print" functions (C<print>, C<sprint>, C<flush>). 

Pre-defined filters: 

=over 4 

=item latin1 

uses either C<Encode>, C<Text::Iconv> or C<Unicode::Map8> and C<Unicode::String>
or a regexp (which works only with XML::Parser 2.27), in this order, to convert 
all characters to ISO-8859-1 (aka latin1)

=item html

does the same conversion as C<latin1>, plus encodes entities using
C<HTML::Entities> (oddly enough you will need to have HTML::Entities intalled 
for it to be available). This should only be used if the tags and attribute 
names themselves are in US-ASCII, or they will be converted and the output will
not be valid XML any more

=item safe

converts the output to ASCII (US) only  plus I<character entities> (C<&#nnn;>) 
this should be used only if the tags and attribute names themselves are in 
US-ASCII, or they will be converted and the output will not be valid XML any 
more

=item safe_hex

same as C<safe> except that the character entities are in hexa (C<&#xnnn;>)

=item encode_convert ($encoding)

Return a subref that can be used to convert utf8 strings to C<$encoding>).
Uses C<Encode>.

   my $conv = XML::Twig::encode_convert( 'latin1');
   my $t = XML::Twig->new(output_filter => $conv);

=item iconv_convert ($encoding)

this function is used to create a filter subroutine that will be used to 
convert the characters to the target encoding using C<Text::Iconv> (which needs
to be installed, look at the documentation for the module and for the
C<iconv> library to find out which encodings are available on your system)

   my $conv = XML::Twig::iconv_convert( 'latin1');
   my $t = XML::Twig->new(output_filter => $conv);

=item unicode_convert ($encoding)

this function is used to create a filter subroutine that will be used to 
convert the characters to the target encoding using  C<Unicode::Strings> 
and C<Unicode::Map8> (which need to be installed, look at the documentation 
for the modules to find out which encodings are available on your system)

   my $conv = XML::Twig::unicode_convert( 'latin1');
   my $t = XML::Twig->new(output_filter => $conv);

=back

The C<text> and C<att> methods do not use the filter, so their 
result are always in unicode.

Those predeclared filters are based on subroutines that can be used
by themselves (as C<XML::Twig::foo>). 

=over 4

=item html_encode ($string)

Use C<HTML::Entities> to encode a utf8 string

=item safe_encode ($string)

Use either a regexp (perl < 5.8) or C<Encode> to encode non-ascii characters
in the string in C<< &#<nnnn>; >> format

=item safe_encode_hex ($string)

Use either a regexp (perl < 5.8) or C<Encode> to encode non-ascii characters
in the string in C<< &#x<nnnn>; >> format

=item regexp2latin1 ($string)

Use a regexp to encode a utf8 string into latin 1 (ISO-8859-1). Does not
work with Perl 5.8.0!

=back

=item output_text_filter

same as output_filter, except it doesn't apply to the brackets and quotes 
around attribute values. This is useful for all filters that could change
the tagging, basically anything that does not just change the encoding of
the output. C<html>, C<safe> and C<safe_hex> are better used with this option.

=item input_filter

This option is similar to C<output_filter> except the filter is applied to 
the characters before they are stored in the twig, at parsing time.

=item remove_cdata

Setting this option to a true value will force the twig to output CDATA 
sections as regular (escaped) PCDATA

=item parse_start_tag

If you use the C<keep_encoding> option then this option can be used to replace
the default parsing function. You should provide a coderef (a reference to a 
subroutine) as the argument, this subroutine takes the original tag (given
by XML::Parser::Expat C<original_string()> method) and returns a tag and the
attributes in a hash (or in a list attribute_name/attribute value).

=item expand_external_ents

When this option is used external entities (that are defined) are expanded
when the document is output using "print" functions such as C<L<print> >,
C<L<sprint|sprint> >, C<L<flush|flush> > and C<L<xml_string|xml_string> >. 
Note that in the twig the entity will be stored as an element whith a 
tag 'C<#ENT>', the entity will not be expanded there, so you might want to 
process the entities before outputting it. 

=item load_DTD

If this argument is set to a true value, C<parse> or C<parsefile> on the twig
will load  the DTD information. This information can then be accessed through 
the twig, in a C<DTD_handler> for example. This will load even an external DTD.

Default and fixed values for attributes will also be filled, based on the DTD.

Note that to do this the module will generate a temporary file in the current
directory. If this is a problem let me know and I will add an option to
specify an alternate directory.

See L<DTD Handling|DTD Handling> for more information

=item DTD_handler

Set a handler that will be called once the doctype (and the DTD) have been 
loaded, with 2 arguments, the twig and the DTD.

=item no_prolog

Does not output a prolog (XML declaration and DTD)

=item id

This optional argument gives the name of an attribute that can be used as
an ID in the document. Elements whose ID is known can be accessed through
the elt_id method. id defaults to 'id'.
See C<L<BUGS|BUGS> >

=item discard_spaces

If this optional argument is set to a true value then spaces are discarded
when they look non-significant: strings containing only spaces are discarded.
This argument is set to true by default.

=item keep_spaces

If this optional argument is set to a true value then all spaces in the
document are kept, and stored as C<PCDATA>.
C<keep_spaces> and C<discard_spaces> cannot be both set.

=item discard_spaces_in

This argument sets C<keep_spaces> to true but will cause the twig builder to
discard spaces in the elements listed.

The syntax for using this argument is:
 
  XML::Twig->new( discard_spaces_in => [ 'elt1', 'elt2']);

=item keep_spaces_in

This argument sets C<discard_spaces> to true but will cause the twig builder to
keep spaces in the elements listed.

The syntax for using this argument is: 

  XML::Twig->new( keep_spaces_in => [ 'elt1', 'elt2']);

=item pretty_print

Set the pretty print method, amongst 'C<none>' (default), 'C<nsgmls>', 
'C<nice>', 'C<indented>', 'C<indented_c>', 'C<record>' and 'C<record_c>'

pretty_print formats:

=over 4

=item none

The document is output as one ling string, with no line breaks except those 
found within text elements

=item nsgmls

Line breaks are inserted in safe places: that is within tags, between a tag 
and an attribute, between attributes and before the > at the end of a tag.

This is quite ugly but better than C<none>, and it is very safe, the document 
will still be valid (conforming to its DTD).

This is how the SGML parser C<sgmls> splits documents, hence the name.

=item nice

This option inserts line breaks before any tag that does not contain text (so
element with textual content are not broken as the \n is the significant).

B<WARNING>: this option leaves the document well-formed but might make it
invalid (not conformant to its DTD). If you have elements declared as

  <!ELEMENT foo (#PCDATA|bar)>

then a C<foo> element including a C<bar> one will be printed as

  <foo>
  <bar>bar is just pcdata</bar>
  </foo>

This is invalid, as the parser will take the line break after the C<foo> tag 
as a sign that the element contains PCDATA, it will then die when it finds the 
C<bar> tag. This may or may not be important for you, but be aware of it!

=item indented

Same as C<nice> (and with the same warning) but indents elements according to 
their level 

=item indented_c

Same as C<indented> but a little more compact: the closing tags are on the 
same line as the preceeding text

=item record

This is a record-oriented pretty print, that display data in records, one field 
per line (which looks a LOT like C<indented>)

=item record_c

Stands for record compact, one record per line

=back


=item empty_tags

Set the empty tag display style ('C<normal>', 'C<html>' or 'C<expand>').

=item comments

Set the way comments are processed: 'C<drop>' (default), 'C<keep>' or 
'C<process>' 

Comments processing options:

=over 4

=item drop

drops the comments, they are not read, nor printed to the output

=item keep

comments are loaded and will appear on the output, they are not 
accessible within the twig and will not interfere with processing
though

B<Note>: comments in the middle of a text element such as 

  <p>text <!-- comment --> more text --></p>

are kept at their original position in the text. Using ??"print"
methods like C<print> or C<sprint> will return the comments in the
text. Using C<text> or C<field> on the other hand will not.

Any use of C<set_pcdata> on the C<#PCDATA> element (directly or 
through other methods like C<set_content>) will delete the comment(s).

=item process

comments are loaded in the twig and will be treated as regular elements 
(their C<tag> is C<#COMMENT>) this can interfere with processing if you
expect C<< $elt->{first_child} >> to be an element but find a comment there.
Validation will not protect you from this as comments can happen anywhere.
You can use C<< $elt->first_child( 'tag') >> (which is a good habit anyway)
to get where you want. 

Consider using C<process> if you are outputing SAX events from XML::Twig.

=back

=item pi

Set the way processing instructions are processed: 'C<drop>', 'C<keep>' 
(default) or 'C<process>'

Note that you can also set PI handlers in the C<twig_handlers> option: 

  '?'       => \&handler
  '?target' => \&handler 2

The handlers will be called with 2 parameters, the twig and the PI element if
C<pi> is set to C<process>, and with 3, the twig, the target and the data if
C<pi> is set to C<keep>. Of course they will not be called if C<pi> is set to 
C<drop>.

If C<pi> is set to C<keep> the handler should return a string that will be used
as-is as the PI text (it should look like "C< <?target data?> >" or '' if you
want to remove the PI), 

Only one handler will be called, C<?target> or C<?> if no specific handler for
that target is available.

=item map_xmlns 

This option is passed a hashref that maps uri's to prefixes. The prefixes in
the document will be replaced by the ones in the map. The mapped prefixes can
(actually have to) be used to trigger handlers, navigate or query the document.

Here is an example:

  my $t= XML::Twig->new( map_xmlns => {'http://www.w3.org/2000/svg' => "svg"},
                         twig_handlers => 
                           { 'svg:circle' => sub { $_->set_att( r => 20) } },
                         pretty_print => 'indented', 
                       )
                  ->parse( '<doc xmlns:gr="http://www.w3.org/2000/svg">
                              <gr:circle cx="10" cy="90" r="10"/>
                           </doc>'
                         )
                  ->print;

This will output:

  <doc xmlns:svg="http://www.w3.org/2000/svg">
     <svg:circle cx="10" cy="90" r="20"/>
  </doc>

=item keep_original_prefix

When used with C<L<map_xmlns>> this option will make C<XML::Twig> use the original
namespace prefixes when outputing a document. The mapped prefix will still be used
for triggering handlers and in navigation and query methods.

  my $t= XML::Twig->new( map_xmlns => {'http://www.w3.org/2000/svg' => "svg"},
                         twig_handlers => 
                           { 'svg:circle' => sub { $_->set_att( r => 20) } },
                         keep_original_prefix => 1,
                         pretty_print => 'indented', 
                       )
                  ->parse( '<doc xmlns:gr="http://www.w3.org/2000/svg">
                              <gr:circle cx="10" cy="90" r="10"/>
                           </doc>'
                         )
                  ->print;

This will output:

  <doc xmlns:gr="http://www.w3.org/2000/svg">
     <gr:circle cx="10" cy="90" r="20"/>
  </doc>

=item index ($arrayref or $hashref)

This option creates lists of specific elements during the parsing of the XML.
It takes a reference to either a list of triggering expressions or to a hash 
name => expression, and for each one generates the list of elements that 
match the expression. The list can be accessed through the C<L<index>> method.

example:

  # using an array ref
  my $t= XML::Twig->new( index => [ 'div', 'table' ])
                  ->parsefile( "foo.xml');
  my $divs= $t->index( 'div');
  my $first_div= $divs->[0];
  my $last_table= $t->index( table => -1);

  # using a hashref to name the indexes
  my $t= XML::Twig->new( index => { email => 'a[@href=~/^\s*mailto:/]')
                  ->parsefile( "foo.xml');
  my $last_emails= $t->index( email => -1);

Note that the index is not maintained after the parsing. If elements are 
deleted, renamed or otherwise hurt during processing, the index is NOT updated.

=back

B<Note>: I _HATE_ the Java-like name of arguments used by most XML modules.
So in pure TIMTOWTDI fashion all arguments can be written either as
C<UglyJavaLikeName> or as C<readable_perl_name>: C<twig_print_outside_roots>
or C<TwigPrintOutsideRoots> (or even C<twigPrintOutsideRoots> {shudder}). 
XML::Twig normalizes them before processing them.

=item parse (SOURCE [, OPT => OPT_VALUE [...]])

This method is inherited from XML::Parser.
The C<SOURCE> parameter should either be a string containing the whole XML
document, or it should be an open C<IO::Handle>. Constructor options to
C<XML::Parser::Expat> given as keyword-value pairs may follow theC<SOURCE> 
parameter. These override, for this call, any options or attributes passed
through from the XML::Parser instance.

A die call is thrown if a parse error occurs. Otherwise it will return 
the twig built by the parse. Use C<safe_parse> if you want the parsing
to return even when an error occurs.

=item parsestring

This is just an alias for C<parse> for backwards compatibility.

=item parsefile (FILE [, OPT => OPT_VALUE [...]])

This method is inherited from XML::Parser.

Open C<FILE> for reading, then call C<parse> with the open handle. The file
is closed no matter how C<parse> returns. 

A C<die> call is thrown if a parse error occurs. Otherwise it will return 
the twig built by the parse. Use C<safe_parsefile> if you want the parsing
to return even when an error occurs.

=item parseurl ($url $optional_user_agent)

Gets the data from C<$url> and parse it. Note that the data is piped to the
parser in chunks the size of the XML::Parser::Expat buffer, so memory 
consumption and hopefully speed are optimal.

If the C<$optional_user_agent> argument is used then it is used, otherwise a
new one is created.

=item safe_parse ( SOURCE [, OPT => OPT_VALUE [...]])

This method is similar to C<parse> except that it wraps the parsing in an
C<eval> block. It returns the twig on success and 0 on failure (the twig object
also contains the parsed twig). C<$@> contains the error message on failure.

Note that the parsing still stops as soon as an error is detected, there is
no way to keep going after an error.

=item safe_parsefile (FILE [, OPT => OPT_VALUE [...]])

This method is similar to C<parsefile> except that it wraps the parsing in an
C<eval> block. It returns the twig on success and 0 on failure (the twig object
also contains the parsed twig) . C<$@> contains the error message on failure

Note that the parsing still stops as soon as an error is detected, there is
no way to keep going after an error.

=item safe_parseurl ($url $optional_user_agent)

Same as C<parseurl> except that it wraps the parsing in an C<eval> block. It 
returns the twig on success and 0 on failure (the twig object also contains
the parsed twig) . C<$@> contains the error message on failure

=item parser

This method returns the C<expat> object (actually the XML::Parser::Expat object) 
used during parsing. It is useful for example to call XML::Parser::Expat methods
on it. To get the line of a tag for example use C<< $t->parser->current_line >>.

=item setTwigHandlers ($handlers)

Set the twig_handlers. C<$handlers> is a reference to a hash similar to the
one in the C<twig_handlers> option of new. All previous handlers are unset.
The method returns the reference to the previous handlers.

=item setTwigHandler ($exp $handler)

Set a single twig_handler for elements matching C<$exp>. C<$handler> is a 
reference to a subroutine. If the handler was previously set then the reference 
to the previous handler is returned.

=item setStartTagHandlers ($handlers)

Set the start_tag handlers. C<$handlers> is a reference to a hash similar to the
one in the C<start_tag_handlers> option of new. All previous handlers are unset.
The method returns the reference to the previous handlers.

=item setStartTagHandler ($exp $handler)

Set a single start_tag handlers for elements matching C<$exp>. C<$handler> is a 
reference to a subroutine. If the handler was previously set then the reference
to the previous handler is returned.

=item setEndTagHandlers ($handlers)

Set the end_tag handlers. C<$handlers> is a reference to a hash similar to the
one in the C<end_tag_handlers> option of new. All previous handlers are unset.
The method returns the reference to the previous handlers.

=item setEndTagHandler ($exp $handler)

Set a single end_tag handlers for elements matching C<$exp>. C<$handler> is a 
reference to a subroutine. If the handler was previously set then the 
reference to the previous handler is returned.

=item setTwigRoots ($handlers)

Same as using the C<L<twig_roots>> option when creating the twig

=item setCharHandler ($exp $handler)

Set a C<char_handler>

=item setIgnoreEltsHandler ($exp)

Set a C<ignore_elt> handler (elements that match C<$exp> will be ignored

=item setIgnoreEltsHandlers ($exp)

Set all C<ignore_elt> handlers (previous handlers are replaced)

=item dtd

Return the dtd (an L<XML::Twig::DTD> object) of a twig

=item xmldecl

Return the XML declaration for the document, or a default one if it doesn't
have one

=item doctype

Return the doctype for the document

=item dtd_text

Return the DTD text

=item dtd_print

Print the DTD

=item model ($tag)

Return the model (in the DTD) for the element C<$tag>

=item root

Return the root element of a twig

=item set_root ($elt)

Set the root of a twig

=item first_elt ($optional_condition)

Return the first element matching C<$optional_condition> of a twig, if
no condition is given then the root is returned

=item last_elt ($optional_condition)

Return the last element matching C<$optional_condition> of a twig, if
no condition is given then the last element of the twig is returned

=item elt_id        ($id)

Return the element whose C<id> attribute is $id

=item getEltById

Same as C<L<elt_id>>

=item index ($index_name, $optional_index)

If the C<$optional_index> argument is present, return the corresponding element
in the index (created using the C<index> option for C<XML::Twig->new>)

If the argument is not present, return an arrayref to the index

=item encoding

This method returns the encoding of the XML document, as defined by the 
C<encoding> attribute in the XML declaration (ie it is C<undef> if the attribute
is not defined)

=item set_encoding

This method sets the value of the C<encoding> attribute in the XML declaration. 
Note that if the document did not have a declaration it is generated (with
an XML version of 1.0)

=item xml_version

This method returns the XML version, as defined by the C<version> attribute in 
the XML declaration (ie it is C<undef> if the attribute is not defined)

=item set_xml_version

This method sets the value of the C<version> attribute in the XML declaration. 
If the declaration did not exist it is created.

=item standalone

This method returns the value of the C<standalone> declaration for the document

=item set_standalone

This method sets the value of the C<standalone> attribute in the XML 
declaration.  Note that if the document did not have a declaration it is 
generated (with an XML version of 1.0)

=item set_output_encoding

Set the C<encoding> "attribute" in the XML declaration

=item set_doctype ($name, $system, $public, $internal)

Set the doctype of the element. If an argument is C<undef> (or not present)
then its former value is retained, if a false ('' or 0) value is passed then
the former value is deleted;

=item entity_list

Return the entity list of a twig

=item entity_names

Return the list of all defined entities

=item entity ($entity_name)

Return the entity 

=item change_gi      ($old_gi, $new_gi)

Performs a (very fast) global change. All elements C<$old_gi> are now 
C<$new_gi>. This is a bit dangerous though and should be avoided if
< possible, as the new tag might be ignored in subsequent processing.

See C<L<BUGS|BUGS> >

=item flush            ($optional_filehandle, $options)

Flushes a twig up to (and including) the current element, then deletes
all unnecessary elements from the tree that's kept in memory.
C<flush> keeps track of which elements need to be open/closed, so if you
flush from handlers you don't have to worry about anything. Just keep 
flushing the twig every time you're done with a sub-tree and it will
come out well-formed. After the whole parsing don't forget toC<flush> 
one more time to print the end of the document.
The doctype and entity declarations are also printed.

flush take an optional filehandle as an argument.

options: use the C<update_DTD> option if you have updated the (internal) DTD 
and/or the entity list and you want the updated DTD to be output 

The C<pretty_print> option sets the pretty printing of the document.

   Example: $t->flush( Update_DTD => 1);
            $t->flush( \*FILE, Update_DTD => 1);
            $t->flush( \*FILE);


=item flush_up_to ($elt, $optional_filehandle, %options)

Flushes up to the C<$elt> element. This allows you to keep part of the
tree in memory when you C<flush>.

options: see flush.

=item purge

Does the same as a C<flush> except it does not print the twig. It just deletes
all elements that have been completely parsed so far.

=item purge_up_to ($elt)

Purges up to the C<$elt> element. This allows you to keep part of the tree in 
memory when you C<purge>.

=item print            ($optional_filehandle, %options)

Prints the whole document associated with the twig. To be used only AFTER the
parse.
 
options: see C<flush>.

=item sprint

Return the text of the whole document associated with the twig. To be used only
AFTER the parse.

options: see C<flush>.

=item trim

Trim the document: gets rid of initial and trailing spaces, and relace multiple spaces
by a single one.

=item toSAX1 ($handler)

Send SAX events for the twig to the SAX1 handler C<$handler>

=item toSAX2 ($handler)

Send SAX events for the twig to the SAX2 handler C<$handler>

=item flush_toSAX1 ($handler)

Same as flush, except that SAX events are sent to the SAX1 handler
C<$handler> instead of the twig being printed

=item flush_toSAX2 ($handler)

Same as flush, except that SAX events are sent to the SAX2 handler
C<$handler> instead of the twig being printed

=item ignore

This method can B<only> be called in C<start_tag_handlers>. It causes the 
element to be skipped during the parsing: the twig is not built for this 
element, it will not be accessible during parsing or after it. The element 
will not take up any memory and parsing will be faster.

Note that this method can also be called on an element. If the element is a 
parent of the current element then this element will be ignored (the twig will
not be built any more for it and what has already been built will be deleted)


=item set_pretty_print  ($style)

Set the pretty print method, amongst 'C<none>' (default), 'C<nsgmls>', 
'C<nice>', 'C<indented>', 'C<record>' and 'C<record_c>'

B<WARNING:> the pretty print style is a B<GLOBAL> variable, so once set it's
applied to B<ALL> C<print>'s (and C<sprint>'s). Same goes if you use XML::Twig
with C<mod_perl> . This should not be a problem as the XML that's generated 
is valid anyway, and XML processors (as well as HTML processors, including
browsers) should not care. Let me know if this is a big problem, but at the
moment the performance/cleanliness trade-off clearly favors the global 
approach.

=item set_empty_tag_style  ($style)

Set the empty tag display style ('C<normal>', 'C<html>' or 'C<expand>'). As 
with C<L<set_pretty_print>> this sets a global flag.  

C<normal> outputs an empty tag 'C<< <tag/> >>', C<html> adds a space 
'C<< <tag /> >>' and C<expand> outputs 'C<< <tag></tag> >>'

=item set_remove_cdata  ($flag)

set (or unset) the flag that forces the twig to output CDATA sections as 
regular (escaped) PCDATA

=item print_prolog     ($optional_filehandle, %options)

Prints the prolog (XML declaration + DTD + entity declarations) of a document.

options: see C<L<flush>>.

=item prolog     ($optional_filehandle, %options)

Return the prolog (XML declaration + DTD + entity declarations) of a document.

options: see C<L<flush>>.

=item finish

Call Expat C<finish> method.
Unsets all handlers (including internal ones that set context), but expat
continues parsing to the end of the document or until it finds an error.
It should finish up a lot faster than with the handlers set.

=item finish_print

Stop twig processing, flush the twig and proceed to finish printing the 
document as fast as possible. Use this method when modifying a document and 
the modification is done. 

=item set_expand_external_entities

Same as using the C<L<expand_external_ents>> option when creating the twig

=item set_input_filter

Same as using the C<L<input_filter>> option when creating the twig

=item set_keep_atts_order

Same as using the C<L<keep_atts_order>> option when creating the twig

=item set_keep_encoding

Same as using the C<L<keep_encoding>> option when creating the twig

=item set_output_filter

Same as using the C<L<output_filter>> option when creating the twig

=item set_output_text_filter

Same as using the C<L<output_text_filter>> option when creating the twig

=item Methods inherited from XML::Parser::Expat

A twig inherits all the relevant methods from XML::Parser::Expat. These 
methods can only be used during the parsing phase (they will generate
a fatal error otherwise).

Inherited methods are:

=over 4

=item depth

Returns the size of the context list.

=item in_element

Returns true if NAME is equal to the name of the innermost cur???
rently opened element. If namespace processing is being used and
you want to check against a name that may be in a namespace, then
use the generate_ns_name method to create the NAME argument.

=item within_element

Returns the number of times the given name appears in the context
list.  If namespace processing is being used and you want to check
against a name that may be in a namespace, then use the gener???
ate_ns_name method to create the NAME argument.

=item context

Returns a list of element names that represent open elements, with
the last one being the innermost. Inside start and end tag han???
dlers, this will be the tag of the parent element.

=item current_line

Returns the line number of the current position of the parse.

=item current_column

Returns the column number of the current position of the parse.

=item current_byte

Returns the current position of the parse.

=item position_in_context

Returns a string that shows the current parse position. LINES
should be an integer >= 0 that represents the number of lines on
either side of the current parse line to place into the returned
string.

=item base ([NEWBASE])

Returns the current value of the base for resolving relative URIs.
If NEWBASE is supplied, changes the base to that value.

=item current_element

Returns the name of the innermost currently opened element. Inside
start or end handlers, returns the parent of the element associated
with those tags.

=item element_index

Returns an integer that is the depth-first visit order of the cur???
rent element. This will be zero outside of the root element. For
example, this will return 1 when called from the start handler for
the root element start tag.

=item recognized_string

Returns the string from the document that was recognized in order
to call the current handler. For instance, when called from a start
handler, it will give us the the start-tag string. The string is
encoded in UTF-8.  This method doesn't return a meaningful string
inside declaration handlers.

=item original_string

Returns the verbatim string from the document that was recognized
in order to call the current handler. The string is in the original
document encoding. This method doesn't return a meaningful string
inside declaration handlers.

=item xpcroak

Concatenate onto the given message the current line number within
the XML document plus the message implied by ErrorContext. Then
croak with the formed message.

=item xpcarp 

Concatenate onto the given message the current line number within
the XML document plus the message implied by ErrorContext. Then
carp with the formed message.

=item xml_escape(TEXT [, CHAR [, CHAR ...]])

Returns TEXT with markup characters turned into character entities.
Any additional characters provided as arguments are also turned
into character references where found in TEXT.

(this method is broken on some versions of expat/XML::Parser)

=back

=item path ( $optional_tag)

Return the element context in a form similar to XPath's short
form: 'C</root/tag1/../tag>'

=item get_xpath  ( $optional_array_ref, $xpath, $optional_offset)

Performs a C<get_xpath> on the document root (see <Elt|"Elt">)

If the C<$optional_array_ref> argument is used the array must contain
elements. The C<$xpath> expression is applied to each element in turn 
and the result is union of all results. This way a first query can be
refined in further steps.


=item find_nodes ( $optional_array_ref, $xpath, $optional_offset)

same as C<get_xpath> 

=item findnodes ( $optional_array_ref, $xpath, $optional_offset)

same as C<get_xpath> (similar to the XML::LibXML method)

=item findvalue ( $optional_array_ref, $xpath, $optional_offset)

Return the C<join> of all texts of the results of appling C<L<get_xpath>>
to the node (similar to the XML::LibXML method)

=item subs_text ($regexp, $replace)

subs_text does text substitution on the whole document, similar to perl's 
C< s///> operator.

=item dispose

Useful only if you don't have C<Scalar::Util> or C<WeakRef> installed.

Reclaims properly the memory used by an XML::Twig object. As the object has
circular references it never goes out of scope, so if you want to parse lots 
of XML documents then the memory leak becomes a problem. Use
C<< $twig->dispose >> to clear this problem.

=back 


=head2 XML::Twig::Elt

=over 4

=item new          ($optional_tag, $optional_atts, @optional_content)

The C<tag> is optional (but then you can't have a content ), the C<$optional_atts> 
argument is a refreference to a hash of attributes, the content can be just a 
string or a list of strings and element. A content of 'C<#EMPTY>' creates an empty 
element;

 Examples: my $elt= XML::Twig::Elt->new();
           my $elt= XML::Twig::Elt->new( para => { align => 'center' });  
           my $elt= XML::Twig::Elt->new( para => { align => 'center' }, 'foo');  
           my $elt= XML::Twig::Elt->new( br   => '#EMPTY');
           my $elt= XML::Twig::Elt->new( 'para');
           my $elt= XML::Twig::Elt->new( para => 'this is a para');  
           my $elt= XML::Twig::Elt->new( para => $elt3, 'another para'); 

The strings are not parsed, the element is not attached to any twig.

B<WARNING>: if you rely on ID's then you will have to set the id yourself. At
this point the element does not belong to a twig yet, so the ID attribute
is not known so it won't be strored in the ID list.

Note that C<#COMMENT>, C<#PCDATA> or C<#CDATA> are valid tag names, that will 
create text elements.

To create an element C<foo> containing a CDATA section:

           my $foo= XML::Twig::Elt->new( '#CDATA' => "content of the CDATA section")
                                  ->wrap_in( 'foo');

=item parse         ($string, %args)

Creates an element from an XML string. The string is actually
parsed as a new twig, then the root of that twig is returned.
The arguments in C<%args> are passed to the twig.
As always if the parse fails the parser will die, so use an
eval if you want to trap syntax errors.

As obviously the element does not exist beforehand this method has to be 
called on the class: 

  my $elt= parse XML::Twig::Elt( "<a> string to parse, with <sub/>
                                  <elements>, actually tons of </elements>
                  h</a>");

=item print         ($optional_filehandle, $optional_pretty_print_style)

Prints an entire element, including the tags, optionally to a 
C<$optional_filehandle>, optionally with a C<$pretty_print_style>.

The print outputs XML data so base entities are escaped.

=item sprint       ($elt, $optional_no_enclosing_tag)

Return the xml string for an entire element, including the tags. 
If the optional second argument is true then only the string inside the 
element is returned (the start and end tag for $elt are not).
The text is XML-escaped: base entities (& and < in text, & < and " in
attribute values) are turned into entities.

=item gi                       

Return the gi of the element (the gi is the C<generic identifier> the tag
name in SGML parlance).

C<tag> and C<name> are synonyms of C<gi>.

=item tag

Same as C<L<gi|gi>>

=item name

Same as C<L<tag|tag>>

=item set_gi         ($tag)

Set the gi (tag) of an element

=item set_tag        ($tag)

Set the tag (=C<L<tag|tag>>) of an element

=item set_name       ($name)

Set the name (=C<L<tag|tag>>) of an element

=item root 

Return the root of the twig in which the element is contained.

=item twig 

Return the twig containing the element. 

=item parent        ($optional_condition)

Return the parent of the element, or the first ancestor matching the 
C<$optional_condition>

=item first_child   ($optional_condition)

Return the first child of the element, or the first child matching the 
C<$optional_condition>

=item has_child ($optional_condition)

Return the first child of the element, or the first child matching the 
C<$optional_condition> (same as L<first_child>)

=item has_children ($optional_condition)

Return the first child of the element, or the first child matching the 
C<$optional_condition> (same as L<first_child>)


=item first_child_text   ($optional_condition)

Return the text of the first child of the element, or the first child
 matching the C<$optional_condition>
If there is no first_child then returns ''. This avoids getting the
child, checking for its existence then getting the text for trivial cases.

Similar methods are available for the other navigation methods: 

=over 4

=item last_child_text

=item prev_sibling_text

=item next_sibling_text

=item prev_elt_text

=item next_elt_text

=item child_text

=item parent_text

=back

All this methods also exist in "trimmed" variant: 

=over 4

=item first_child_trimmed_text

=item last_child_trimmed_text

=item prev_sibling_trimmed_text

=item next_sibling_trimmed_text

=item prev_elt_trimmed_text

=item next_elt_trimmed_text

=item child_trimmed_text

=item parent_trimmed_text

=back

=item field         ($optional_condition)

Same method as C<first_child_text> with a different name

=item trimmed_field         ($optional_condition)

Same method as C<first_child_trimmed_text> with a different name

=item set_field ($condition, $optional_atts, @list_of_elt_and_strings)

Set the content of the first child of the element that matches
C<$condition>, the rest of the arguments is tha same as for C<L<set_content>>

If no child matches C<$condition> _and_ if C<$condition> is a valid
XML element name, then a new element by that name is created and 
inserted as the last child.

=item first_child_matches   ($optional_condition)

Return the element if the first child of the element (if it exists) passes
the C<$optional_condition> C<undef> otherwise

  if( $elt->first_child_matches( 'title')) ... 

is equivalent to

  if( $elt->{first_child} && $elt->{first_child}->passes( 'title')) 

C<first_child_is> is an other name for this method

Similar methods are available for the other navigation methods: 

=over 4

=item last_child_matches

=item prev_sibling_matches

=item next_sibling_matches

=item prev_elt_matches

=item next_elt_matches

=item child_matches

=item parent_matches

=back

=item is_first_child ($optional_condition)

returns true (the element) if the element is the first child of its parent
(optionaly that satisfies the C<$optional_condition>)

=item is_last_child ($optional_condition)

returns true (the element) if the element is the first child of its parent
(optionaly that satisfies the C<$optional_condition>)

=item prev_sibling  ($optional_condition)

Return the previous sibling of the element, or the previous sibling matching
C<$optional_condition>

=item next_sibling  ($optional_condition)

Return the next sibling of the element, or the first one matching 
C<$optional_condition>.

=item next_elt     ($optional_elt, $optional_condition)

Return the next elt (optionally matching C<$optional_condition>) of the element. This 
is defined as the next element which opens after the current element opens.
Which usually means the first child of the element.
Counter-intuitive as it might look this allows you to loop through the
whole document by starting from the root.

The C<$optional_elt> is the root of a subtree. When the C<next_elt> is out of the
subtree then the method returns undef. You can then walk a sub tree with:

  my $elt= $subtree_root;
  while( $elt= $elt->next_elt( $subtree_root)
    { # insert processing code here
    }

=item prev_elt     ($optional_condition)

Return the previous elt (optionally matching C<$optional_condition>) of the
element. This is the first element which opens before the current one.
It is usually either the last descendant of the previous sibling or
simply the parent

=item next_n_elt   ($offset, $optional_condition)

Return the C<$offset>-th element that matches the C<$optional_condition> 

=item children     ($optional_condition)

Return the list of children (optionally which matches C<$optional_condition>) of 
the element. The list is in document order.

=item children_count ($optional_condition)

Return the number of children of the element (optionally which matches 
C<$optional_condition>)

=item children_text ($optional_condition)

Return an array containing the text of children of the element (optionally 
which matches C<$optional_condition>)

=item children_trimmed_text ($optional_condition)

Return an array containing the trimmed text of children of the element (optionally 
which matches C<$optional_condition>)


=item children_copy ($optional_condition)

Return a list of elements that are copies of the children of the element, 
optionally which matches C<$optional_condition>

=item descendants     ($optional_condition)

Return the list of all descendants (optionally which matches 
C<$optional_condition>) of the element. This is the equivalent of the 
C<getElementsByTagName> of the DOM (by the way, if you are really a DOM 
addict, you can use C<getElementsByTagName> instead)

=item getElementsByTagName ($optional_condition)

Same as C<L<descendants>>

=item find_by_tag_name ($optional_condition)

Same as C<L<descendants>>

=item descendants_or_self ($optional_condition)

Same as C<L<descendants>> except that the element itself is included in the list
if it matches the C<$optional_condition> 

=item first_descendant  ($optional_condition)

Return the first descendant of the element that matches the condition  

=item last_descendant  ($optional_condition)

Return the last descendant of the element that matches the condition  

=item ancestors    ($optional_condition)

Return the list of ancestors (optionally matching C<$optional_condition>) of the 
element.  The list is ordered from the innermost ancestor to the outtermost one

NOTE: the element itself is not part of the list, in order to include it 
you will have to use ancestors_or_self

=item ancestors_or_self     ($optional_condition)

Return the list of ancestors (optionally matching C<$optional_condition>) of the 
element, including the element (if it matches the condition>).  
The list is ordered from the innermost ancestor to the outtermost one

=item passes ($condition)

Return the element if it passes the C<$condition> 

=item att          ($att)

Return the value of attribute C<$att> or C<undef>

=item set_att      ($att, $att_value)

Set the attribute of the element to the given value

You can actually set several attributes this way:

  $elt->set_att( att1 => "val1", att2 => "val2");

=item del_att      ($att)

Delete the attribute for the element

You can actually delete several attributes at once:

  $elt->del_att( 'att1', 'att2', 'att3');

=item cut

Cut the element from the tree. The element still exists, it can be copied
or pasted somewhere else, it is just not attached to the tree anymore.

=item cut_children ($optional_condition)

Cut all the children of the element (or all of those which satisfy the
C<$optional_condition>).

Return the list of children 

=item copy        ($elt)

Return a copy of the element. The copy is a "deep" copy: all sub elements of 
the element are duplicated.

=item paste       ($optional_position, $ref)

Paste a (previously C<cut> or newly generated) element. Die if the element
already belongs to a tree.

Note that the calling element is pasted:

  $child->paste( first_child => $existing_parent);
  $new_sibling->paste( after => $this_sibling_is_already_in_the_tree);

or

  my $new_elt= XML::Twig::Elt->new( tag => $content);
  $new_elt->paste( $position => $existing_elt);

Example:

  my $t= XML::Twig->new->parse( 'doc.xml')
  my $toc= $t->root->new( 'toc');
  $toc->paste( $t->root); # $toc is pasted as first child of the root 
  foreach my $title ($t->findnodes( '/doc/section/title'))
    { my $title_toc= $title->copy;
      # paste $title_toc as the last child of toc
      $title_toc->paste( last_child => $toc) 
    }

Position options:

=over 4

=item first_child (default)

The element is pasted as the first child of C<$ref>

=item last_child

The element is pasted as the last child of C<$ref>

=item before

The element is pasted before C<$ref>, as its previous sibling.

=item after

The element is pasted after C<$ref>, as its next sibling.

=item within

In this case an extra argument, C<$offset>, should be supplied. The element
will be pasted in the reference element (or in its first text child) at the
given offset. To achieve this the reference element will be split at the 
offset.

=back

Note that you can call directly the underlying method:

=over 4

=item paste_before

=item paste_after

=item paste_first_child

=item paste_last_child

=item paste_within

=back

=item move       ($optional_position, $ref)

Move an element in the tree.
This is just a C<cut> then a C<paste>.  The syntax is the same as C<paste>.

=item replace       ($ref)

Replaces an element in the tree. Sometimes it is just not possible toC<cut> 
an element then C<paste> another in its place, so C<replace> comes in handy.
The calling element replaces C<$ref>.

=item replace_with   (@elts)

Replaces the calling element with one or more elements 

=item delete

Cut the element and frees the memory.

=item prefix       ($text, $optional_option)

Add a prefix to an element. If the element is a C<PCDATA> element the text
is added to the pcdata, if the elements first child is a C<PCDATA> then the
text is added to it's pcdata, otherwise a new C<PCDATA> element is created 
and pasted as the first child of the element.

If the option is C<asis> then the prefix is added asis: it is created in
a separate C<PCDATA> element with an C<asis> property. You can then write:

  $elt1->prefix( '<b>', 'asis');

to create a C<< <b> >> in the output of C<print>.

=item suffix       ($text, $optional_option)

Add a suffix to an element. If the element is a C<PCDATA> element the text
is added to the pcdata, if the elements last child is a C<PCDATA> then the
text is added to it's pcdata, otherwise a new PCDATA element is created 
and pasted as the last child of the element.

If the option is C<asis> then the suffix is added asis: it is created in
a separate C<PCDATA> element with an C<asis> property. You can then write:

  $elt2->suffix( '</b>', 'asis');

=item trim

Trim the element in-place: spaces at the beginning and at the end of the element
are discarded and multiple spaces within the element (or its descendants) are 
replaced by a single space.

Note that in some cases you can still end up with multiple spaces, if they are
split between several elements:

  <doc>  text <b>  hah! </b>  yep</doc>

gets trimmed to

  <doc>text <b> hah! </b> yep</doc>

This is somewhere in between a bug and a feature.


=item simplify (%options)

Return a data structure suspiciously similar to XML::Simple's. Options are
identical to XMLin options, see XML::Simple doc for more details (or use
DATA::dumper or YAML to dump the data structure)

=over 4

=item content_key

=item forcearray 
                             
=item keyattr 

=item noattr 

=item normalize_space

aka normalise_space

=item variables (%var_hash)

%var_hash is a hash { name => value }

This option allows variables in the XML to be expanded when the file is read. (there is no facility for putting the variable names back if you regenerate XML using XMLout).

A 'variable' is any text of the form ${name} (or $name) which occurs in an attribute value or in the text content of an element. If 'name' matches a key in the supplied hashref, ${name} will be replaced with the corresponding value from the hashref. If no matching key is found, the variable will not be replaced. 

=item var_att ($attribute_name)

This option gives the name of an attribute that will be used to create 
variables in the XML:

  <dirs>
    <dir name="prefix">/usr/local</dir>
    <dir name="exec_prefix">$prefix/bin</dir>
  </dirs>

use C<< var => 'name' >> to get $prefix replaced by /usr/local in the
generated data structure  

By default variables are captured by the following regexp: /$(\w+)/
    
=item var_regexp (regexp)

This option changes the regexp used to capture variables. The variable
name should be in $1

=item group_tags { grouping tag => grouped tag, grouping tag 2 => grouped tag 2...}

Option used to simplify the structure: elements listed will not be used.
Their children will be, they will be considered children of the element
parent.

If the element is:

  <config host="laptop.xmltwig.com">
    <server>localhost</server>
    <dirs>
      <dir name="base">/home/mrodrigu/standards</dir>
      <dir name="tools">$base/tools</dir>
    </dirs>
    <templates>
      <template name="std_def">std_def.templ</template>
      <template name="dummy">dummy</template>
    </templates>
  </config>

Then calling simplify with C<< group_tags => { dirs => 'dir',
templates => 'template'} >>
makes the data structure be exactly as if the start and end tags for C<dirs> and
C<templates> were not there.

A YAML dump of the structure 

  base: '/home/mrodrigu/standards'
  host: laptop.xmltwig.com
  server: localhost
  template:
    - std_def.templ
    - dummy.templ
  tools: '$base/tools'


=back

=item split_at        ($offset)

Split a text (C<PCDATA> or C<CDATA>) element in 2 at C<$offset>, the original
element now holds the first part of the string and a new element holds the
right part. The new element is returned

If the element is not a text element then the first text child of the element
is split

=item split        ( $optional_regexp, $tag1, $atts1, $tag2, $atts2...)

Split the text descendants of an element in place, the text is split using 
the C<$regexp>, if the regexp includes () then the matched separators will be 
wrapped in elements.  C<$1> is wrapped in $tag1, with attributes C<$atts1> if
C<$atts1> is given (as a hashref), C<$2> is wrapped in $tag2... 

if $elt is C<< <p>tati tata <b>tutu tati titi</b> tata tati tata</p> >>

  $elt->split( qr/(ta)ti/, 'foo', {type => 'toto'} )

will change $elt to

  <p><foo type="toto">ta</foo> tata <b>tutu <foo type="toto">ta</foo>
      titi</b> tata <foo type="toto">ta</foo> tata</p> 

The regexp can be passed either as a string or as C<qr//> (perl 5.005 and 
later), it defaults to \s+ just as the C<split> built-in (but this would be 
quite a useless behaviour without the C<$optional_tag> parameter)

C<$optional_tag> defaults to PCDATA or CDATA, depending on the initial element
type

The list of descendants is returned (including un-touched original elements 
and newly created ones)

=item mark        ( $regexp, $optional_tag, $optional_attribute_ref)

This method behaves exactly as L<split|split>, except only the newly created 
elements are returned

=item wrap_children ( $regexp_string, $tag, $optional_att, $otional_value)

Wrap the children of the element that match the regexp in an element C<$tag>.
If C<$optional_att> and C<$optional_value> are passed then the new element will
have an attribute C<$optional_att> with a value C<$optional_value>.

Note that elements might get extra C<id> attributes in the process. See L<add_id>.
Use L<strip_att> to remove unwanted id's. 

Here is an example:

If the element C<$elt> has the following content:

  <elt>
   <p>para 1</p>
   <l_l1_1>list 1 item 1 para 1</l_l1_1>
     <l_l1>list 1 item 1 para 2</l_l1>
   <l_l1_n>list 1 item 2 para 1 (only para)</l_l1_n>
   <l_l1_n>list 1 item 3 para 1</l_l1_n>
     <l_l1>list 1 item 3 para 2</l_l1>
     <l_l1>list 1 item 3 para 3</l_l1>
   <l_l1_1>list 2 item 1 para 1</l_l1_1>
     <l_l1>list 2 item 1 para 2</l_l1>
   <l_l1_n>list 2 item 2 para 1 (only para)</l_l1_n>
   <l_l1_n>list 2 item 3 para 1</l_l1_n>
     <l_l1>list 2 item 3 para 2</l_l1>
     <l_l1>list 2 item 3 para 3</l_l1>
  </elt>

Then the code

  $elt->wrap_children( q{<l_l1_1><l_l1>*} , li => { type => "ul1" });
  $elt->wrap_children( q{<l_l1_n><l_l1>*} , li => { type => "ul" });

  $elt->wrap_children( q{<li type="ul1"><li type="ul">+}, "ul");
  $elt->strip_att( 'id');
  $elt->strip_att( 'type');
  $elt->print;

will output:

  <elt>
     <p>para 1</p>
     <ul>
       <li>
         <l_l1_1>list 1 item 1 para 1</l_l1_1>
         <l_l1>list 1 item 1 para 2</l_l1>
       </li>
       <li>
         <l_l1_n>list 1 item 2 para 1 (only para)</l_l1_n>
       </li>
       <li>
         <l_l1_n>list 1 item 3 para 1</l_l1_n>
         <l_l1>list 1 item 3 para 2</l_l1>
         <l_l1>list 1 item 3 para 3</l_l1>
       </li>
     </ul>
     <ul>
       <li>
         <l_l1_1>list 2 item 1 para 1</l_l1_1>
         <l_l1>list 2 item 1 para 2</l_l1>
       </li>
       <li>
         <l_l1_n>list 2 item 2 para 1 (only para)</l_l1_n>
       </li>
       <li>
         <l_l1_n>list 2 item 3 para 1</l_l1_n>
         <l_l1>list 2 item 3 para 2</l_l1>
         <l_l1>list 2 item 3 para 3</l_l1>
       </li>
     </ul>
  </elt>

=item subs_text ($regexp, $replace)

subs_text does text substitution, similar to perl's C< s///> operator.

C<$regexp> must be a perl regexp, created with the C<qr> operatot.

C<$replace> can include C<$1, $2>... from the C<$regexp>. It can also be
used to create element and entities, by using 
C<< &elt( tag => { att => val }, text) >> (similar syntax as C<L<new>>) and
C<< &ent( name) >>.

Here is a rather complex example:

  $elt->subs_text( qr{(?<!do not )link to (http://([^\s,]*))},
                   'see &elt( a =>{ href => $1 }, $2)'
                 );

This will replace text like I<link to http://www.xmltwig.com> by 
I<< see <a href="www.xmltwig.com">www.xmltwig.com</a> >>, but not
I<do not link to...>

Generating entities (here replacing spaces with &nbsp;):

  $elt->subs_text( qr{ }, '&ent( "&nbsp;")');

or, using a variable:

  my $ent="&nbsp;";
  $elt->subs_text( qr{ }, "&ent( '$ent')");

Note that the substitution is always global, as in using the C<g> modifier
in a perl substitution, and that it is performed on all text descendants
of the element.

Bug warning: in the C<$regexp>, you can only use C<\1>, C<\2>... if the replacement
expression does not include elements or attributes. eg

  t->subs_text( qr/((t[aiou])\2)/, '$2');             # ok, replaces toto, tata, titi, tutu by to, ta, ti, tu
  t->subs_text( qr/((t[aiou])\2)/, '&elt(p => $1)' ); # NOK, does not find toto...

=item add_id

Add an id to the element.

The id is an attribute (C<id> by default, see the C<id> option for XML::Twig
C<new> to change it. Use an id starting with C<#> to get an id that's not 
output by L<print>, L<flush> or L<sprint>) that allows you to use the
L<elt_id> method to get the element easily.

=item set_id_seed ($prefix)

by default the id generated by C<L<add_id>> is C<< twig_id_<nnnn> >>, 
C<set_id_seed> changes the prefix to C<$prefix> and resets the number
to 1

=item strip_att ($att)

Remove the attribute C<$att> from all descendants of the element (including 
the element)

=item change_att_name ($old_name, $new_name)

Change the name of the attribute from C<$old_name> to C<$new_name>. If there is no
attribute C<$old_name> nothing happens.

=item sort_children_on_value( %options)

Sort the children of the element in place according to their text.
All children are sorted. 

Return the element, with its children sorted.


L<%options> are

  type  : numeric |  alpha     (default: alpha)
  order : normal  |  reverse   (default: normal)

Return the element, with its children sorted


=item sort_children_on_att ($att, %options)

Sort the children of the  element in place according to attribute C<$att>. 
C<%options> are the same as for L<C<sort_children_on_value>>

Return the element.


=item sort_children_on_field ($tag, %options)

Sort the children of the element in place, according to the field C<$tag> (the 
text of the first child of the child with this tag). C<%options> are the same
as for L<C<sort_children_on_value>>.

Return the element, with its children sorted


=item sort_children( $get_key, %options) 

Sort the children of the element in place. The C<$get_key> argument is
a reference to a function that returns the sort key when passed an element.

For example:

  $elt->sort_children( sub { $_[0]->{'att'}->{"nb"} + $_[0]->text }, 
                       type => 'numeric', order => 'reverse'
                     );

=item field_to_att ($cond, $att)

Turn the text of the first sub-element matched by C<$cond> into the value of 
attribute C<$att> of the element. If C<$att> is ommited then C<$cond> is used 
as the name of the attribute, which makes sense only if C<$cond> is a valid
element (and attribute) name.

The sub-element is then cut.

=item att_to_field ($att, $tag)

Take the value of attribute C<$att> and create a sub-element C<$tag> as first
child of the element. If C<$tag> is ommited then C<$att> is used as the name of
the sub-element. 


=item get_xpath  ($xpath, $optional_offset)

Return a list of elements satisfying the C<$xpath>. C<$xpath> is an XPATH-like 
expression.

A subset of the XPATH abbreviated syntax is covered:

  tag
  tag[1] (or any other positive number)
  tag[last()]
  tag[@att] (the attribute exists for the element)
  tag[@att="val"]
  tag[@att=~ /regexp/]
  tag[att1="val1" and att2="val2"]
  tag[att1="val1" or att2="val2"]
  tag[string()="toto"] (returns tag elements which text (as per the text method) 
                       is toto)
  tag[string()=~/regexp/] (returns tag elements which text (as per the text 
                          method) matches regexp)
  expressions can start with / (search starts at the document root)
  expressions can start with . (search starts at the current element)
  // can be used to get all descendants instead of just direct children
  * matches any tag
  
So the following examples from the 
F<XPath recommendationL<http://www.w3.org/TR/xpath.html#path-abbrev>> work:

  para selects the para element children of the context node
  * selects all element children of the context node
  para[1] selects the first para child of the context node
  para[last()] selects the last para child of the context node
  */para selects all para grandchildren of the context node
  /doc/chapter[5]/section[2] selects the second section of the fifth chapter 
     of the doc 
  chapter//para selects the para element descendants of the chapter element 
     children of the context node
  //para selects all the para descendants of the document root and thus selects
     all para elements in the same document as the context node
  //olist/item selects all the item elements in the same document as the 
     context node that have an olist parent
  .//para selects the para element descendants of the context node
  .. selects the parent of the context node
  para[@type="warning"] selects all para children of the context node that have
     a type attribute with value warning 
  employee[@secretary and @assistant] selects all the employee children of the
     context node that have both a secretary attribute and an assistant 
     attribute


The elements will be returned in the document order.

If C<$optional_offset> is used then only one element will be returned, the one 
with the appropriate offset in the list, starting at 0

Quoting and interpolating variables can be a pain when the Perl syntax and the 
XPATH syntax collide, so here are some more examples to get you started:

  my $p1= "p1";
  my $p2= "p2";
  my @res= $t->get_xpath( "p[string( '$p1') or string( '$p2')]");

  my $a= "a1";
  my @res= $t->get_xpath( "//*[@att=\"$a\"]);

  my $val= "a1";
  my $exp= "//p[ \@att='$val']"; # you need to use \@ or you will get a warning
  my @res= $t->get_xpath( $exp);

Note that the only supported regexps delimiters are / and that you must 
backslash all / in regexps AND in regular strings.

XML::Twig does not provide natively full XPATH support, but you can use 
XML::Twig does not provide natively full XPATH support, but you can use 
C<L<XML::Twig::XPath>> to get C<findnodes> to use C<XML::XPath> as the
XPath engine, with full coverage of the spec.

C<L<XML::Twig::XPath>> to get C<findnodes> to use C<XML::XPath> as the
XPath engine, with full coverage of the spec.

=item find_nodes

same asC<get_xpath> 

=item findnodes

same asC<get_xpath> 


=item text

Return a string consisting of all the C<PCDATA> and C<CDATA> in an element, 
without any tags. The text is not XML-escaped: base entities such as C<&> 
and C<< < >> are not escaped.

=item trimmed_text

Same as C<text> except that the text is trimmed: leading and trailing spaces
are discarded, consecutive spaces are collapsed

=item set_text        ($string)

Set the text for the element: if the element is a C<PCDATA>, just set its
text, otherwise cut all the children of the element and create a single
C<PCDATA> child for it, which holds the text.

=item insert         ($tag1, [$optional_atts1], $tag2, [$optional_atts2],...)

For each tag in the list inserts an element C<$tag> as the only child of the 
element.  The element gets the optional attributes inC<< $optional_atts<n>. >> 
All children of the element are set as children of the new element.
The upper level element is returned.

  $p->insert( table => { border=> 1}, 'tr', 'td') 

put C<$p> in a table with a visible border, a single C<tr> and a single C<td> 
and return the C<table> element:

  <p><table border="1"><tr><td>original content of p</td></tr></table></p>

=item wrap_in        (@tag)

Wrap elements C<$tag> as the successive ancestors of the element, returns the 
new element.
$elt->wrap_in( 'td', 'tr', 'table') wraps the element as a single cell in a 
table for example.

=item insert_new_elt ($opt_position, $tag, $opt_atts_hashref, @opt_content)

Combines a C<L<new|new> > and a C<L<paste|paste> >: creates a new element using 
C<$tag>, C<$opt_atts_hashref >and C<@opt_content> which are arguments similar 
to those for C<new>, then paste it, using C<$opt_position> or C<'first_child'>,
relative to C<$elt>.

Return the newly created element

=item erase

Erase the element: the element is deleted and all of its children are
pasted in its place.

=item set_content    ( $optional_atts, @list_of_elt_and_strings)
                     ( $optional_atts, '#EMPTY')

Set the content for the element, from a list of strings and
elements.  Cuts all the element children, then pastes the list
elements as the children.  This method will create a C<PCDATA> element
for any strings in the list.

The C<$optional_atts> argument is the ref of a hash of attributes. If this
argument is used then the previous attributes are deleted, otherwise they
are left untouched. 

B<WARNING>: if you rely on ID's then you will have to set the id yourself. At
this point the element does not belong to a twig yet, so the ID attribute
is not known so it won't be strored in the ID list.

A content of 'C<#EMPTY>' creates an empty element;

=item namespace ($optional_prefix)

Return the URI of the namespace that C<$optional_prefix> or the element name
belongs to. If the name doesn't belong to any namespace, C<undef> is returned.

=item local_name

Return the local name (without the prefix) for the element

=item ns_prefix

Return the namespace prefix for the element

=item current_ns_prefixes

Returna list of namespace prefixes valid for the element. The order of the
prefixes in the list has no meaning. If the default namespace is currently 
bound, '' appears in the list.


=item inherit_att  ($att, @optional_tag_list)

Return the value of an attribute inherited from parent tags. The value
returned is found by looking for the attribute in the element then in turn
in each of its ancestors. If the C<@optional_tag_list> is supplied only those
ancestors whose tag is in the list will be checked. 

=item all_children_are ($optional_condition)

return 1 if all children of the element pass the C<$optional_condition>, 
0 otherwise

=item level       ($optional_condition)

Return the depth of the element in the twig (root is 0).
If C<$optional_condition> is given then only ancestors that match the condition are 
counted.
 
B<WARNING>: in a tree created using the C<twig_roots> option this will not return
the level in the document tree, level 0 will be the document root, level 1 
will be the C<twig_roots> elements. During the parsing (in a C<twig_handler>)
you can use the C<depth> method on the twig object to get the real parsing depth.

=item in           ($potential_parent)

Return true if the element is in the potential_parent (C<$potential_parent> is 
an element)

=item in_context   ($cond, $optional_level)

Return true if the element is included in an element which passes C<$cond>
optionally within C<$optional_level> levels. The returned value is the 
including element.

=item pcdata

Return the text of a C<PCDATA> element or C<undef> if the element is not 
C<PCDATA>.

=item pcdata_xml_string

Return the text of a PCDATA element or undef if the element is not PCDATA. 
The text is "XML-escaped" ('&' and '<' are replaced by '&amp;' and '&lt;')

=item set_pcdata     ($text)

Set the text of a C<PCDATA> element. 

=item append_pcdata  ($text)

Add the text at the end of a C<PCDATA> element.

=item is_cdata

Return 1 if the element is a C<CDATA> element, returns 0 otherwise.

=item is_text

Return 1 if the element is a C<CDATA> or C<PCDATA> element, returns 0 otherwise.

=item cdata

Return the text of a C<CDATA> element or C<undef> if the element is not 
C<CDATA>.

=item cdata_string

Return the XML string of a C<CDATA> element, including the opening and
closing markers.

=item set_cdata     ($text)

Set the text of a C<CDATA> element. 

=item append_cdata  ($text)

Add the text at the end of a C<CDATA> element.

=item remove_cdata

Turns all C<CDATA> sections in the element into regular C<PCDATA> elements. This is useful
when converting XML to HTML, as browsers do not support CDATA sections. 

=item extra_data 

Return the extra_data (comments and PI's) attached to an element

=item set_extra_data     ($extra_data)

Set the extra_data (comments and PI's) attached to an element

=item append_extra_data  ($extra_data)

Append extra_data to the existing extra_data before the element (if no
previous extra_data exists then it is created)

=item set_asis

Set a property of the element that causes it to be output without being XML
escaped by the print functions: if it contains C<< a < b >> it will be output
as such and not as C<< a &lt; b >>. This can be useful to create text elements
that will be output as markup. Note that all C<PCDATA> descendants of the 
element are also marked as having the property (they are the ones taht are
actually impacted by the change).

If the element is a C<CDATA> element it will also be output asis, without the
C<CDATA> markers. The same goes for any C<CDATA> descendant of the element

=item set_not_asis

Unsets the C<asis> property for the element and its text descendants.

=item is_asis

Return the C<asis> property status of the element ( 1 or C<undef>)

=item closed                   

Return true if the element has been closed. Might be usefull if you are
somewhere in the tree, during the parse, and have no idea whether a parent
element is completely loaded or not.

=item get_type

Return the type of the element: 'C<#ELT>' for "real" elements, or 'C<#PCDATA>',
'C<#CDATA>', 'C<#COMMENT>', 'C<#ENT>', 'C<#PI>'

=item is_elt

Return the tag if the element is a "real" element, or 0 if it is C<PCDATA>, 
C<CDATA>...

=item contains_only_text

Return 1 if the element does not contain any other "real" element

=item contains_only ($exp)

Return the list of children if all children of the element match
the expression C<$exp> 

  if( $para->contains_only( 'tt')) { ... }

=item contains_a_single ($exp)

If the element contains a single child that matches the expression C<$exp>
returns that element. Otherwise returns 0.

=item is_field

same as C<contains_only_text> 

=item is_pcdata

Return 1 if the element is a C<PCDATA> element, returns 0 otherwise.

=item is_ent

Return 1 if the element is an entity (an unexpanded entity) element, 
return 0 otherwise.

=item is_empty

Return 1 if the element is empty, 0 otherwise

=item set_empty

Flags the element as empty. No further check is made, so if the element
is actually not empty the output will be messed. The only effect of this 
method is that the output will be C<< <tag att="value""/> >>.

=item set_not_empty

Flags the element as not empty. if it is actually empty then the element will
be output as C<< <tag att="value""></tag> >>

=item is_pi

Return 1 if the element is a processing instruction (C<#PI>) element,
return 0 otherwise.

=item target

Return the target of a processing instruction

=item set_target ($target)

Set the target of a processing instruction

=item data

Return the data part of a processing instruction

=item set_data ($data)

Set the data of a processing instruction

=item set_pi ($target, $data)

Set the target and data of a processing instruction

=item pi_string

Return the string form of a processing instruction
(C<< <?target data?> >>)

=item is_comment

Return 1 if the element is a comment (C<#COMMENT>) element,
return 0 otherwise.

=item set_comment ($comment_text)

Set the text for a comment

=item comment

Return the content of a comment (just the text, not the C<< <!-- >>
and C<< --> >>)

=item comment_string 

Return the XML string for a comment (C<< <!-- comment --> >>)

=item set_ent ($entity)

Set an (non-expanded) entity (C<#ENT>). C<$entity>) is the entity
text (C<&ent;>)

=item ent

Return the entity for an entity (C<#ENT>) element (C<&ent;>)

=item ent_name

Return the entity name for an entity (C<#ENT>) element (C<ent>)

=item ent_string

Return the entity, either expanded if the expanded version is available,
or non-expanded (C<&ent;>) otherwise

=item child ($offset, $optional_condition)

Return the C<$offset>-th child of the element, optionally the C<$offset>-th 
child that matches C<$optional_condition>. The children are treated as a list, so 
C<< $elt->child( 0) >> is the first child, while C<< $elt->child( -1) >> is 
the last child.

=item child_text ($offset, $optional_condition)

Return the text of a child or C<undef> if the sibling does not exist. Arguments
are the same as child.

=item last_child    ($optional_condition)

Return the last child of the element, or the last child matching 
C<$optional_condition> (ie the last of the element children matching
the condition).

=item last_child_text   ($optional_condition)

Same as C<first_child_text> but for the last child.

=item sibling  ($offset, $optional_condition)

Return the next or previous C<$offset>-th sibling of the element, or the 
C<$offset>-th one matching C<$optional_condition>. If C<$offset> is negative then a 
previous sibling is returned, if $offset is positive then  a next sibling is 
returned. C<$offset=0> returns the element if there is no condition or
if the element matches the condition>, C<undef> otherwise.

=item sibling_text ($offset, $optional_condition)

Return the text of a sibling or C<undef> if the sibling does not exist. 
Arguments are the same as C<sibling>.

=item prev_siblings ($optional_condition)

Return the list of previous siblings (optionaly matching C<$optional_condition>)
for the element. The elements are ordered in document order.

=item next_siblings ($optional_condition)

Return the list of siblings (optionaly matching C<$optional_condition>)
following the element. The elements are ordered in document order.

=item pos ($optional_condition)

Return the position of the element in the children list. The first child has a
position of 1 (as in XPath).

If the C<$optional_condition> is given then only siblings that match the condition 
are counted. If the element itself does not match the  condition then
0 is returned.

=item atts

Return a hash ref containing the element attributes

=item set_atts      ({att1=>$att1_val, att2=> $att2_val... })

Set the element attributes with the hash ref supplied as the argument

=item del_atts

Deletes all the element attributes.

=item att_nb

Return the number of attributes for the element

=item has_atts

Return true if the element has attributes (in fact return the number of
attributes, thus being an alias to C<L<att_nb>>

=item has_no_atts

Return true if the element has no attributes, false (0) otherwise

=item att_names

return a list of the attribute names for the element

=item att_xml_string ($att, $optional_quote)

Return the attribute value, where '&', '<' and $quote (" by default)
are XML-escaped

if C<$optional_quote> is passed then it is used as the quote.

=item set_id       ($id)

Set the C<id> attribute of the element to the value.
See C<L<elt_id|elt_id> > to change the id attribute name

=item id

Gets the id attribute value

=item del_id       ($id)

Deletes the C<id> attribute of the element and remove it from the id list
for the document

=item class

Return the C<class> attribute for the element (methods on the C<class>
attribute are quite convenient when dealing with XHTML, or plain XML that
will eventually be displayed using CSS)

=item set_class ($class)

Set the C<class> attribute for the element to C<$class>

=item add_to_class ($class)

Add C<$class> to the element C<class> attribute: the new class is added
only if it is not already present. Note that classes are sorted alphabetically,
so the C<class> attribute can be changed even if the class is already there

=item att_to_class ($att)

Set the C<class> attribute to the value of attribute C<$att>

=item add_att_to_class ($att)

Add the value of attribute C<$att> to the C<class> attribute of the element

=item move_att_to_class ($att)

Add the value of attribute C<$att> to the C<class> attribute of the element
and delete the attribute

=item tag_to_class

Set the C<class> attribute of the element to the element tag

=item add_tag_to_class

Add the element tag to its C<class> attribute

=item set_tag_class ($new_tag)

Add the element tag to its C<class> attribute and sets the tag to C<$new_tag>

=item in_class ($class)

Return true (C<1>) if the element is in the class C<$class> (if C<$class> is
one of the tokens in the element C<class> attribute)

=item DESTROY

Frees the element from memory.

=item start_tag

Return the string for the start tag for the element, including 
the C<< /> >> at the end of an empty element tag

=item end_tag

Return the string for the end tag of an element.  For an empty
element, this returns the empty string ('').

=item xml_string

Equivalent to C<< $elt->sprint( 1) >>, returns the string for the entire 
element, excluding the element's tags (but nested element tags are present)

=item xml_text 

Return the text of the element, encoded (and processed by the current 
C<L<output_filter>> or C<L<output_encoding>> options, without any tag.

=item set_pretty_print ($style)

Set the pretty print method, amongst 'C<none>' (default), 'C<nsgmls>', 
'C<nice>', 'C<indented>', 'C<record>' and 'C<record_c>'

pretty_print styles:

=over 4

=item none

the default, no C<\n> is used

=item nsgmls

nsgmls style, with C<\n> added within tags

=item nice

adds C<\n> wherever possible (NOT SAFE, can lead to invalid XML)

=item indented

same as C<nice> plus indents elements (NOT SAFE, can lead to invalid XML) 

=item record

table-oriented pretty print, one field per line 

=item record_c

table-oriented pretty print, more compact than C<record>, one record per line 

=back

=item set_empty_tag_style ($style)

Set the method to output empty tags, amongst 'C<normal>' (default), 'C<html>',
and 'C<expand>', 

=item set_remove_cdata  ($flag)

set (or unset) the flag that forces the twig to output CDATA sections as 
regular (escaped) PCDATA


=item set_indent ($string)

Set the indentation for the indented pretty print style (default is 2 spaces)

=item set_quote ($quote)

Set the quotes used for attributes. can be 'C<double>' (default) or 'C<single>'

=item cmp       ($elt)

  Compare the order of the 2 elements in a twig.

  C<$a> is the <A>..</A> element, C<$b> is the <B>...</B> element
  
  document                        $a->cmp( $b)
  <A> ... </A> ... <B>  ... </B>     -1
  <A> ... <B>  ... </B> ... </A>     -1
  <B> ... </B> ... <A>  ... </A>      1
  <B> ... <A>  ... </A> ... </B>      1
   $a == $b                           0
   $a and $b not in the same tree   undef

=item before       ($elt)

Return 1 if C<$elt> starts before the element, 0 otherwise. If the 2 elements 
are not in the same twig then return C<undef>.

    if( $a->cmp( $b) == -1) { return 1; } else { return 0; }

=item after       ($elt)

Return 1 if $elt starts after the element, 0 otherwise. If the 2 elements 
are not in the same twig then return C<undef>.

    if( $a->cmp( $b) == -1) { return 1; } else { return 0; }

=item other comparison methods

=over 4

=item lt

=item le

=item gt

=item ge

=back

=item path

Return the element context in a form similar to XPath's short
form: 'C</root/tag1/../tag>'

=item xpath

Return a unique XPath expression that can be used to find the element
again. 

It looks like C</doc/sect[3]/title>: unique elements do not have an index,
the others do.

=item private methods

Low-level methods on the twig:

=over 4

=item set_parent        ($parent)

=item set_first_child   ($first_child)

=item set_last_child    ($last_child)

=item set_prev_sibling  ($prev_sibling)

=item set_next_sibling  ($next_sibling)

=item set_twig_current

=item del_twig_current

=item twig_current

=item flush

This method should NOT be used, always flush the twig, not an element.

=item contains_text

=back

Those methods should not be used, unless of course you find some creative 
and interesting, not to mention useful, ways to do it.

=back

=head2 cond

Most of the navigation functions accept a condition as an optional argument
The first element (or all elements for C<L<children|children> > or 
C<L<ancestors|ancestors> >) that passes the condition is returned.

The condition is a single step of an XPath expression using the XPath subset
defined by C<L<get_xpath>>. Additional conditions are:

The condition can be 

=over 4

=item #ELT

return a "real" element (not a PCDATA, CDATA, comment or pi element) 

=item #TEXT

return a PCDATA or CDATA element

=item regular expression

return an element whose tag matches the regexp. The regexp has to be created 
with C<qr//> (hence this is available only on perl 5.005 and above)

=item code reference

applies the code, passing the current element as argument, if the code returns
true then the element is returned, if it returns false then the code is applied
to the next candidate.

=back

=head2 XML::Twig::XPath

XML::Twig implements a subset of XPath through the C<L<get_xpath>> method. 

If you want to use the whole XPath power, then you can use C<XML::Twig::XPath>
instead. In this case C<XML::Twig> uses C<XML::XPath> to execute XPath queries.
You will of course need C<XML::XPath> installed to be able to use C<XML::Twig::XPath>.

See L<XML::XPath> for more information.

The methods you can use are:

=over 4

=item findnodes              ($path)

return a list of nodes found by C<$path>.

=item findnodes_as_string    ($path)

return the nodes found reproduced as XML. The result is not guaranteed
to be valid XML though.

=item findvalue              ($path)

return the concatenation of the text content of the result nodes

=back

In order for C<XML::XPath> to be used as the XPath engine the following methods
are included in C<XML::Twig>:

in XML::Twig

=over 4

=item getRootNode

=item getParentNode

=item getChildNodes 

=back

in XML::Twig::Elt

=over 4

=item string_value

=item toString

=item getName

=item getRootNode

=item getNextSibling

=item getPreviousSibling

=item isElementNode

=item isTextNode

=item isPI

=item isPINode

=item isProcessingInstructionNode

=item isComment

=item isCommentNode

=item getTarget 

=item getChildNodes 

=item getElementById

=back

=head2 XML::Twig::XPath::Elt

The methods you can use are the same as on C<XML::Twig::XPath> elements:

=over 4

=item findnodes              ($path)

return a list of nodes found by C<$path>.

=item findnodes_as_string    ($path)

return the nodes found reproduced as XML. The result is not guaranteed
to be valid XML though.

=item findvalue              ($path)

return the concatenation of the text content of the result nodes

=back


=head2 XML::Twig::Entity_list

=over 4

=item new

Create an entity list.

=item add         ($ent)

Add an entity to an entity list.

=item add_new_ent ($name, $val, $sysid, $pubid, $ndata)

Create a new entity and add it to the entity list

=item delete     ($ent or $tag).

Delete an entity (defined by its name or by the Entity object)
from the list.

=item print      ($optional_filehandle)

Print the entity list.

=item list

Return the list as an array

=back


=head2 XML::Twig::Entity

=over 4

=item new        ($name, $val, $sysid, $pubid, $ndata)

Same arguments as the Entity handler for XML::Parser.

=item print       ($optional_filehandle)

Print an entity declaration.

=item name 

Return the name of the entity

=item val  

Return the value of the entity

=item sysid

Return the system id for the entity (for NDATA entities)

=item pubid

Return the public id for the entity (for NDATA entities)

=item ndata

Return true if the entity is an NDATA entity

=item text

Return the entity declaration text.

=back


=head1 EXAMPLES

Additional examples (and a complete tutorial) can be found  on the
F<XML::Twig PageL<http://www.xmltwig.com/xmltwig/>>

To figure out what flush does call the following script with an
XML file and an element name as arguments

  use XML::Twig;

  my ($file, $elt)= @ARGV;
  my $t= XML::Twig->new( twig_handlers => 
      { $elt => sub {$_[0]->flush; print "\n[flushed here]\n";} });
  $t->parsefile( $file, ErrorContext => 2);
  $t->flush;
  print "\n";


=head1 NOTES

=head2 Subclassing XML::Twig

Useful methods:

=over 4

=item elt_class

In order to subclass C<XML::Twig> you will probably need to subclass also
C<L<XML::Twig::Elt>>. Use the C<elt_class> option when you create the
C<XML::Twig> object to get the elements created in a different class
(which should be a subclass of C<XML::Twig::Elt>.

=item add_options

If you inherit C<XML::Twig> new method but want to add more options to it
you can use this method to prevent XML::Twig to issue warnings for those
additional options.

=back

=head2 DTD Handling

There are 3 possibilities here.  They are:

=over 4

=item No DTD

No doctype, no DTD information, no entity information, the world is simple...

=item Internal DTD

The XML document includes an internal DTD, and maybe entity declarations.

If you use the load_DTD option when creating the twig the DTD information and
the entity declarations can be accessed.

The DTD and the entity declarations will be C<flush>'ed (or C<print>'ed) either
as is (if they have not been modified) or as reconstructed (poorly, comments 
are lost, order is not kept, due to it's content this DTD should not be viewed 
by anyone) if they have been modified. You can also modify them directly by 
changing the C<< $twig->{twig_doctype}->{internal} >> field (straight from 
XML::Parser, see the C<Doctype> handler doc)

=item External DTD

The XML document includes a reference to an external DTD, and maybe entity 
declarations.

If you use the C<load_DTD> when creating the twig the DTD information and the 
entity declarations can be accessed. The entity declarations will be
C<flush>'ed (or C<print>'ed) either as is (if they have not been modified) or
as reconstructed (badly, comments are lost, order is not kept).

You can change the doctype through the C<< $twig->set_doctype >> method and 
print the dtd through the C<< $twig->dtd_text >> or C<< $twig->dtd_print >>
 methods.

If you need to modify the entity list this is probably the easiest way to do it.

=back


=head2 Flush

If you set handlers and use C<flush>, do not forget to flush the twig one
last time AFTER the parsing, or you might be missing the end of the document.

Remember that element handlers are called when the element is CLOSED, so
if you have handlers for nested elements the inner handlers will be called
first. It makes it for example trickier than it would seem to number nested
clauses.



=head1 BUGS

=over 4

=item entity handling

Due to XML::Parser behaviour, non-base entities in attribute values disappear:
C<att="val&ent;"> will be turned into C<< att => val >>, unless you use the 
C<keep_encoding> argument to C<< XML::Twig->new >> 

=item DTD handling

Basically the DTD handling methods are competely bugged. No one uses them and
it seems very difficult to get them to work in all cases, including with 
several slightly incompatible versions of XML::Parser and of libexpat.

So use XML::Twig with standalone documents, or with documents refering to an
external DTD, but don't expect it to properly parse and even output back the
DTD.

=item memory leak

If you use a lot of twigs you might find that you leak quite a lot of memory
(about 2Ks per twig). You can use the C<L<dispose|dispose> > method to free 
that memory after you are done.

If you create elements the same thing might happen, use the C<L<delete|delete>>
method to get rid of them.

Alternatively installing the C<Scalar::Util> (or C<WeakRef>) module on a version 
of Perl that supports it (>5.6.0) will get rid of the memory leaks automagically.

=item ID list

The ID list is NOT updated when ID's are modified or elements cut or
deleted.

=item change_gi

This method will not function properly if you do:

     $twig->change_gi( $old1, $new);
     $twig->change_gi( $old2, $new);
     $twig->change_gi( $new, $even_newer);

=item sanity check on XML::Parser method calls

XML::Twig should really prevent calls to some XML::Parser methods, especially 
the C<setHandlers> method.

=item pretty printing

Pretty printing (at least using the 'C<indented>' style) is hard to get right! 
Only elements that belong to the document will be properly indented. Printing 
elements that do not belong to the twig makes it impossible for XML::Twig to 
figure out their depth, and thus their indentation level.

Also there is an anavoidable bug when using C<flush> and pretty printing for
elements with mixed content that start with an embedded element:

  <elt><b>b</b>toto<b>bold</b></elt>

  will be output as 

  <elt>
    <b>b</b>toto<b>bold</b></elt>

if you flush the twig when you find the C<< <b> >> element
  

=back

=head1 Globals

These are the things that can mess up calling code, especially if threaded.
They might also cause problem under mod_perl. 

=over 4

=item Exported constants

Whether you want them or not you get them! These are subroutines to use
as constant when creating or testing elements

  PCDATA  return '#PCDATA'
  CDATA   return '#CDATA'
  PI      return '#PI', I had the choice between PROC and PI :--(

=item Module scoped values: constants

these should cause no trouble:

  %base_ent= ( '>' => '&gt;',
               '<' => '&lt;',
               '&' => '&amp;',
               "'" => '&apos;',
               '"' => '&quot;',
             );
  CDATA_START   = "<![CDATA[";
  CDATA_END     = "]]>";
  PI_START      = "<?";
  PI_END        = "?>";
  COMMENT_START = "<!--";
  COMMENT_END   = "-->";

pretty print styles

  ( $NSGMLS, $NICE, $INDENTED, $RECORD1, $RECORD2)= (1..5);

empty tag output style

  ( $HTML, $EXPAND)= (1..2);

=item Module scoped values: might be changed

Most of these deal with pretty printing, so the worst that can
happen is probably that XML output does not look right, but is
still valid and processed identically by XML processors.

C<$empty_tag_style> can mess up HTML bowsers though and changing C<$ID> 
would most likely create problems.

  $pretty=0;           # pretty print style
  $quote='"';          # quote for attributes
  $INDENT= '  ';       # indent for indented pretty print
  $empty_tag_style= 0; # how to display empty tags
  $ID                  # attribute used as an id ('id' by default)

=item Module scoped values: definitely changed

These 2 variables are used to replace tags by an index, thus 
saving some space when creating a twig. If they really cause
you too much trouble, let me know, it is probably possible to
create either a switch or at least a version of XML::Twig that 
does not perform this optimisation.

  %gi2index;     # tag => index
  @index2gi;     # list of tags

=back

If you need to manipulate all those values, you can use the following methods on the
XML::Twig object:

=over 4

=item global_state

Return a hasref with all the global variables used by XML::Twig

The hash has the following fields:  C<pretty>, C<quote>, C<indent>, 
C<empty_tag_style>, C<keep_encoding>, C<expand_external_entities>, 
C<output_filter>, C<output_text_filter>, C<keep_atts_order>

=item set_global_state ($state)

Set the global state, C<$state> is a hashref

=item save_global_state

Save the current global state

=item restore_global_state

Restore the previously saved (using C<Lsave_global_state>> state

=back

=head1 TODO 

=over 4

=item SAX handlers

Allowing XML::Twig to work on top of any SAX parser

=item multiple twigs are not well supported

A number of twig features are just global at the moment. These include
the ID list and the "tag pool" (if you use C<change_gi> then you change the tag 
for ALL twigs).

A future version will try to support this while trying not to be to
hard on performance (at least when a single twig is used!).


=back


=head1 AUTHOR

Michel Rodriguez <mirod@xmltwig.com>

=head1 LICENSE

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

Bug reports should be sent using:
F<RTL<http://rt.cpan.org/NoAuth/Bugs.html?Dist=XML-Twig>>

Comments can be sent to mirod@xmltwig.com

The XML::Twig page is at L<http://www.xmltwig.com/xmltwig/>
It includes the development version of the module, a slightly better version 
of the documentation, examples, a tutorial and a: 
F<Processing XML efficiently with Perl and XML::Twig: 
L<http://www.xmltwig.com/xmltwig/tutorial/index.html>>

=head1 SEE ALSO

Complete docs, including a tutorial, examples, an easier to use HTML version,
a quick reference card and a FAQ are available at http://www.xmltwig.com/xmltwig/

XML::Parser,XML::Parser::Expat, Encode, Text::Iconv, Scalar::Utils


=cut


