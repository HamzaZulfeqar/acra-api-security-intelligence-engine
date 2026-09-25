package io.acra.lab;

import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class RuntimeApplication {
  public static void main(String[] args){ SpringApplication.run(RuntimeApplication.class,args); }

  @GetMapping("/health")
  public Map<String,Object> health(){ return Map.of("status","ok","framework","spring"); }

  @GetMapping("/runtime/spring/assets/{assetId}")
  public Map<String,Object> asset(@PathVariable String assetId){
    String owner=assetId.equals("asset-red")?"alice":"bob";
    String tenant=assetId.equals("asset-red")?"red":"blue";
    return Map.of("id",assetId,"ownerId",owner,"tenantId",tenant);
  }

  @GetMapping("/runtime/spring/workspaces/{tenant}/reports/{reportId}")
  public Map<String,Object> report(@PathVariable String tenant,@PathVariable String reportId){
    return Map.of("id",reportId,"tenantId",tenant);
  }

  @GetMapping("/runtime/spring/management/audit")
  public Map<String,Object> audit(@RequestHeader(value="X-Role",defaultValue="") String role){
    return Map.of("area","audit","role",role,"requiredRole","security-admin");
  }

  @PostMapping("/runtime/spring/workflows/release/resources/{resourceId}/transition")
  public Map<String,Object> workflow(@PathVariable String resourceId,@RequestHeader(value="X-Role",defaultValue="") String role,@RequestBody Map<String,Object> body){
    Map<String,Object> out=new LinkedHashMap<>(body); out.put("resourceId",resourceId); out.put("state",body.get("toState")); out.put("role",role); return out;
  }

  @GetMapping("/runtime/spring/routing/equivalent")
  public Map<String,Object> routing(@RequestHeader(value="X-Role",defaultValue="") String role){
    return Map.of("area","route","role",role,"routeForm","canonical");
  }

  @PatchMapping("/runtime/spring/accounts/{account}/preferences")
  public Map<String,Object> prefs(@PathVariable String account,@RequestBody Map<String,Object> body){
    List<String> fields=new ArrayList<>(body.keySet()); Collections.sort(fields);
    return Map.of("id",account,"appliedProperties",fields);
  }

  @PostMapping("/runtime/spring/assets/bulk-read")
  public Map<String,Object> bulk(@RequestBody Map<String,Object> body){
    Object idsObj=body.get("resourceIds"); List<Map<String,Object>> items=new ArrayList<>();
    if(idsObj instanceof List<?> ids){ for(Object raw:ids){ String id=String.valueOf(raw); String owner=id.equals("asset-red")?"alice":"bob"; String tenant=id.equals("asset-red")?"red":"blue"; items.add(Map.of("resourceId",id,"decision","ALLOW","ownerId",owner,"tenantId",tenant)); } }
    return Map.of("items",items);
  }

  @GetMapping("/runtime/spring/references/{alias}")
  public Map<String,Object> reference(@PathVariable String alias){
    String id=alias.equals("ref-red")?"asset-red":"asset-blue"; String owner=id.equals("asset-red")?"alice":"bob"; String tenant=id.equals("asset-red")?"red":"blue";
    return Map.of("alias",alias,"resolvedResourceId",id,"resource",Map.of("id",id,"ownerId",owner,"tenantId",tenant));
  }
}
