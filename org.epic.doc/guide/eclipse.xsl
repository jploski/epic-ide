<?xml version="1.0"?>
<xsl:stylesheet exclude-result-prefixes="d"
                 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:d="http://docbook.org/ns/docbook"
version="1.0">

<xsl:import href="/data/mnt/data/usr/local/docbook-xsl-ns-1.73.0/html/chunk.xsl"/>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->
<!--
	jploski: I had to hack it to add the 'html/guide' href prefix :-(
	         This should really be settable using a parameter...
-->
<xsl:template match="/">
  <xsl:choose>
    <xsl:when test="$rootid != ''">
      <xsl:choose>
        <xsl:when test="count(key('id',$rootid)) = 0">
          <xsl:message terminate="yes">
            <xsl:text>ID '</xsl:text>
            <xsl:value-of select="$rootid"/>
            <xsl:text>' not found in document.</xsl:text>
          </xsl:message>
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="$collect.xref.targets = 'yes' or
                        $collect.xref.targets = 'only'">
            <xsl:apply-templates select="key('id', $rootid)"
                        mode="collect.targets"/>
          </xsl:if>
          <xsl:if test="$collect.xref.targets != 'only'">
            <xsl:message>Formatting from <xsl:value-of 
	                          select="$rootid"/></xsl:message>
            <xsl:apply-templates select="key('id',$rootid)"
                        mode="process.root"/>
            <xsl:call-template name="etoc"/>
            <xsl:call-template name="plugin.xml"/>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="$collect.xref.targets = 'yes' or
                    $collect.xref.targets = 'only'">
        <xsl:apply-templates select="/" mode="collect.targets"/>
      </xsl:if>
      <xsl:if test="$collect.xref.targets != 'only'">
        <xsl:apply-templates select="/" mode="process.root"/>
        <xsl:call-template name="etoc"/>
        <xsl:call-template name="plugin.xml"/>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>


</xsl:template>

<xsl:template name="etoc">
  <xsl:call-template name="write.chunk">
    <xsl:with-param name="filename">
      <xsl:if test="$manifest.in.base.dir != 0">
        <xsl:value-of select="$base.dir"/>
      </xsl:if>
      <xsl:value-of select="'toc.xml'"/>
    </xsl:with-param>
    <xsl:with-param name="method" select="'xml'"/>
    <xsl:with-param name="encoding" select="'utf-8'"/>
    <xsl:with-param name="indent" select="'yes'"/>
    <xsl:with-param name="content">
      <xsl:choose>

        <xsl:when test="$rootid != ''">
          <xsl:variable name="title">
            <xsl:if test="$eclipse.autolabel=1">
              <xsl:variable name="label.markup">
                <xsl:apply-templates select="key('id',$rootid)" mode="label.markup"/>
              </xsl:variable>
              <xsl:if test="normalize-space($label.markup)">
                <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
              </xsl:if>
            </xsl:if>
            <xsl:apply-templates select="key('id',$rootid)" mode="title.markup"/>
          </xsl:variable>
          <xsl:variable name="href">
			<xsl:value-of select="'html/guide/'"/>
            <xsl:call-template name="href.target.with.base.dir">
              <xsl:with-param name="object" select="key('id',$rootid)"/>
            </xsl:call-template>
          </xsl:variable>
          
          <toc label="{normalize-space($title)}" topic="{$href}">
            <xsl:apply-templates select="key('id',$rootid)/*" mode="etoc"/>
          </toc>
        </xsl:when>

        <xsl:otherwise>
          <xsl:variable name="title">
            <xsl:if test="$eclipse.autolabel=1">
              <xsl:variable name="label.markup">
                <xsl:apply-templates select="/*" mode="label.markup"/>
              </xsl:variable>
              <xsl:if test="normalize-space($label.markup)">
                <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
              </xsl:if>
            </xsl:if>
            <xsl:apply-templates select="/*" mode="title.markup"/>
          </xsl:variable>
          <xsl:variable name="href">
		    <xsl:value-of select="'html/guide/'"/>
            <xsl:call-template name="href.target.with.base.dir">
              <xsl:with-param name="object" select="/"/>
            </xsl:call-template>
          </xsl:variable>
          
          <toc label="{normalize-space($title)}" topic="{$href}">
            <xsl:apply-templates select="/*/*" mode="etoc"/>
          </toc>
        </xsl:otherwise>

      </xsl:choose>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match="d:book|d:part|d:reference|d:preface|d:chapter|d:bibliography|d:appendix|d:article|d:glossary|d:section|d:sect1|d:sect2|d:sect3|d:sect4|d:sect5|d:refentry|d:colophon|d:bibliodiv|d:index" mode="etoc">
  <xsl:variable name="title">
    <xsl:if test="$eclipse.autolabel=1">
      <xsl:variable name="label.markup">
        <xsl:apply-templates select="." mode="label.markup"/>
      </xsl:variable>
      <xsl:if test="normalize-space($label.markup)">
        <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
      </xsl:if>
    </xsl:if>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <xsl:variable name="href">
	<xsl:value-of select="'html/guide/'"/>
    <xsl:call-template name="href.target.with.base.dir">
      <xsl:with-param name="context" select="/"/>        <!-- Generate links relative to the location of root file/toc.xml file -->
    </xsl:call-template>
  </xsl:variable>

  <topic label="{normalize-space($title)}" href="{$href}">
    <xsl:apply-templates select="d:part|d:reference|d:preface|d:chapter|d:bibliography|d:appendix|d:article|d:glossary|d:section|d:sect1|d:sect2|d:sect3|d:sect4|d:sect5|d:refentry|d:colophon|d:bibliodiv|d:index" mode="etoc"/>
  </topic>

</xsl:template>

<xsl:template match="text()" mode="etoc"/>

<xsl:template name="plugin.xml">
  <xsl:call-template name="write.chunk">
    <xsl:with-param name="filename">
      <xsl:if test="$manifest.in.base.dir != 0">
        <xsl:value-of select="$base.dir"/>
      </xsl:if>
      <xsl:value-of select="'plugin.xml'"/>
    </xsl:with-param>
    <xsl:with-param name="method" select="'xml'"/>
    <xsl:with-param name="encoding" select="'utf-8'"/>
    <xsl:with-param name="indent" select="'yes'"/>
    <xsl:with-param name="content">
      <plugin name="{$eclipse.plugin.name}"
        id="{$eclipse.plugin.id}"
        version="1.0"
        provider-name="{$eclipse.plugin.provider}">

        <extension point="org.eclipse.help.toc">
          <toc file="toc.xml" primary="true"/>
        </extension>
          
      </plugin>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

</xsl:stylesheet>
