package io.acra.core.tests;

import io.acra.core.domain.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.Base64;

public final class Fixtures {
    private Fixtures() {}
    public static HttpTransaction tx(String target,List<HttpHeader> headers){
        HttpRequest req=HttpRequest.of(HttpMethod.GET,"https","api.example.test",443,target,headers,"".getBytes(StandardCharsets.UTF_8),HttpProtocol.HTTP_2);
        HttpResponse res=new HttpResponse(200,List.of(new HttpHeader("Content-Type","application/json")),"{}".getBytes(StandardCharsets.UTF_8),"application/json",HttpProtocol.HTTP_2,new byte[0]);
        return new HttpTransaction(req,res,Instant.parse("2026-08-30T12:00:00Z"),"req-1032","test",Map.of());
    }
    public static HttpTransaction tx(String target){return tx(target,List.of());}
    public static String jwt(Map<String,String> claims){
        String h="{\"alg\":\"none\",\"typ\":\"JWT\"}";
        StringBuilder p=new StringBuilder("{"); boolean first=true; for(var e:new TreeMap<>(claims).entrySet()){if(!first)p.append(',');first=false;p.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');}p.append('}');
        return b64(h)+"."+b64(p.toString())+"."+b64("runtime-signature");
    }
    private static String b64(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public static String runtimeSecret(){return "s3cr3t-"+"X".repeat(40)+"-"+System.nanoTime();}
}
