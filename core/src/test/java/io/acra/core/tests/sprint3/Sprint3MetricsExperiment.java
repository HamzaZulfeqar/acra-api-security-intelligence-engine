package io.acra.core.tests.sprint3;
import io.acra.core.analysis.semantic.*;
import io.acra.core.domain.http.*;
import io.acra.core.engine.*;
import io.acra.core.openapi.*;
import io.acra.core.recon.*;
import io.acra.core.route.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import io.acra.core.tests.Fixtures;
public final class Sprint3MetricsExperiment {
    private Sprint3MetricsExperiment(){}
    public static void main(String[] args) throws Exception {
        OpenApiDocument spec=new OpenApiImporter().importText(Files.readString(Path.of("lab/openapi/acra-lab-openapi.json")),"lab/openapi/acra-lab-openapi.json");
        List<HttpTransaction> observed=List.of(
            tx(HttpMethod.GET,"/health","{}"),
            tx(HttpMethod.GET,"/api/v1/tenants/tenant-a/documents/1001","{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}"),
            tx(HttpMethod.PATCH,"/api/v1/tenants/tenant-a/documents/1001","{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}"),
            tx(HttpMethod.GET,"/api/v2/tenants/tenant-a/documents/1001","{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}"),
            tx(HttpMethod.GET,"/api/v1/tenants/tenant-a/documents","{\"items\":[{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}]}"),
            tx(HttpMethod.GET,"/api/v1/tenants/tenant-a/users/user-a","{\"id\":\"user-a\",\"tenant_id\":\"tenant-a\"}"),
            tx(HttpMethod.GET,"/api/v1/users/user-a","{\"id\":\"user-a\",\"tenant_id\":\"tenant-a\"}"),
            tx(HttpMethod.POST,"/api/v1/tenants/tenant-a/documents/1001/approve","{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\",\"state\":\"approved\"}"),
            tx(HttpMethod.GET,"/api/v1/tenants/tenant-a/documents/1001/comments","{\"items\":[{\"id\":\"c-1\",\"document_id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}]}"),
            tx(HttpMethod.GET,"/api/v1/search?tenant_id=tenant-a","{\"items\":[{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}]}"));
        SecurityContextEngine engine=new SecurityContextEngine();OpenApiTrafficCorrelator correlator=new OpenApiTrafficCorrelator();int epTp=0,epFp=0;Set<String> matched=new HashSet<>();
        for(HttpTransaction t:observed){var s=engine.analyze(t);var status=correlator.correlate(s.endpoint(),spec);if(status==OpenApiCorrelationStatus.DOCUMENTED_OBSERVED){epTp++;for(OpenApiOperation op:spec.operations())if(op.method()==s.endpoint().method()&&new RouteEquivalenceEngine().compare(s.endpoint().routeTemplate(),op.path()).kind()!=RouteEquivalenceKind.DIFFERENT)matched.add(op.key());}else epFp++;}
        int epFn=spec.operations().size()-matched.size();double epPrecision=ratio(epTp,epTp+epFp),epRecall=ratio(epTp,epTp+epFn);

        List<LabeledIdentifier> idCases=List.of(
            new LabeledIdentifier("/api/v1/tenants/17", "17", SemanticIdentifierType.TENANT_ID),
            new LabeledIdentifier("/api/v1/users/482", "482", SemanticIdentifierType.USER_ID),
            new LabeledIdentifier("/api/v1/documents/991", "991", SemanticIdentifierType.RESOURCE_ID),
            new LabeledIdentifier("/api/v1/organizations/acme", "acme", SemanticIdentifierType.ORGANIZATION_ID),
            new LabeledIdentifier("/api/v1/roles/7", "7", SemanticIdentifierType.ROLE_ID),
            new LabeledIdentifier("/api/v1/workflows/88", "88", SemanticIdentifierType.WORKFLOW_ID));
        int idTp=0,idFp=0,idFn=0;SemanticIdentifierClassifier classifier=new SemanticIdentifierClassifier();for(LabeledIdentifier c:idCases){var s=engine.analyze(tx(HttpMethod.GET,c.path,"{}"));var match=classifier.classify(s.uri()).stream().filter(x->x.candidate().value().equals(c.value)).findFirst();if(match.isPresent()&&match.get().semanticType()==c.expected)idTp++;else{if(match.isPresent())idFp++;idFn++;}}
        double idPrecision=ratio(idTp,idTp+idFp),idRecall=ratio(idTp,idTp+idFn);

        List<String[]> routeCases=List.of(
            new String[]{"/users/{id}","/users/:id","EQ"},
            new String[]{"/documents/{document_id}","/documents/<int:document_id>","EQ"},
            new String[]{"/tenants/{tenant}/documents/{id}","/tenants/:tenant/documents/:id","EQ"},
            new String[]{"/users/{id}","/admin/{id}","NE"},
            new String[]{"/documents/{id}","/documents/{id}/comments","NE"});
        int routeCorrect=0;RouteEquivalenceEngine routeEngine=new RouteEquivalenceEngine();for(String[] c:routeCases){boolean actual=Set.of(RouteEquivalenceKind.SYNTACTICALLY_EQUAL,RouteEquivalenceKind.CANONICALLY_EQUIVALENT,RouteEquivalenceKind.SAME_FAMILY).contains(routeEngine.compare(c[0],c[1]).kind());boolean expected=c[2].equals("EQ");if(actual==expected)routeCorrect++;}
        double routeAccuracy=routeCorrect/(double)routeCases.size();

        int tenantCorrect=0,ownerCorrect=0,totalContext=0,totalKnown=0,evidenceFields=0,inferredFields=0;ApiReconnaissanceEngine recon=new ApiReconnaissanceEngine();for(HttpTransaction t:observed){var s=engine.analyze(t);if(t.request().path().contains("tenant-a")&&!t.request().path().equals("/health")){if(s.tenant().resolved().map(x->x.tenantId().equals("tenant-a")).orElse(false))tenantCorrect++;}if(t.response()!=null&&t.response().bodyUtf8().contains("owner_id")){if(s.resource().resolved().map(r->"user-a".equals(r.ownerPrincipalId())).orElse(false))ownerCorrect++;}var rr=recon.analyze(t,s,spec,List.of("user-a","user-b"),List.of("document:1001","document:2001"));totalContext++;totalKnown+=rr.reconnaissance().coverage().knownCount();int inferred=0;int withEvidence=0;if(s.identity().principal().resolved().isPresent()){inferred++;if(!s.identity().principal().evidence().isEmpty())withEvidence++;}if(s.tenant().resolved().isPresent()){inferred++;if(!s.tenant().evidence().isEmpty())withEvidence++;}if(s.resource().resolved().isPresent()){inferred++;if(!s.resource().evidence().isEmpty())withEvidence++;}if(s.action().actionType()!=io.acra.core.domain.authorization.ActionType.UNKNOWN){inferred++;if(s.evidence().stream().anyMatch(e->e.location().equals("action")))withEvidence++;}inferredFields+=inferred;evidenceFields+=withEvidence;}
        long tenantEligible=observed.stream().filter(t->t.request().path().contains("tenant-a")&&!t.request().path().equals("/health")).count();long ownerEligible=observed.stream().filter(t->t.response()!=null&&t.response().bodyUtf8().contains("owner_id")).count();double tenantAccuracy=ratio(tenantCorrect,(int)tenantEligible);double ownerAccuracy=ratio(ownerCorrect,(int)ownerEligible);double avgCoverage=totalKnown/(double)(totalContext*9);double evidenceCompleteness=ratio(evidenceFields,inferredFields);

        ResponseSemanticAnalyzer rsa=new ResponseSemanticAnalyzer();var denied=rsa.fingerprint(response(200,"{\"success\":false,\"message\":\"Access denied\"}"));boolean fpSoft200=denied.responseClass()==SemanticResponseClass.ERROR_LIKE;var ra=rsa.fingerprint(response(200,"{\"id\":1001,\"timestamp\":\"a\",\"request_id\":\"x\"}"));var rb=rsa.fingerprint(response(200,"{\"request_id\":\"y\",\"timestamp\":\"b\",\"id\":1001}"));boolean fpDynamic=new ResourceSemanticMatcher().compare(ra,rb).sameResource();

        System.out.printf(Locale.ROOT,"endpoint.tp=%d endpoint.fp=%d endpoint.fn=%d endpoint.precision=%.4f endpoint.recall=%.4f%n",epTp,epFp,epFn,epPrecision,epRecall);
        System.out.printf(Locale.ROOT,"identifier.tp=%d identifier.fp=%d identifier.fn=%d identifier.precision=%.4f identifier.recall=%.4f%n",idTp,idFp,idFn,idPrecision,idRecall);
        System.out.printf(Locale.ROOT,"tenant.accuracy=%.4f owner.accuracy=%.4f route.accuracy=%.4f%n",tenantAccuracy,ownerAccuracy,routeAccuracy);
        System.out.printf(Locale.ROOT,"context.coverage=%.4f evidence.completeness=%.4f%n",avgCoverage,evidenceCompleteness);
        System.out.println("falsePositive.soft200Handled="+fpSoft200);System.out.println("falsePositive.dynamicReorderHandled="+fpDynamic);System.out.println("scope=CONTROLLED_LOCAL_GROUND_TRUTH_ONLY");System.out.println("burpRuntime=UNVERIFIED_BLOCKED");
        if(epPrecision<1.0||epRecall<1.0||idPrecision<1.0||idRecall<1.0||routeAccuracy<1.0||!fpSoft200||!fpDynamic)throw new AssertionError("controlled Sprint 3 metric gate failed");
    }
    private static HttpTransaction tx(HttpMethod method,String target,String body){List<HttpHeader> hs=target.equals("/health")?List.of():List.of(new HttpHeader("Authorization","Bearer "+Fixtures.jwt(Map.of("sub","user-a","tenant_id","tenant-a","role","viewer"))));HttpRequest req=new HttpRequest(method,"https","api.lab",443,target,hs,Map.of(),new byte[0],HttpProtocol.HTTP_1_1,new byte[0]);return new HttpTransaction(req,response(200,body),Instant.parse("2026-08-31T00:00:00Z"),"metric-"+Math.abs((method+target).hashCode()),"metric",Map.of());}
    private static HttpResponse response(int status,String body){return new HttpResponse(status,List.of(new HttpHeader("Content-Type","application/json")),body.getBytes(StandardCharsets.UTF_8),"application/json",HttpProtocol.HTTP_1_1,new byte[0]);}
    private static double ratio(int a,int b){return b==0?0.0:a/(double)b;}
    private record LabeledIdentifier(String path,String value,SemanticIdentifierType expected){}
}
