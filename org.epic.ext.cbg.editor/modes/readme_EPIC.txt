Notes on the newly (Jan. 2005) implemented features of the XML-File.

For the SEQ-Type a new Keyword was introduced:

GROUP

If there exits an Group, any kind of Keyword is ignored!


Added Keywords effect mainly the SPAN-Type where additional Keywords specifiy the context more prececisly. The Keywords are:

1) GROUP
2) IGNORE_TEXT_AFTER_START_TAG = TRUE/FALSE (default)
3) MATCH_BRACKET=TRUE/FALSE (default)
4) DYNAMIC_TAGGING=TRUE/FALSE (default)
5) DELIMITER_MAX_CHAR = (any given positive Number)
6) NO_OF_MULTIPLE_ENDTAG= (any given positive Number)
7) REQUIRE_BEFORE_DELIMITER_CHAR= (any Characters)
8) REQUIRE_AFTER_DELIMITER_CHAR= (any Characters resp. special Literals: :LINEFEED:)
9) DELIMITER_TAG_CHARS= (default = empty)
10)OPTIONAL_MODIFIER = (default = empty)

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

7) REQUIRE_BEFORE_DELIMITER_CHAR: The character which is required before the Delimiter is retrieved. This parameter makes only sense, for Dynamic-Tagging, otherwise it is ignored.

The usage is especially for the HERE-Documents in Perl where '<<' specifies the start of the HERE-docs but could start immediately with the Delimiter as well with the Delimiter under quotes. But then the quotes are not required to be immediately after '<<' could be as well filled with withspaces. Therefore we have as BEGIN the <<-sequence, and then the REQUIRE_BEFORE_DELIMITER_CHAR set with quotes.

8) REQUIRE_AFTER_DELIMITER_CHAR: requires the follow-up char(sequence) after the Delimiter. If the follow-up char cannot be found, it continues the search. The Linefeed are used in symbolical manner. That's the only time where symbolical charactes are used!

Pleaze note the lazyiness of the implementor (LeO): Currently it is only considered one Character only. If there is any kind of requirement to expand it to two or more tags, please feel free to adapt the code correspondigly.


9) DELIMITER_TAG_CHARS: Specifies what the Delimiter could consists of (any kind of combination is allowed):

- ":NONSCALAR:" => Delimiter is not a Scalar. If it is one => the Token is rejected. (Check on Scalar is done: First character of the Delimiter is '$')
- ":LETTER_OR_DIGITS:" => Delimiter contains ONLY letters or digits. All others characters are rejected, if not otherwise specified. If the Delimiter is empty and the Token not already rejected => a normal LineFeed is assumed as Line-Feed.
- ":NO_WHITESPACE_BEFORE_DELIM:" => reject the Token if after before Delimiter is whitepace
- ":ALLOW_ESCAPE:" => Delimiter could consist as well of Escape-characters.
- ":NONINTEGER:" => Delimiter does not contain an Integer, if it contains one => the Token is rejected


10) OPTIONAL_MODIFIER: After the last delimiter additional modifiers could be specified.

It is only checked, if the character after the last delimiter is contained in OPTIONAL_MODIFIER. If yes, it will be treated as well as a Token. Otherwise, the Token is finished.