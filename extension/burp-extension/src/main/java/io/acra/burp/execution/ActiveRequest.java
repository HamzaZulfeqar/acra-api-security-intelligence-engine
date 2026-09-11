package io.acra.burp.execution;
public record ActiveRequest(String scheme,String host,int port,String path,String purpose,boolean userConfirmed) {
    public ActiveRequest { if(scheme==null||host==null||path==null) throw new IllegalArgumentException("request target required"); purpose=purpose==null?"":purpose; }
}
