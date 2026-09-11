package io.acra.core.extraction.defaults;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.*;

final class SimpleJwtClaims {
    private SimpleJwtClaims() {}
    static String claim(String token, String name) {
        try {
            String[] parts=token.split("\\."); if(parts.length<2) return null;
            String json=new String(Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
            Pattern p=Pattern.compile("\\\""+Pattern.quote(name)+"\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9]+)|([^,}\\s]+))");
            Matcher m=p.matcher(json); if(!m.find()) return null;
            for(int i=1;i<=3;i++) if(m.group(i)!=null) return m.group(i);
            return null;
        } catch(Exception e){ return null; }
    }
    private static String pad(String s){ int rem=s.length()%4; return rem==0?s:s+"=".repeat(4-rem); }
}
