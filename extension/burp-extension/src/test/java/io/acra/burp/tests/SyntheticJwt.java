package io.acra.burp.tests;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public final class SyntheticJwt {
    private SyntheticJwt(){}
    public static String token(String subject,String tenant,String role){
        String h="{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String p="{\"sub\":\""+esc(subject)+"\",\"tenant_id\":\""+esc(tenant)+"\",\"role\":\""+esc(role)+"\"}";
        return b64(h)+"."+b64(p)+"."+b64("synthetic-runtime-signature");
    }
    private static String b64(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String esc(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"");}
}

