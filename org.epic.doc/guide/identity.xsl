<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xslt">
<xsl:output method="xml" indent="yes" xalan:indent-amount="2" encoding="ISO-8859-1"/>

<xsl:template match="@*|node( )">
 <xsl:copy>
  <xsl:apply-templates select="@*|node( )"/>
 </xsl:copy>
</xsl:template>
   
</xsl:stylesheet>