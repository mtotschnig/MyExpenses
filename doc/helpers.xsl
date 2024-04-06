<?xml version='1.0' ?>
<xsl:stylesheet xmlns:my="http://myexpenses.mobi/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

    <xsl:template match="item|string" mode="unescape">
        <xsl:variable name="apostrophe">'</xsl:variable>
        <xsl:variable name="quote">"</xsl:variable>
        <xsl:variable name="trim">
            <xsl:choose>
                <xsl:when test="starts-with(., $quote)">
                    <xsl:value-of select="substring-before(substring-after(., $quote), $quote) " />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="." />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of
            select="replace(replace($trim,concat('\\',$apostrophe),$apostrophe),concat('\\',$quote),$quote)" />
    </xsl:template>

    <!-- WIP     -->
<!--    <xsl:template name="resolveWithFallBack">
        <xsl:param name="lang" />
        <xsl:param name="resource" />
        <xsl:param name="name" />
        <xsl:variable name="localResource">
            <xsl:call-template name="values-dir">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
            <xsl:text>/</xsl:text>
            <xsl:value-of select="$resource" />
            <xsl:text>.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="fallbackResource">
            <xsl:call-template name="values-dir">
                <xsl:with-param name="lang" select="'en'" />
            </xsl:call-template>
            <xsl:text>/</xsl:text>
            <xsl:value-of select="$resource" />
            <xsl:text>.xml</xsl:text>
        </xsl:variable>
        <xsl:apply-templates mode="unescape"
            select="document($localResource)/resources/string[@name=$name]" />
    </xsl:template>-->

    <xsl:template name="special-version-info">
        <xsl:param name="version" />
        <xsl:param name="strings" />
        <xsl:param name="aosp" />
        <xsl:param name="help" />
        <xsl:param name="upgrade" />
        <xsl:param name="lang" />
        <xsl:param name="itemize" select="true()" />
        <xsl:variable name="separator">
            <xsl:choose>
                <xsl:when test="$itemize">
                    <xsl:value-of select="$newline" />
                    <xsl-text>•&#032;</xsl-text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>.&#032;</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <!-- a version name with 3 dots and 4 digits is a bug fix release -->
            <xsl:when test="string-length($version) = 7">
<!--                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>-->
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='bug_fixes']" />
<!--                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />-->
            </xsl:when>
            <xsl:when test="$version = '3.4.1'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='split_transaction']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='split_parts_heading']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_original_amount']" />
            </xsl:when>
            <xsl:when test="$version = '3.4.2'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='title_webui']" />
                <xsl:text>&#032;2.0</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.4.3'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='customize']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="my:simpleFormatRes(document($strings)/resources/string[@name='export_to_format'], 'CSV')" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="my:simpleFormatRes(document($strings)/resources/string[@name='export_to_format'], 'JSON')" />
                <xsl:text>&#032;2.0</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.4.4'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='icons_for_categories']" />
                <xsl:text>&#032;2.0</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.4.5'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_budget']" />
                <xsl:text>&#032;3.0</xsl:text>
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='title_webui']" />
                <xsl:text>: https</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.4.6'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($help)/resources/string[@name='help_MyExpenses_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='redesign']" />
            </xsl:when>
            <xsl:when test="$version = '3.4.7'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='storage_description']" />
            </xsl:when>
            <xsl:when test="$version = '3.4.8'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_translation_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForLanguage('nl', $lang)" />
            </xsl:when>
            <xsl:when test="$version = '3.4.9'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text> (WebDAV):&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_reconfigure']" />

                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_translation_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForLanguage('ur', $lang)" />
            </xsl:when>
            <xsl:when test="$version = '3.5.0'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='encrypt_database']" />
                <xsl:text>&#032;(</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='experimental']" />
                <xsl:text>)</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.5.1' or $version = '3.5.3'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='debt_managment']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.5.2'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_manage_categories_title']" />
            </xsl:when>
            <xsl:when test="$version = '3.5.4'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_exchange_rate_provider_title']" />
                <xsl:text>:&#032;https://coinapi.io</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.5.6'">
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='optimize_image_file_size']" />
            </xsl:when>
            <xsl:when test="$version = '3.5.8'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($upgrade)/resources/string[@name='whats_new_358']" />
                <xsl:text>&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='template_shortcut']" />
            </xsl:when>
            <xsl:when test="$version = '3.5.9'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($upgrade)/resources/string[@name='whats_new_359']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='grand_total']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='menu_search']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_equivalent_amount']" />
            </xsl:when>
            <xsl:when test="$version = '3.6.0'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($help)/resources/string[@name='help_ManageTemplates_plans_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.6.2'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_settings']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.6.3'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:text>Multibanking&#032;(</xsl:text>
                <xsl:value-of select="my:displayNameForCountry('de', $lang)" />
                <xsl:text>)</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.6.8'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_distribution']" />
                <xsl:text>,&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_budget']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.7.1'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='take_photo']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.7.2'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text>:&#032;Microsoft OneDrive</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.7.3'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='stability_improvements']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($help)/resources/string[@name='help_MyExpenses_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.7.5'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_unlink_transfer']" />
            </xsl:when>
            <xsl:when test="$version = '3.7.6'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($upgrade)/resources/string[@name='whats_new_376_1']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($upgrade)/resources/string[@name='whats_new_376_2']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_transform_to_transfer']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="my:simpleFormatRes(document($strings)/resources/string[@name='export_to_format'], 'CSV')" />
                <xsl:text>:&#032;MS Excel</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.7.7'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="my:simpleFormatRes(document($strings)/resources/string[@name='export_to_format'], 'CSV')" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_original_amount']" />
                <xsl:text>,&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_equivalent_amount']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.7.9'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_original_amount']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.8.0'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='merge_categories_dialog_title']" />
            </xsl:when>
            <xsl:when test="$version = '3.8.1'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='menu_print']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='configuration']" />
                <xsl:text> (</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='paper_format']" />
                <xsl:text>, </xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='header_footer']" />
                <xsl:text>)</xsl:text>
            </xsl:when>
            <xsl:otherwise />
        </xsl:choose>
    </xsl:template>

    <xsl:function name="my:changeLogResourceName">
        <xsl:param name="version" />
        <xsl:value-of select="concat('whats_new_',replace($version,'\.',''))" />
    </xsl:function>
    <xsl:function name="my:githubBoardResourceName">
        <xsl:param name="version" />
        <xsl:value-of select="concat('project_board_',replace($version,'\.',''))" />
    </xsl:function>
    <xsl:function name="my:simpleFormatRes">
        <xsl:param name="format" />
        <xsl:param name="arg" />
        <xsl:value-of select="replace($format,'%s',$arg)" />
    </xsl:function>
</xsl:stylesheet>
