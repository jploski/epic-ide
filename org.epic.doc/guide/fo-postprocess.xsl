<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xslt"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	>
<!--
	This stylesheet is a hack used to remove the 3 front pages generated
	by FOP and to insert the title just above the table of contents.
-->
<xsl:output method="xml" indent="yes" xalan:indent-amount="2" encoding="ISO-8859-1"/>

<xsl:template match="fo:page-sequence[@master-reference='titlepage']">
</xsl:template>
	
<xsl:template match="fo:block[@id='toc...N10001']">
 <fo:block>                                                                       
   <fo:block font-family="sans-serif,Symbol,ZapfDingbats" font-weight="bold" font-size="24.8832pt" text-align="left" space-before="0pt">
     <fo:block hyphenate="false" keep-with-next.within-column="always">EPIC - User's Guide</fo:block>
   </fo:block>                                                                    
 </fo:block>   
 <xsl:copy>
  <xsl:apply-templates select="@*|node( )"/>
 </xsl:copy>
</xsl:template>

<xsl:template match="@*|node( )">
 <xsl:copy>
  <xsl:apply-templates select="@*|node( )"/>
 </xsl:copy>
</xsl:template>
   
</xsl:stylesheet>