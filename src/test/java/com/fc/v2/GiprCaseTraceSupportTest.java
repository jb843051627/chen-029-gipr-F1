package com.fc.v2;

import com.fc.v2.model.auto.TGiprCaseTrace;
import com.fc.v2.util.GiprCaseTraceSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 追溯串数法口径的单元测试：数格、第几次进入、最早按语、核签去重、旧件兼容。
 */
class GiprCaseTraceSupportTest {

    private TGiprCaseTrace ev(String action, int stage, int attempt, String content) {
        TGiprCaseTrace e = new TGiprCaseTrace();
        e.setAction(action);
        e.setStage(stage);
        e.setAttempt(attempt);
        e.setContent(content);
        e.setHappenedAt(new Date());
        return e;
    }

    @Test
    void 空串回落到行内旧序号_兼容早年立下的件() {
        assertEquals(3, GiprCaseTraceSupport.deriveStage(new ArrayList<TGiprCaseTrace>(), 3));
        assertEquals(0, GiprCaseTraceSupport.deriveStage(new ArrayList<TGiprCaseTrace>(), null));
        assertEquals(1, GiprCaseTraceSupport.currentAttempt(new ArrayList<TGiprCaseTrace>(), 2));
    }

    @Test
    void 串里没起件那笔时以行内旧序号为底再叠挪退() {
        List<TGiprCaseTrace> events = new ArrayList<TGiprCaseTrace>();
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));
        // 旧件：串里只有后补的话，没有起件那笔 → 以行内的 2 为底
        assertEquals(2, GiprCaseTraceSupport.deriveStage(events, 2));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 3, 1, "补挪一格"));
        assertEquals(3, GiprCaseTraceSupport.deriveStage(events, 2));
        events.add(ev(TGiprCaseTrace.ACTION_ROLLBACK, 2, 2, "退回"));
        assertEquals(2, GiprCaseTraceSupport.deriveStage(events, 2));
    }

    @Test
    void 当前格由串数出来_不跟报面序号走() {
        List<TGiprCaseTrace> events = new ArrayList<TGiprCaseTrace>();
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "往前"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, "往前"));
        assertEquals(2, GiprCaseTraceSupport.deriveStage(events, 0));

        events.add(ev(TGiprCaseTrace.ACTION_ROLLBACK, 1, 2, "原样挪回的按语"));
        assertEquals(1, GiprCaseTraceSupport.deriveStage(events, 0));
    }

    @Test
    void 退回再走回按新起的一次重新累计() {
        List<TGiprCaseTrace> events = new ArrayList<TGiprCaseTrace>();
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, null));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, null));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, null));
        events.add(ev(TGiprCaseTrace.ACTION_ROLLBACK, 1, 2, null));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 2, null));

        assertEquals(2, GiprCaseTraceSupport.currentAttempt(events, 1));
        assertEquals(2, GiprCaseTraceSupport.currentAttempt(events, 2));
        assertEquals(1, GiprCaseTraceSupport.currentAttempt(events, 0));
        assertEquals(2, GiprCaseTraceSupport.deriveStage(events, 0));
    }

    @Test
    void 同一格第二次落字只认最早那一句() {
        List<TGiprCaseTrace> events = new ArrayList<TGiprCaseTrace>();
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "可"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "顶不掉的第二句"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 2, "新一次的另一句"));

        assertEquals("可", GiprCaseTraceSupport.firstAnnotation(events, 1, 1).getContent());
        assertEquals("新一次的另一句", GiprCaseTraceSupport.firstAnnotation(events, 1, 2).getContent());
        assertNull(GiprCaseTraceSupport.firstAnnotation(events, 2, 1));
    }

    @Test
    void 核签岗去重且保序() {
        List<TGiprCaseTrace> events = new ArrayList<TGiprCaseTrace>();
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, " 乙岗 "));
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "  "));
        Set<String> signers = GiprCaseTraceSupport.signers(events, 2, 1);
        assertEquals(2, signers.size());
        assertTrue(signers.contains("甲岗"));
        assertTrue(signers.contains("乙岗"));
    }
}
