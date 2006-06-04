<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
	Stylesheet used to generate a single XHTML file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl"/>

<xsl:template match="/">
  <!-- Call original code from the imported stylesheet -->
  <xsl:apply-imports/>
</xsl:template>
	
<xsl:param name="paper.type">A4</xsl:param>
<xsl:param name="draft.mode">no</xsl:param>
<xsl:param name="chapter.autolabel" select="0"/>

</xsl:stylesheet>