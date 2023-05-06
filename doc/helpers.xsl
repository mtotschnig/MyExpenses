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
            <xsl:when test="$version = '3.2.5'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='contrib_feature_csv_import_label']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='autofill']" />
                <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.3.0'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='contrib_feature_csv_import_label']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='tags']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='active_tags']" />
                <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.3.1'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_settings']" />
                <xsl:text>&#032;-&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='autofill']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
                <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.3.2'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_translation_title']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForLanguage('te', $lang)" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='currency']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
                <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.3.3'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_exchange_rate_provider_title']" />
                <xsl:text>:&#032;https://exchangerate.host</xsl:text>
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_backup_cloud_summary']" />
            </xsl:when>
            <xsl:when test="$version = '3.3.4'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='debt_managment']" />
            </xsl:when>
            <xsl:when test="$version = '3.3.5'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='debt_managment']" />
                <xsl:text>&#032;2.0</xsl:text>
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_category_title_export']" />
                <xsl:text>: JSON</xsl:text>
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='title_scan_receipt_feature']" />
                <xsl:text>&#032;(</xsl:text>
                <xsl:value-of select="my:displayNameForScript('Han', $lang)" />
                <xsl:text>,&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForScript('Deva', $lang)" />
                <xsl:text>,&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForScript('Jpan', $lang)" />
                <xsl:text>,&#032;</xsl:text>
                <xsl:value-of select="my:displayNameForScript('Kore', $lang)" />
                <xsl:text>)</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.3.7'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
                <xsl:text>&#032;-&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($aosp)/resources/string[@name='setup']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
                <xsl:text>.</xsl:text>
            </xsl:when>

            <xsl:when test="$version = '3.3.8'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='whats_new_338']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='menu_budget']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='ui_refinement']" />
            </xsl:when>
            <xsl:when test="$version = '3.3.9'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='dialog_title_purge_backups']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='pref_perform_share_title']" />
                <xsl:text>:&#032;HTTP</xsl:text>
            </xsl:when>
            <xsl:when test="$version = '3.4.0'">
                <xsl:if test="$itemize">
                    <xsl-text>•&#032;</xsl-text>
                </xsl:if>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='whats_new_340']" />
                <xsl:value-of select="$separator" />
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='bug_fixes']" />
                <xsl:text>:&#032;</xsl:text>
                <xsl:apply-templates mode="unescape"
                    select="document($strings)/resources/string[@name='synchronization']" />
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
                    select="document($strings)/resources/string[@name='whats_new_344']" />
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
