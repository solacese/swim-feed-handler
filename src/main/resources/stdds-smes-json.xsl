<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="urn:us:gov:dot:faa:atm:terminal:entities:v4-0:smes:surfacemovementevent">
    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="//positionReport">
        <xsl:text>{</xsl:text>
        <xsl:text>"time":"</xsl:text><xsl:value-of select="time"/><xsl:text>",</xsl:text>
        <xsl:text>"identifier":"</xsl:text><xsl:value-of select="stid"/><xsl:text>",</xsl:text>
        <xsl:text>"aircraftIdentification":"</xsl:text><xsl:value-of select="flightId/aircraftId"/><xsl:text>",</xsl:text>
        <xsl:text>"aircraftType":"</xsl:text><xsl:value-of select="flightInfo/acType"/><xsl:text>",</xsl:text>
        <xsl:text>"lat":"</xsl:text><xsl:value-of select="position/latitude"/><xsl:text>",</xsl:text>
        <xsl:text>"lon":"</xsl:text><xsl:value-of select="position/longitude"/><xsl:text>",</xsl:text>
        <xsl:text>"altitude":"</xsl:text><xsl:value-of select="position/altitude"/><xsl:text>",</xsl:text>
        <xsl:text>"speed":"</xsl:text><xsl:value-of select="movement/speed"/><xsl:text>",</xsl:text>
        <xsl:text>"heading":"</xsl:text><xsl:value-of select="movement/heading"/><xsl:text>"</xsl:text>
        <xsl:text>}</xsl:text>
    </xsl:template>

</xsl:stylesheet>
