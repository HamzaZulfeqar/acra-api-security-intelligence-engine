package io.acra.core.tests;

import io.acra.core.domain.uri.*;
import io.acra.core.extraction.defaults.*;

public final class UriTests {
    public static int run(){int n=0; DefaultUriExtractor x=new DefaultUriExtractor();
        UriModel u=x.extract(Fixtures.tx("/api/v1/documents/%39%38%32%31")); TestSupport.assertEquals("/api/v1/documents/%39%38%32%31",u.rawPath(),"raw retained"); TestSupport.assertEquals("/api/v1/documents/9821",u.decodedPath(),"decoded retained"); TestSupport.assertEquals("/api/v1/documents/{document_id}",u.canonicalPath(),"canonical route"); n++;
        UriModel nested=x.extract(Fixtures.tx("/api/v1/users/482/documents/991")); TestSupport.assertEquals(PathSegmentClassification.RESOURCE_CANDIDATE,nested.pathSegments().get(3).classification(),"user id resource candidate"); TestSupport.assertEquals(PathSegmentClassification.RESOURCE_CANDIDATE,nested.pathSegments().get(5).classification(),"document id resource candidate"); n++;
        IdentifierDetector d=new IdentifierDetector(); var uuid=d.detect("550e8400-e29b-41d4-a716-446655440000",IdentifierLocation.PATH,"test",null); TestSupport.assertEquals(IdentifierType.UUID,uuid.type(),"uuid detection"); n++;
        UriModel amb=x.extract(Fixtures.tx("/api/a%2Fb/items/1")); TestSupport.assertEquals(4,amb.pathSegments().size(),"encoded slash must not create extra raw path segment"); TestSupport.assertContains(amb.decodedPath(),"a/b","decoded representation should expose slash divergence"); n++;
        UriModel malformed=x.extract(Fixtures.tx("/api/%ZZ/items")); TestSupport.assertContains(String.join(",",malformed.normalizationMetadata()),"malformed-uri-normalization-skipped","malformed URI should fail safe"); n++;
        UriModel q=x.extract(Fixtures.tx("/search?foo=bar")); TestSupport.assertTrue(q.identifierCandidates().isEmpty(),"foo=bar must not hallucinate identifier"); n++;
        return n;}
}
