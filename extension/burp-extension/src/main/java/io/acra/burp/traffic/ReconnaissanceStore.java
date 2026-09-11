package io.acra.burp.traffic;
import io.acra.core.recon.ApiReconnaissanceResult;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class ReconnaissanceStore {
    private final ConcurrentHashMap<String,ApiReconnaissanceResult> byTransaction=new ConcurrentHashMap<>(); private final int max;
    public ReconnaissanceStore(int max){if(max<1)throw new IllegalArgumentException("max");this.max=max;}
    public void put(String transactionId,ApiReconnaissanceResult result){if(transactionId==null||transactionId.isBlank()||result==null)throw new IllegalArgumentException("transactionId/result required");byTransaction.put(transactionId,result);if(byTransaction.size()>max){byTransaction.keySet().stream().sorted().limit(byTransaction.size()-max).toList().forEach(byTransaction::remove);}}
    public Optional<ApiReconnaissanceResult> get(String transactionId){return Optional.ofNullable(byTransaction.get(transactionId));}
    public List<Map.Entry<String,ApiReconnaissanceResult>> entries(){return byTransaction.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();}
    public int size(){return byTransaction.size();}
}
