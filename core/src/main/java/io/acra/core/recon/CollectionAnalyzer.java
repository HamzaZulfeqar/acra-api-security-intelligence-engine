package io.acra.core.recon;
import io.acra.core.analysis.semantic.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
import java.util.*;
public final class CollectionAnalyzer {
    public CollectionIntelligence collection(HttpTransaction tx,ResponseSemanticFingerprint fp){String p=tx.request().path().toLowerCase(Locale.ROOT);boolean c=fp.responseClass()==SemanticResponseClass.COLLECTION||p.endsWith("/search")||p.matches(".*/(users|documents|orders|invoices|projects|files|comments|jobs|exports)/?$");return new CollectionIntelligence(c,fp.resourceIds(),c?"collection-response-or-route":"not-observed-as-collection");}
    public PaginationIntelligence pagination(UriModel uri){TreeMap<String,String> m=new TreeMap<>();for(String k:List.of("page","offset","limit","cursor","next","previous","per_page","page_size"))if(uri.queryParameters().containsKey(k)&&!uri.queryParameters().get(k).isEmpty())m.put(k,uri.queryParameters().get(k).getFirst());return new PaginationIntelligence(m,m.containsKey("cursor")||m.containsKey("next"),m.containsKey("offset")||m.containsKey("page"));}
}
