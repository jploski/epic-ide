<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
	Stylesheet used to generate multiple XHTML files for the web site
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/1.73.0/xhtml/chunk.xsl"/>
	
<xsl:template match="/">
  <!-- Call original code from the imported stylesheet -->
  <xsl:apply-imports/>
</xsl:template>

<xsl:param name="html.base">http://www.epic-ide.org/guide/</xsl:param>
<xsl:param name="html.ext">.php</xsl:param>
<xsl:param name="html.stylesheet">../css/style.css</xsl:param>
<xsl:param name="chunk.quietly" select="1"/>
<xsl:param name="base.dir">temp/</xsl:param>
<xsl:param name="chunker.output.omit-xml-declaration">yes</xsl:param>

</xsl:stylesheet>
