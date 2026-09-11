package io.acra.core.recon;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class ManualContextRegistry {private final ConcurrentHashMap<String,ManualContextMapping> mappings=new ConcurrentHashMap<>();public void put(ManualContextMapping m){if(m==null||m.key().isBlank())throw new IllegalArgumentException("mapping key required");mappings.put(m.key(),m);}public Optional<ManualContextMapping> get(String key){return Optional.ofNullable(mappings.get(key));}public List<ManualContextMapping> snapshot(){return mappings.values().stream().sorted(Comparator.comparing(ManualContextMapping::key)).toList();}}
