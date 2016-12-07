/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package org.totschnig.myexpenses.sync.webdav;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUtils {
    private static final Pattern authSchemeWithParam = Pattern.compile("^([^ \"]+) +(.*)$");

    public static List<AuthScheme> parseWwwAuthenticate(String[] wwwAuths) {
        /* WWW-Authenticate  = "WWW-Authenticate" ":" 1#challenge

           challenge      = auth-scheme 1*SP 1#auth-param
           auth-scheme    = token
           auth-param     = token "=" ( token | quoted-string )

           We call the auth-param tokens: <name>=<value>

           token          = 1*<any CHAR except CTLs or separators>
           separators     = "(" | ")" | "<" | ">" | "@"
                          | "," | ";" | ":" | "\" | <">
                          | "/" | "[" | "]" | "?" | "="
                          | "{" | "}" | SP | HT

           quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
           qdtext         = <any TEXT except <">>
           quoted-pair    = "\" CHAR
      */

        List<AuthScheme> schemes = new LinkedList<>();
        for (String wwwAuth : wwwAuths) {
            // Step 1: tokenize by ',', but take into account that auth-param values may contain quoted-pair values with ',' in it (these ',' have to be ignored)
            // Auth-scheme and auth-param names are tokens and thus must not contain the '"' separator.
            List<String> tokens = new LinkedList<>();
            StringBuilder token = new StringBuilder();

            boolean inQuotes = false;
            int len = wwwAuth.length();
            for (int i = 0; i < len; i++) {
                char c = wwwAuth.charAt(i);

                boolean literal = false;
                if (c == '"')
                    inQuotes = !inQuotes;
                else if (inQuotes && c == '\\' && i + 1 < len) {
                    token.append(c);

                    c = wwwAuth.charAt(++i);
                    literal = true;
                }

                if (c == ',' && !inQuotes && !literal) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                } else
                    token.append(c);
            }
            if (token.length() != 0)
                tokens.add(token.toString());

            /* Step 2: determine token type after trimming:
                "<authSchemes> <auth-param>"        new auth scheme + 1 param
                "<auth-param>"                      add param to previous auth scheme
                Take into account that the second type may contain quoted spaces.
                The auth scheme name must not contain separators (including quotes).
             */
            AuthScheme scheme = null;
            for (String s : tokens) {
                s = s.trim();

                Matcher matcher = authSchemeWithParam.matcher(s);
                if (matcher.matches()) {
                    // auth-scheme with auth-param
                    schemes.add(scheme = new AuthScheme(matcher.group(1)));
                    scheme.addRawParam(matcher.group(2));
                } else if (scheme != null) {
                    // if there was an auth-scheme before, this must be an auth-param
                    scheme.addRawParam(s);
                } else {
                    // there was not auth-scheme before, so this must be an auth-scheme
                    schemes.add(scheme = new AuthScheme(s));
                }
            }
        }

        return schemes;
    }

    public static class AuthScheme {
        Pattern nameValue = Pattern.compile("^([^=]+)=(.*)$");

        public final String name;
        public final Map<String, String> params = new HashMap<>();
        public final List<String> unnamedParams = new LinkedList<>();

        public AuthScheme(String name) {
            this.name = name;
        }

        public void addRawParam(String authParam) {
            Matcher m = nameValue.matcher(authParam);
            if (m.matches()) {
                String name = m.group(1),
                        value = m.group(2);
                int len = value.length();
                if (value.charAt(0) == '"' && value.charAt(len - 1) == '"') {
                    // quoted-string
                    value = value
                            .substring(1, len - 1)
                            .replace("\\\"", "\"");
                }
                params.put(name, value);
            } else
                unnamedParams.add(authParam);
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(name).append("(");
            for (String name : params.keySet())
                s.append(name).append("=[").append(params.get(name)).append("],");
            s.append(")");
            return s.toString();
        }
    }
}
