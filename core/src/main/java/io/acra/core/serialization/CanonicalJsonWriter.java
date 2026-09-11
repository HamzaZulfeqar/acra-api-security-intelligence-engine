package io.acra.core.serialization;

import io.acra.core.domain.http.*;
import io.acra.core.security.*;
import io.acra.core.graph.SecurityContextGraph;
import java.lang.reflect.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;

final class CanonicalJsonWriter {
    private final UniversalRedactor redactor;
    CanonicalJsonWriter(UniversalRedactor redactor){this.redactor=redactor;}

    String write(Object value){ StringBuilder b=new StringBuilder(); append(b,value); return b.toString(); }
    private void append(StringBuilder b,Object v){
        if(v==null){b.append("null");return;}
        if(v instanceof HttpRequest r){ appendRequest(b,r); return; }
        if(v instanceof HttpResponse r){ appendResponse(b,r); return; }
        if(v instanceof HttpHeader h){ map(b,Map.of("name",h.name(),"value",redactor.redactHeader(h.name(),h.value()))); return; }
        if(v instanceof SecurityContextGraph g){ map(b,Map.of("nodes",g.nodes(),"edges",g.edges(),"evidence",g.evidenceList())); return; }
        if(v instanceof String s){ quote(b,redactor.redactText(s)); return; }
        if(v instanceof Character c){quote(b,c.toString());return;}
        if(v instanceof Number||v instanceof Boolean){b.append(v);return;}
        if(v instanceof Enum<?> e){quote(b,e.name());return;}
        if(v instanceof TemporalAccessor){quote(b,v.toString());return;}
        if(v instanceof byte[] bytes){ map(b,Map.of("length",bytes.length,"sha256",TokenFingerprint.sha256(bytes),"rawPersisted",false)); return; }
        if(v instanceof Map<?,?> m){ map(b,m); return; }
        if(v instanceof Iterable<?> it){ b.append('['); boolean first=true; for(Object x:it){if(!first)b.append(',');first=false;append(b,x);} b.append(']'); return; }
        if(v.getClass().isArray()){ b.append('['); int n=Array.getLength(v); for(int i=0;i<n;i++){if(i>0)b.append(',');append(b,Array.get(v,i));} b.append(']'); return; }
        if(v.getClass().isRecord()){ record(b,v); return; }
        quote(b,redactor.redactText(v.toString()));
    }
    private void appendRequest(StringBuilder b,HttpRequest r){
        Map<String,Object> m=new TreeMap<>(); m.put("bodyLength",r.body().length); m.put("bodySha256",TokenFingerprint.sha256(r.body())); m.put("bodyText",redactor.redactText(r.bodyUtf8()));
        m.put("cookies",redactedCookies(r.cookies())); m.put("headers",r.headers()); m.put("host",r.host()); m.put("method",r.method()); m.put("path",r.path()); m.put("port",r.port()); m.put("protocol",r.protocol()); m.put("query",r.query()); m.put("rawBytes",Map.of("length",r.rawBytes().length,"sha256",TokenFingerprint.sha256(r.rawBytes()),"rawPersisted",false)); m.put("rawTarget",redactor.redactText(r.rawTarget())); m.put("scheme",r.scheme()); map(b,m);
    }
    private void appendResponse(StringBuilder b,HttpResponse r){
        Map<String,Object> m=new TreeMap<>(); m.put("bodyLength",r.body().length); m.put("bodySha256",TokenFingerprint.sha256(r.body())); m.put("bodyText",redactor.redactText(r.bodyUtf8())); m.put("contentType",r.contentType()); m.put("headers",r.headers()); m.put("protocol",r.protocol()); m.put("rawBytes",Map.of("length",r.rawBytes().length,"sha256",TokenFingerprint.sha256(r.rawBytes()),"rawPersisted",false)); m.put("status",r.status()); map(b,m);
    }
    private Map<String,String> redactedCookies(Map<String,String> cookies){ TreeMap<String,String> out=new TreeMap<>(); cookies.forEach((k,v)->out.put(k,UniversalRedactor.REDACTED)); return out; }
    private void record(StringBuilder b,Object v){
        TreeMap<String,Object> m=new TreeMap<>();
        try{ for(RecordComponent c:v.getClass().getRecordComponents()) m.put(c.getName(),c.getAccessor().invoke(v)); }
        catch(ReflectiveOperationException e){ throw new IllegalStateException("cannot serialize record",e); }
        map(b,m);
    }
    private void map(StringBuilder b,Map<?,?> m){
        TreeMap<String,Object> sorted=new TreeMap<>(); for(var e:m.entrySet()) sorted.put(String.valueOf(e.getKey()),e.getValue());
        b.append('{'); boolean first=true; for(var e:sorted.entrySet()){if(!first)b.append(',');first=false;quote(b,redactor.redactText(e.getKey()));b.append(':');append(b,redactor.isSensitiveField(e.getKey())?UniversalRedactor.REDACTED:e.getValue());} b.append('}');
    }
    private static void quote(StringBuilder b,String s){ b.append('"'); for(char c:s.toCharArray()) switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<0x20)b.append(String.format("\\u%04x",(int)c));else b.append(c);}} b.append('"'); }
}
