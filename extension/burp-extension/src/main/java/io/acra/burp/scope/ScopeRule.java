package io.acra.burp.scope;
import java.util.Locale;
public record ScopeRule(String host,int port,String scheme,String pathPrefix,String apiVersion,String environment) {
    public ScopeRule {
        host=host==null?"":host.trim().toLowerCase(Locale.ROOT); scheme=scheme==null?"":scheme.trim().toLowerCase(Locale.ROOT);
        pathPrefix=pathPrefix==null||pathPrefix.isBlank()?"/":pathPrefix; apiVersion=apiVersion==null?"":apiVersion; environment=environment==null?"UNKNOWN":environment;
        if(port<0||port>65535) throw new IllegalArgumentException("invalid port");
    }
    public boolean matches(String candidateScheme,String candidateHost,int candidatePort,String path){
        if(!host.isBlank()&&!host.equalsIgnoreCase(candidateHost)) return false;
        if(port!=0&&port!=candidatePort) return false;
        if(!scheme.isBlank()&&!scheme.equalsIgnoreCase(candidateScheme)) return false;
        return path!=null&&path.startsWith(pathPrefix);
    }
}
