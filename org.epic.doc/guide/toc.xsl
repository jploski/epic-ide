<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
	Stylesheet used to generate toc.xml for the Eclipse help system.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:import href="eclipse.xsl"/>
<xsl:output method="xml" indent="yes" encoding="ISO-8859-1"/>

<xsl:template match="/">
  <xsl:call-template name="etoc"/>
</xsl:template>

<xsl:param name="chunk.first.sections" select="1"/>
<xsl:param name="base.dir">temp/</xsl:param>
<xsl:param name="manifest.in.base.dir">1</xsl:param>

</xsl:stylesheet>