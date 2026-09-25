package io.acra.lab.eval;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
@SpringBootApplication
@RestController
public class EvalApplication {
 public static void main(String[] args){SpringApplication.run(EvalApplication.class,args);}
 @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","ok","framework","spring-eval");}
 @GetMapping("/runtime-eval/spring/documents/{docId}") public Map<String,Object> document(@PathVariable String docId){String owner=docId.equals("doc-alpha")?"robin":"casey";String tenant=docId.equals("doc-alpha")?"alpha":"beta";return Map.of("id",docId,"ownerId",owner,"tenantId",tenant);}
 @GetMapping("/runtime-eval/spring/organizations/{tenant}/reports/{reportId}") public Map<String,Object> report(@PathVariable String tenant,@PathVariable String reportId){return Map.of("id",reportId,"tenantId",tenant);}
 @GetMapping("/runtime-eval/spring/admin/security-log") public Map<String,Object> rbac(@RequestHeader(value="X-Role",defaultValue="") String role){return Map.of("area","security-log","role",role,"requiredRole","audit-admin");}
 @PostMapping("/runtime-eval/spring/workflows/deploy/resources/{resourceId}/transition") public Map<String,Object> workflow(@PathVariable String resourceId,@RequestHeader(value="X-Role",defaultValue="") String role,@RequestBody Map<String,Object> body){Map<String,Object> out=new LinkedHashMap<>(body);out.put("resourceId",resourceId);out.put("state",body.get("toState"));out.put("role",role);return out;}
 @GetMapping("/runtime-eval/spring/routing/equivalent") public Map<String,Object> routing(@RequestHeader(value="X-Role",defaultValue="") String role){return Map.of("area","route","role",role,"routeForm","canonical");}
 @PatchMapping("/runtime-eval/spring/profiles/{account}/settings") public Map<String,Object> settings(@PathVariable String account,@RequestBody Map<String,Object> body){List<String> f=new ArrayList<>(body.keySet());Collections.sort(f);return Map.of("id",account,"appliedProperties",f);}
 @PostMapping("/runtime-eval/spring/documents/bulk-fetch") public Map<String,Object> bulk(@RequestBody Map<String,Object> body){List<Map<String,Object>> items=new ArrayList<>();Object idsObj=body.get("resourceIds");if(idsObj instanceof List<?> ids){for(Object raw:ids){String id=String.valueOf(raw);String owner=id.equals("doc-alpha")?"robin":"casey";String tenant=id.equals("doc-alpha")?"alpha":"beta";items.add(Map.of("resourceId",id,"decision","ALLOW","ownerId",owner,"tenantId",tenant));}}return Map.of("items",items);}
 @GetMapping("/runtime-eval/spring/links/{alias}") public Map<String,Object> link(@PathVariable String alias){String id=alias.equals("link-alpha")?"doc-alpha":"doc-beta";String owner=id.equals("doc-alpha")?"robin":"casey";String tenant=id.equals("doc-alpha")?"alpha":"beta";return Map.of("alias",alias,"resolvedResourceId",id,"resource",Map.of("id",id,"ownerId",owner,"tenantId",tenant));}
}
