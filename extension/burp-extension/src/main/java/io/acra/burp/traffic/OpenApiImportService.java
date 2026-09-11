package io.acra.burp.traffic;
import io.acra.core.openapi.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class OpenApiImportService {
    private final TrafficIntelligencePipeline pipeline; private final OpenApiImporter importer=new OpenApiImporter();
    public OpenApiImportService(TrafficIntelligencePipeline pipeline){if(pipeline==null)throw new IllegalArgumentException("pipeline required");this.pipeline=pipeline;}
    public OpenApiDocument importText(String text,String source){OpenApiDocument d=importer.importText(text,source);pipeline.openApi(d);return d;}
    public OpenApiDocument importFile(Path path) throws IOException {if(path==null)throw new IllegalArgumentException("path required");String text=Files.readString(path,StandardCharsets.UTF_8);return importText(text,path.toAbsolutePath().normalize().toString());}
}
