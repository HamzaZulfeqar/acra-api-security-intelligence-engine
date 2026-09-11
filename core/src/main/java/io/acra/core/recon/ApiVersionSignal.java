package io.acra.core.recon;
import io.acra.core.domain.common.Confidence;
public record ApiVersionSignal(String version, String source, Confidence confidence) {
    public ApiVersionSignal {
        version=version==null?"":version; source=source==null?"":source; if(confidence==null) confidence=Confidence.unknown();
    }
}
