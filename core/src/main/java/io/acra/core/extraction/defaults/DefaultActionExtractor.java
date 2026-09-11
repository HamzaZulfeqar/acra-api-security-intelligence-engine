package io.acra.core.extraction.defaults;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.http.*;
import io.acra.core.domain.uri.*;
import io.acra.core.extraction.ActionExtractor;
import java.util.Locale;

public final class DefaultActionExtractor implements ActionExtractor {
    @Override public Action extract(HttpTransaction tx, UriModel uri) {
        if(!uri.pathSegments().isEmpty()){
            String last=uri.pathSegments().getLast().decodedValue().toLowerCase(Locale.ROOT);
            ActionType special=switch(last){case "approve"->ActionType.APPROVE; case "share"->ActionType.SHARE; case "export"->ActionType.EXPORT; case "execute","run"->ActionType.EXECUTE; default->null;};
            if(special!=null) return new Action(special,EvidenceSource.PATH,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),last);
        }
        ActionType type=switch(tx.request().method()){
            case GET,HEAD -> ActionType.READ;
            case POST -> ActionType.CREATE;
            case PUT,PATCH -> ActionType.UPDATE;
            case DELETE -> ActionType.DELETE;
            default -> ActionType.UNKNOWN;
        };
        return new Action(type,EvidenceSource.HTTP_METHOD,type==ActionType.UNKNOWN?Confidence.unknown():Confidence.of(ConfidenceBasis.EXACT_OBSERVED),"");
    }
}
