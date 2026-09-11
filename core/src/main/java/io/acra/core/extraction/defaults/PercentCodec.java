package io.acra.core.extraction.defaults;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class PercentCodec {
    private PercentCodec() {}
    static String decode(String input) {
        if (input == null || input.indexOf('%') < 0) return input == null ? "" : input;
        StringBuilder out = new StringBuilder();
        for (int i=0;i<input.length();) {
            if (input.charAt(i) == '%') {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int j=i;
                while (j+2<input.length() && input.charAt(j)=='%') {
                    int hi=Character.digit(input.charAt(j+1),16), lo=Character.digit(input.charAt(j+2),16);
                    if (hi<0 || lo<0) break;
                    bytes.write((hi<<4)|lo); j+=3;
                }
                if (j>i) { out.append(bytes.toString(StandardCharsets.UTF_8)); i=j; continue; }
            }
            out.append(input.charAt(i++));
        }
        return out.toString();
    }
}
