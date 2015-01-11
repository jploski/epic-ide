<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
	Stylesheet used to generate a single XHTML file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/1.73.0/xhtml/docbook.xsl"/>
	
<xsl:template match="/">
  <!-- Call original code from the imported stylesheet -->
  <xsl:apply-imports/>
</xsl:template>
	
<xsl:param name="html.stylesheet">book.css</xsl:param>

</xsl:stylesheet>
