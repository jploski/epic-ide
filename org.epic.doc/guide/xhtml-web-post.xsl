<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
	Stylesheet used to postprocess generate multiple XHTML files for the web site
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:xhtml="http://www.w3.org/1999/xhtml"
                version="1.0">
- 
<xsl:template match='*|@*' priority="1">
<xsl:copy>
<xsl:apply-templates select='node()|@*'/>
</xsl:copy>
</xsl:template>

<xsl:template match="xhtml:base" priority="100">
    <xsl:text disable-output-escaping="yes">
&lt;?php
if ($_SERVER['HTTP_HOST'] == "localhost") {
  $base_href = "http://localhost/epic/";
} else {
  $base_href = "http://".$_SERVER['HTTP_HOST']."/";
}
echo '&lt;base href="'.$base_href.'guide/" /&gt;';
?&gt;    
    </xsl:text>
</xsl:template>

<xsl:template match="xhtml:div[@class='navheader']" priority="100">
	<xsl:copy>
		<xsl:apply-templates select='node()|@*'/>
	</xsl:copy>
	<xsl:choose>
		<xsl:when test="/descendant::xhtml:div[@class='book']">
			<p class="more" style="width: 500px">Note: This documentation is also available as a <a href="../downloads/EPIC_User's_Guide.pdf">printable PDF file</a> for download.</p>
		</xsl:when>
		<xsl:otherwise>
			<center><xsl:text disable-output-escaping="yes">&lt;?php include(&quot;../ad2.inc&quot;); ?&gt;</xsl:text></center>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="xhtml:body" priority="100">
	<div id="container">
		<xsl:text disable-output-escaping="yes">&lt;?php include(&quot;../header.inc&quot;); ?&gt;</xsl:text>
		<div id="page">
      		<div class="col wide">
				<xsl:copy>
					<xsl:apply-templates select='node()|@*'/>
				</xsl:copy>
			<br clear="all" />
			</div><br clear="all" />
		</div>
		<xsl:text disable-output-escaping="yes">&lt;?php include(&quot;../footer.inc&quot;); ?&gt;</xsl:text>
	</div>
</xsl:template>

</xsl:stylesheet>