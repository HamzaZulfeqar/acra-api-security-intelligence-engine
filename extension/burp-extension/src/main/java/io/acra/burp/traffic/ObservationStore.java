package io.acra.burp.traffic;
import io.acra.core.domain.observation.*;
import io.acra.burp.model.ContextObservation;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
public final class ObservationStore {
    private final ConcurrentLinkedDeque<TrafficObservation> observations=new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<EvidenceTimelineEntry> timeline=new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ContextObservation> contexts=new ConcurrentLinkedDeque<>();
    private final int max;
    public ObservationStore(int max){if(max<1)throw new IllegalArgumentException("max must be positive");this.max=max;}
    public void add(TrafficObservation o,ContextObservation context,List<EvidenceTimelineEntry> entries){observations.addLast(o); if(context!=null) contexts.addLast(context); if(entries!=null) timeline.addAll(entries);trim();}
    private void trim(){while(observations.size()>max) observations.pollFirst(); while(contexts.size()>max) contexts.pollFirst(); while(timeline.size()>max*4L) timeline.pollFirst();}
    public List<TrafficObservation> observations(){return List.copyOf(observations);}
    public List<EvidenceTimelineEntry> timeline(){return List.copyOf(timeline);}
    public List<ContextObservation> contexts(){return List.copyOf(contexts);}
    public Optional<ContextObservation> context(String transactionId){return contexts.stream().filter(c->c.transactionId().equals(transactionId)).findFirst();}
    public List<EvidenceTimelineEntry> timeline(String transactionId){return timeline.stream().filter(e->e.transactionId().equals(transactionId)).toList();}
    public int size(){return observations.size();}
}
