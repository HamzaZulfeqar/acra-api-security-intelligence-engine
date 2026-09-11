package io.acra.core.planning;
import io.acra.core.domain.identity.AuthenticationType;
import java.util.List;
public record TargetProfile(String host,TargetEnvironment environment,String apiType,SetLike<AuthenticationType> authentication,List<String> versions,List<String> frameworkHints,List<String> proxyHints,boolean inScope,int requestBudget,int concurrencyLimit,boolean activeAllowed) {
    public TargetProfile {host=host==null?"":host;if(environment==null)environment=TargetEnvironment.UNKNOWN;apiType=apiType==null?"UNKNOWN":apiType;if(authentication==null)authentication=SetLike.empty();versions=List.copyOf(versions==null?List.of():versions);frameworkHints=List.copyOf(frameworkHints==null?List.of():frameworkHints);proxyHints=List.copyOf(proxyHints==null?List.of():proxyHints);if(requestBudget<0||concurrencyLimit<0)throw new IllegalArgumentException("limits");}
    public record SetLike<T>(java.util.Set<T> values){public SetLike{values=java.util.Set.copyOf(values==null?java.util.Set.of():values);}public static <T> SetLike<T> empty(){return new SetLike<>(java.util.Set.of());}}
}
