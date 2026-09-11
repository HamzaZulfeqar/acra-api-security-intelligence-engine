package io.acra.core.recon;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class IdentityConfirmationRegistry {
    private final ConcurrentHashMap<String,ConfirmedIdentityContext> contexts=new ConcurrentHashMap<>();
    public ConfirmedIdentityContext confirmUser(String tokenFingerprint,String principalId,String role,String tenantId,String source){var c=new ConfirmedIdentityContext(tokenFingerprint,principalId,role,tenantId,IdentityConfidenceState.USER_CONFIRMED,source,Instant.now());contexts.put(tokenFingerprint,c);return c;}
    public ConfirmedIdentityContext confirmLab(String tokenFingerprint,String principalId,String role,String tenantId,String source){var c=new ConfirmedIdentityContext(tokenFingerprint,principalId,role,tenantId,IdentityConfidenceState.LAB_CONFIRMED,source,Instant.now());contexts.put(tokenFingerprint,c);return c;}
    public Optional<ConfirmedIdentityContext> find(String tokenFingerprint){return Optional.ofNullable(contexts.get(tokenFingerprint));}
    public List<ConfirmedIdentityContext> snapshot(){return contexts.values().stream().sorted(Comparator.comparing(ConfirmedIdentityContext::tokenFingerprint)).toList();}
    public int size(){return contexts.size();}
}
