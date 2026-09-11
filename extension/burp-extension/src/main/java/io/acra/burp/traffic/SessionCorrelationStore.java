package io.acra.burp.traffic;
import io.acra.core.security.TokenFingerprint;
import java.util.concurrent.ConcurrentHashMap;
public final class SessionCorrelationStore {
    private final ConcurrentHashMap<String,String> sessions=new ConcurrentHashMap<>();
    public String sessionId(String fingerprint){ if(fingerprint==null||fingerprint.isBlank()) return ""; return sessions.computeIfAbsent(fingerprint,fp->"session-"+TokenFingerprint.sha256(fp).substring(0,16)); }
    public int size(){return sessions.size();}
}
