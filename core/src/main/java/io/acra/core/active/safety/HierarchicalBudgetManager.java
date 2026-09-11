package io.acra.core.active.safety;

import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.evidence.SafetyEventType;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class HierarchicalBudgetManager {
    private static final class Account {
        private int allocated;
        private int reserved;
        private int executed;

        Account(int allocated) {
            this.allocated = allocated;
        }

        BudgetSnapshot snapshot() {
            return new BudgetSnapshot(allocated, reserved, executed, allocated - reserved - executed);
        }
    }

    private final TreeMap<BudgetKey, Account> accounts = new TreeMap<>();
    private final SafetyAuditLog audit;

    public HierarchicalBudgetManager(SafetyAuditLog audit) {
        if (audit == null) throw new IllegalArgumentException("audit log required");
        this.audit = audit;
    }

    public synchronized void configure(BudgetKey key, int allocated) {
        if (key == null || allocated < 0) throw new IllegalArgumentException("budget key/amount");
        Account account = accounts.computeIfAbsent(key, ignored -> new Account(allocated));
        if (allocated < account.reserved + account.executed) throw new IllegalArgumentException("allocation below consumed budget");
        account.allocated = allocated;
    }

    public synchronized boolean canReserve(List<BudgetKey> keys, int count) {
        if (keys == null || keys.isEmpty() || count < 1) return false;
        return keys.stream().distinct().allMatch(key -> {
            Account account = accounts.get(key);
            return account != null && account.snapshot().remaining() >= count;
        });
    }

    public synchronized BudgetReservation reserve(List<BudgetKey> keys, int count, String executionId, String testId) {
        List<BudgetKey> distinct = keys == null ? List.of() : keys.stream().distinct().sorted().toList();
        if (!canReserve(distinct, count)) throw new BudgetExceededException("one or more request budgets are exhausted");
        distinct.forEach(key -> accounts.get(key).reserved += count);
        audit.append(SafetyEventType.BUDGET_RESERVED, executionId, testId, Map.of("count", Integer.toString(count)));
        return new BudgetReservation(this, distinct, count);
    }

    synchronized void commit(List<BudgetKey> keys, int count) {
        keys.forEach(key -> {
            Account account = accounts.get(key);
            account.reserved -= count;
            account.executed += count;
        });
        audit.append(SafetyEventType.BUDGET_COMMITTED, "", "", Map.of("count", Integer.toString(count)));
    }

    synchronized void release(List<BudgetKey> keys, int count) {
        keys.forEach(key -> accounts.get(key).reserved -= count);
        audit.append(SafetyEventType.BUDGET_RELEASED, "", "", Map.of("count", Integer.toString(count)));
    }

    public synchronized BudgetSnapshot snapshot(BudgetKey key) {
        Account account = accounts.get(key);
        if (account == null) throw new IllegalArgumentException("budget not configured");
        return account.snapshot();
    }
}
