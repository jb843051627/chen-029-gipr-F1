package com.fc.v2.util;

import com.fc.v2.model.auto.TGiprCaseTrace;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 办件追溯串的数法口径。
 *
 * <p>件此刻站在第几格，一律由身后那串按语数出来；早年立下的旧件身后还没有串，
 * 才回落到行内留存的那个序号上——新件老件读法一致，两处口径不许多岔。</p>
 *
 * <p>本类全是纯函数，只出结果、不回写，回放与落库共用同一套数法。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class GiprCaseTraceSupport {

    private GiprCaseTraceSupport() {
    }

    /**
     * 由追溯串数出件此刻站在哪一格。
     *
     * <p>串里带着起件那一笔的，从头一格起数，每挪一格加一、每往回收一格减一。
     * 早年立下的旧件串里没有起件那笔（甚至整串还空着），就以行内留存的序号为底，
     * 再叠上串里的挪退——新件老件读法一致，两处口径不许多岔。</p>
     *
     * @param events      该件身后的追溯串，按落话先后排好
     * @param legacyStage 行内留存的旧序号（串里没有起件那笔时作底）
     * @return 当前格次 0..3
     */
    public static int deriveStage(List<TGiprCaseTrace> events, Integer legacyStage) {
        if (events == null || events.isEmpty()) {
            return legacyStage == null ? 0 : Math.max(0, legacyStage);
        }
        boolean hasCreate = false;
        for (TGiprCaseTrace e : events) {
            if (TGiprCaseTrace.ACTION_CREATE.equals(e.getAction())) {
                hasCreate = true;
                break;
            }
        }
        int stage = hasCreate ? 0 : (legacyStage == null ? 0 : Math.max(0, legacyStage));
        for (TGiprCaseTrace e : events) {
            if (TGiprCaseTrace.ACTION_ADVANCE.equals(e.getAction())) {
                stage += 1;
            } else if (TGiprCaseTrace.ACTION_ROLLBACK.equals(e.getAction())) {
                stage -= 1;
            }
            if (stage < 0) {
                stage = 0;
            }
        }
        return stage;
    }

    /**
     * 这一格眼下是第几次进入。
     *
     * <p>落进同一格的 CREATE（头一格）／ADVANCE／ROLLBACK 各算一次进入；
     * 退回去再走回来的，按新起的一次重新累计。串是空的旧件按头一次计。</p>
     *
     * @param events 该件身后的追溯串，按落话先后排好
     * @param stage  哪一格
     * @return 第几次进入，从 1 起
     */
    public static int currentAttempt(List<TGiprCaseTrace> events, int stage) {
        if (events == null || events.isEmpty()) {
            return 1;
        }
        int entries = 0;
        for (TGiprCaseTrace e : events) {
            Integer s = e.getStage();
            if (s == null || s != stage) {
                continue;
            }
            if (TGiprCaseTrace.ACTION_CREATE.equals(e.getAction())
                    || TGiprCaseTrace.ACTION_ADVANCE.equals(e.getAction())
                    || TGiprCaseTrace.ACTION_ROLLBACK.equals(e.getAction())) {
                entries += 1;
            }
        }
        return Math.max(1, entries);
    }

    /**
     * 某一格某一次进入留下的最早那句按语。
     *
     * <p>同一格第二次往按语栏写字，只认最早那一句；这里取的即是串里排最前的那句。</p>
     *
     * @param events  该件身后的追溯串，按落话先后排好
     * @param stage   哪一格
     * @param attempt 第几次进入
     * @return 最早那句按语；这一格这一次还没落过按语返回 null
     */
    public static TGiprCaseTrace firstAnnotation(List<TGiprCaseTrace> events, int stage, int attempt) {
        if (events == null) {
            return null;
        }
        for (TGiprCaseTrace e : events) {
            if (TGiprCaseTrace.ACTION_ANNOTATE.equals(e.getAction())
                    && e.getStage() != null && e.getStage() == stage
                    && e.getAttempt() != null && e.getAttempt() == attempt) {
                return e;
            }
        }
        return null;
    }

    /**
     * 某一格某一次进入里，核签岗落过笔的签名（去重，保持落笔先后）。
     *
     * @param events  该件身后的追溯串，按落话先后排好
     * @param stage   哪一格
     * @param attempt 第几次进入
     * @return 核签岗签名集合
     */
    public static Set<String> signers(List<TGiprCaseTrace> events, int stage, int attempt) {
        Set<String> names = new LinkedHashSet<String>();
        if (events == null) {
            return names;
        }
        for (TGiprCaseTrace e : events) {
            if (TGiprCaseTrace.ACTION_SIGN.equals(e.getAction())
                    && e.getStage() != null && e.getStage() == stage
                    && e.getAttempt() != null && e.getAttempt() == attempt
                    && e.getContent() != null && !e.getContent().trim().isEmpty()) {
                names.add(e.getContent().trim());
            }
        }
        return names;
    }
}
