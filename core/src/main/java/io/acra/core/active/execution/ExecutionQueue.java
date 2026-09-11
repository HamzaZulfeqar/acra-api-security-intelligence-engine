package io.acra.core.active.execution;

import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.evidence.SafetyEventType;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.testing.TestState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ExecutionQueue {
    private static final class Entry {
        private final SecurityTest test;
        private final TestPriority priority;
        private final long sequence;
        private TestState state;
        private int attempts;
        private String reason;

        Entry(SecurityTest test, TestPriority priority, long sequence, TestState state, String reason) {
            this.test = test;
            this.priority = priority;
            this.sequence = sequence;
            this.state = state;
            this.reason = reason;
        }

        QueueEntrySnapshot snapshot() {
            return new QueueEntrySnapshot(test.testId(), test.signature(), state, priority, sequence, attempts, reason);
        }
    }

    private static final Comparator<Entry> ORDER = Comparator
            .comparingLong((Entry entry) -> entry.priority.score()).reversed()
            .thenComparing(Comparator.comparingInt((Entry entry) -> entry.test.priority()).reversed())
            .thenComparingLong(entry -> entry.sequence);

    private final PriorityQueue<Entry> queued = new PriorityQueue<>(ORDER);
    private final Map<String, Entry> byId = new HashMap<>();
    private final Set<String> signatures = new HashSet<>();
    private final AtomicLong sequence = new AtomicLong();
    private final SafetyAuditLog audit;

    public ExecutionQueue(SafetyAuditLog audit) {
        if (audit == null) throw new IllegalArgumentException("audit log required");
        this.audit = audit;
    }

    public synchronized QueueEntrySnapshot enqueue(SecurityTest test, TestPriority priority) {
        if (test == null || priority == null) throw new IllegalArgumentException("test and priority required");
        if (byId.containsKey(test.testId())) throw new IllegalArgumentException("testId already queued");
        boolean duplicate = !signatures.add(test.signature());
        Entry entry = new Entry(test, priority, sequence.incrementAndGet(),
                duplicate ? TestState.SKIPPED : TestState.QUEUED,
                duplicate ? "duplicate canonical test signature" : "");
        byId.put(test.testId(), entry);
        if (!duplicate) queued.add(entry);
        return entry.snapshot();
    }

    public synchronized Optional<SecurityTest> startNext() {
        List<Entry> deferred = new ArrayList<>();
        try {
            while (!queued.isEmpty()) {
                Entry entry = queued.poll();
                requireState(entry, TestState.QUEUED);
                String failedDependency = failedDependency(entry);
                if (!failedDependency.isEmpty()) {
                    entry.state = TestState.BLOCKED;
                    entry.reason = "dependency did not complete: " + failedDependency;
                    continue;
                }
                if (!dependenciesComplete(entry)) {
                    deferred.add(entry);
                    continue;
                }
                entry.state = TestState.RUNNING;
                entry.attempts++;
                entry.reason = "";
                return Optional.of(entry.test);
            }
            return Optional.empty();
        } finally {
            queued.addAll(deferred);
        }
    }

    public synchronized QueueEntrySnapshot pause(String testId) {
        Entry entry = entry(testId);
        if (entry.state == TestState.QUEUED) queued.remove(entry);
        if (entry.state != TestState.QUEUED && entry.state != TestState.RUNNING) {
            throw new IllegalStateException("only queued or running tests may pause");
        }
        entry.state = TestState.PAUSED;
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot resume(String testId) {
        Entry entry = entry(testId);
        requireState(entry, TestState.PAUSED);
        entry.state = TestState.QUEUED;
        entry.reason = "";
        queued.add(entry);
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot complete(String testId) { return transition(testId, TestState.RUNNING, TestState.COMPLETED, ""); }
    public synchronized QueueEntrySnapshot fail(String testId, String reason) { return transition(testId, TestState.RUNNING, TestState.FAILED, requiredReason(reason)); }
    public synchronized QueueEntrySnapshot block(String testId, String reason) {
        Entry entry = entry(testId);
        if (entry.state == TestState.QUEUED) queued.remove(entry);
        if (entry.state != TestState.QUEUED && entry.state != TestState.RUNNING && entry.state != TestState.PAUSED) {
            throw new IllegalStateException("test cannot be blocked from state " + entry.state);
        }
        entry.state = TestState.BLOCKED;
        entry.reason = requiredReason(reason);
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot cancel(String testId, String reason) {
        Entry entry = entry(testId);
        if (entry.state == TestState.QUEUED) queued.remove(entry);
        if (terminal(entry.state)) throw new IllegalStateException("terminal test cannot be cancelled");
        entry.state = TestState.CANCELLED;
        entry.reason = requiredReason(reason);
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot stop(String testId, String reason) {
        return cancel(testId, reason);
    }

    public synchronized QueueEntrySnapshot skip(String testId, String reason) {
        Entry entry = entry(testId);
        requireState(entry, TestState.QUEUED);
        queued.remove(entry);
        entry.state = TestState.SKIPPED;
        entry.reason = requiredReason(reason);
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot retry(String testId) {
        Entry entry = entry(testId);
        if (entry.state != TestState.FAILED && entry.state != TestState.BLOCKED) {
            throw new IllegalStateException("only failed or blocked tests may retry");
        }
        entry.state = TestState.QUEUED;
        entry.reason = "";
        queued.add(entry);
        return entry.snapshot();
    }

    public synchronized QueueEntrySnapshot rerun(String testId) {
        Entry entry = entry(testId);
        if (entry.state != TestState.COMPLETED && entry.state != TestState.CANCELLED && entry.state != TestState.SKIPPED) {
            throw new IllegalStateException("only terminal completed/cancelled/skipped tests may rerun");
        }
        entry.state = TestState.QUEUED;
        entry.reason = "";
        queued.add(entry);
        return entry.snapshot();
    }

    public synchronized List<QueueEntrySnapshot> stopAll(String reason) {
        List<QueueEntrySnapshot> cancelled = new ArrayList<>();
        for (Entry entry : byId.values()) {
            if (entry.state == TestState.QUEUED || entry.state == TestState.RUNNING || entry.state == TestState.PAUSED) {
                entry.state = TestState.CANCELLED;
                entry.reason = requiredReason(reason);
                cancelled.add(entry.snapshot());
            }
        }
        queued.clear();
        audit.append(SafetyEventType.QUEUE_CANCELLED, "", "", Map.of("count", Integer.toString(cancelled.size()), "reason", requiredReason(reason)));
        return List.copyOf(cancelled);
    }

    public synchronized List<QueueEntrySnapshot> snapshots() {
        return byId.values().stream().map(Entry::snapshot)
                .sorted(Comparator.comparingLong(QueueEntrySnapshot::sequence)).toList();
    }

    public synchronized QueueEntrySnapshot snapshot(String testId) { return entry(testId).snapshot(); }
    public synchronized Optional<SecurityTest> test(String testId) { return Optional.ofNullable(byId.get(testId)).map(entry -> entry.test); }

    private QueueEntrySnapshot transition(String testId, TestState expected, TestState next, String reason) {
        Entry entry = entry(testId);
        requireState(entry, expected);
        entry.state = next;
        entry.reason = reason;
        return entry.snapshot();
    }

    private Entry entry(String testId) {
        Entry entry = byId.get(testId);
        if (entry == null) throw new IllegalArgumentException("unknown testId");
        return entry;
    }

    private boolean dependenciesComplete(Entry entry) {
        return entry.test.dependencies().stream().allMatch(dependency -> {
            Entry required = byId.get(dependency);
            return required != null && required.state == TestState.COMPLETED;
        });
    }

    private String failedDependency(Entry entry) {
        return entry.test.dependencies().stream().filter(dependency -> {
            Entry required = byId.get(dependency);
            return required == null || required.state == TestState.FAILED || required.state == TestState.CANCELLED
                    || required.state == TestState.SKIPPED || required.state == TestState.BLOCKED;
        }).findFirst().orElse("");
    }

    private static void requireState(Entry entry, TestState expected) {
        if (entry.state != expected) throw new IllegalStateException("expected " + expected + " but was " + entry.state);
    }

    private static boolean terminal(TestState state) {
        return state == TestState.COMPLETED || state == TestState.FAILED || state == TestState.CANCELLED
                || state == TestState.SKIPPED || state == TestState.BLOCKED;
    }

    private static String requiredReason(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason required");
        return reason;
    }
}
