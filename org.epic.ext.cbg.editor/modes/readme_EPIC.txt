Notes on the newly (Dec. 2004) implemented features of the XML-File.

Added Keywords effect mainly the SPAN-Type where additional Keywords specifiy the context more prececisly. The Keywords are:

1) GROUP
2) IGNORE_TEXT_AFTER_START_TAG = TRUE/FALSE (default)
3) MATCH_BRACKET=TRUE/FALSE (default)
4) DYNAMIC_TAGGING=TRUE/FALSE (default)
5) DELIMITER_MAX_CHAR = (any given positive Number)
6) NO_OF_MULTIPLE_ENDTAG= (any given positive Number)
7) REQUIRE_BEFORE_TAG= (any Characters resp. special Literals: :LINEFEED: :WHITESPACE:)#
8) REQUIRE_AFTER_TAG= (any Characters resp. special Literals: :LINEFEED: )

In detail:

1) This allows to summon different Keywords for SPAN-Type into a group and apply the same characteristic on them. If there exists a BEGIN then this will be treated as GROUP, but is only inspected once.

The design of the GROUP is, that Keywords are summoned into a *Field* and will be inspected starting with the shortest ones. If a string is found, then no further inspection is done. If you have e.g. 's' and 'sub' in your GROUP, the first 's' will be marked, ignoring the 'sub'. If you wanna consider 'sub' instead of 's', then you have to make a different TYPE (e.g. SPAN) and then place it in front of the 's' - TYPE.

2) If set to true, and the start string Tag is found, any further text after the Start Tag will be marked with that specifica.

None of the other given additional keywords are considered.

3) If set to true, it will search for this kind of SPAN the matching closing-Bracket. The Brackets are derived from the END-Tag and are searched accordingly.

The extension of this, is that you can now dynamically mark you Bracket-array, e.g. ${$::FORM{"id}} will now mark till the end of the last matching }.

This option overrules the next optional parameters.

4) DYNAMIC_TAGGING: If set to true, the Delimiter resp. Tag is dervived dynamcially out of the context. The following parameters are required to make this parameter useful.

The Tag starts with the first non-Whitespace character after the specified (=found) BEGIN resp. GROUP and will be taken untill the first Whitespace occurs resp. EOF *OR* if the DELIMITER_MAX_CHAR is exceeded.

e.g. m+t+a+;

the Tag 'm' is found out of the GROUP-List, next it takes the first non-Whitespace char after 'm', i.e. '+' and here it takes 1 char => the delimiter is '+'.

5) DELIMITER_MAX_CHAR: The parameter specifies the length of the Tag.

This paramter has ONLY effect on DYNAMIC_TAGGING=TRUE, in any other case it is ignored.

6) NO_OF_MULTIPLE_ENDTAG: Specifies how many times the Tag should be used.

This property could be used independent of the setting DYNAMIC_TAGGING.

7) REQUIRE_BEFORE_TAG: This specifies which characters are required in front when DYNAMIC_TAGGING=TRUE.

Note: The programmers lazyness (LeO) was too much, there it only checks, if there is content or not. If yes => Delimiter could be any
If not => Delimter any non-whitespace + non-alphanummerical

8) REQUIRE_AFTER_TAG: Similar to the Tag REQUIRE_BEFORE_TAG.

BUT here pleaze note the lazyiness of the implementor (LeO): Currently it is only considered one Character only. If there is any kind of requirement to expand it to two or more tags, please feel free to adapt the code correspondigly.


KNOWN BUGs:

What not works:

- different delimitertypes for Brackets, e.g.

s[a]{b}

but what works is (as well as the other synthetic candies)

s[a][b]

- HERE Documents provide no stack as well the interpretation of \" is not performed. Additionally spaces could be interpreted differently then Perl does.

- the trinary operator ?: is not provided (see also below)

- the operator / could be used for divisions as well as matching operator. Not much effort to interpret the correct situation is done, except for match-operator it is required to have them in one line. Same applies for the operator ?

