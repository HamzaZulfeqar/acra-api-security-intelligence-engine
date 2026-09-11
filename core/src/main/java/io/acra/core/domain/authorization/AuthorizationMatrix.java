package io.acra.core.domain.authorization;
import java.util.*;
public final class AuthorizationMatrix {
    private final NavigableMap<String,AuthorizationMatrixEntry> entries=new TreeMap<>();
    public synchronized void put(AuthorizationMatrixEntry e){if(e==null)throw new IllegalArgumentException("entry required");entries.put(key(e),e);}public synchronized List<AuthorizationMatrixEntry> entries(){return List.copyOf(entries.values());}public synchronized int size(){return entries.size();}
    private static String key(AuthorizationMatrixEntry e){return String.join("|",e.principal(),e.role(),e.tenant(),e.resource(),e.action(),e.endpoint());}
}
