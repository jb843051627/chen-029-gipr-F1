package com.fc.v2;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TGiprCaseFlowMapper;
import com.fc.v2.mapper.auto.TGiprCaseTraceMapper;
import com.fc.v2.mapper.auto.TGiprHolderBookMapper;
import com.fc.v2.mapper.auto.TGiprProductBookMapper;
import com.fc.v2.model.auto.TGiprCaseFlow;
import com.fc.v2.model.auto.TGiprCaseTrace;
import com.fc.v2.model.auto.TGiprHolderBook;
import com.fc.v2.model.auto.TGiprProductBook;
import com.fc.v2.model.custom.GiprFlowResult;
import com.fc.v2.service.impl.TGiprCaseFlowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 办件流转的口径测试：四格钉死、门槛、回退、三道闩、幂等、回放不回写。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TGiprCaseFlowServiceImplTest {

    @Mock
    private TGiprCaseFlowMapper giprCaseFlowMapper;
    @Mock
    private TGiprCaseTraceMapper giprCaseTraceMapper;
    @Mock
    private TGiprHolderBookMapper giprHolderBookMapper;
    @Mock
    private TGiprProductBookMapper giprProductBookMapper;

    @InjectMocks
    private TGiprCaseFlowServiceImpl service;

    private TGiprCaseFlow row;
    private List<TGiprCaseTrace> events;

    @BeforeEach
    void setUp() {
        row = new TGiprCaseFlow();
        row.setId(1L);
        row.setBizNo("BIZ-1");
        row.setStage(0);
        row.setStatus(1);
        row.setHolderNo("H001");
        row.setProdNo("GP00");
        row.setCertFlag(0);
        row.setEndKind(0);
        events = new ArrayList<TGiprCaseTrace>();
        when(giprCaseFlowMapper.selectById(1L)).thenReturn(row);
        when(giprCaseTraceMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TGiprCaseTrace>(events));
    }

    private TGiprCaseTrace ev(String action, int stage, int attempt, String content) {
        TGiprCaseTrace e = new TGiprCaseTrace();
        e.setBizNo("BIZ-1");
        e.setAction(action);
        e.setStage(stage);
        e.setAttempt(attempt);
        e.setContent(content);
        e.setHappenedAt(new Date());
        return e;
    }

    private TGiprHolderBook holder() {
        TGiprHolderBook h = new TGiprHolderBook();
        h.setHolderNo("H001");
        h.setProdNo("GP00");
        return h;
    }

    private TGiprProductBook product(int status) {
        TGiprProductBook p = new TGiprProductBook();
        p.setProdNo("GP00");
        p.setStatus(status);
        return p;
    }

    // ---------- 起件 ----------

    @Test
    void 起件缺项要点名少了哪一样() {
        GiprFlowResult r = service.start("B1", null, "GP00", "水产类", new Date());
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("哪一家企业"));

        r = service.start("B1", "H001", null, "水产类", new Date());
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("名录产品"));

        r = service.start("B1", "H001", "GP00", null, new Date());
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("哪一类货"));

        r = service.start("B1", "H001", "GP00", "水产类", null);
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("递件那一刻"));
    }

    @Test
    void 同一件次并着进两回要幂等() {
        TGiprCaseFlow existed = new TGiprCaseFlow();
        existed.setBizNo("B1");
        when(giprCaseFlowMapper.selectOne(any())).thenReturn(existed);
        GiprFlowResult r = service.start("B1", "H001", "GP00", "水产类", new Date());
        assertTrue(r.isOk());
        assertTrue(r.getMsg().contains("幂等"));
        verify(giprCaseFlowMapper, never()).insert(any());
    }

    @Test
    void 同一企业没闭上的件至多一条() {
        TGiprCaseFlow open = new TGiprCaseFlow();
        open.setBizNo("B0");
        // 第一次 selectOne 是查件次幂等（无），第二次是查手上未闭的件（有）
        when(giprCaseFlowMapper.selectOne(any())).thenReturn(null, open);
        when(giprHolderBookMapper.selectOne(any())).thenReturn(holder());
        when(giprProductBookMapper.selectOne(any())).thenReturn(product(0));
        GiprFlowResult r = service.start("B1", "H001", "GP00", "水产类", new Date());
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("B0"));
    }

    @Test
    void 起件落在头一格() {
        when(giprCaseFlowMapper.selectOne(any())).thenReturn(null);
        when(giprHolderBookMapper.selectOne(any())).thenReturn(holder());
        when(giprProductBookMapper.selectOne(any())).thenReturn(product(0));
        GiprFlowResult r = service.start("B1", "H001", "GP00", "水产类", new Date());
        assertTrue(r.isOk());
        assertEquals(0, r.getStage().intValue());
        ArgumentCaptor<TGiprCaseTrace> cap = ArgumentCaptor.forClass(TGiprCaseTrace.class);
        verify(giprCaseTraceMapper).insert(cap.capture());
        assertEquals(TGiprCaseTrace.ACTION_CREATE, cap.getValue().getAction());
        assertNotNull(cap.getValue().getHappenedAt());
    }

    // ---------- 推进 ----------

    @Test
    void 一笔跨两格递上来系统不认_件停在原格() {
        GiprFlowResult r = service.advance(1L, 2, "冲两格");
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("跨两格"));
        assertEquals(0, row.getStage().intValue());
        verify(giprCaseFlowMapper, never()).updateById(any(TGiprCaseFlow.class));
    }

    @Test
    void 推进一次只挪一格_不多冲不停滞() {
        when(giprHolderBookMapper.selectOne(any())).thenReturn(holder());
        when(giprProductBookMapper.selectOne(any())).thenReturn(product(0));
        GiprFlowResult r = service.advance(1L, null, "收件齐");
        assertTrue(r.isOk());
        assertEquals(1, row.getStage().intValue());
        assertEquals(1, r.getStage().intValue());
    }

    @Test
    void 名录已移出的初审这格进不去() {
        when(giprHolderBookMapper.selectOne(any())).thenReturn(holder());
        when(giprProductBookMapper.selectOne(any())).thenReturn(product(1));
        GiprFlowResult r = service.advance(1L, null, "往前");
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("移出或并走"));
        assertEquals(0, row.getStage().intValue());
    }

    @Test
    void 初审没记可核准这道门不开() {
        row.setStage(1);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "缓一缓"));
        GiprFlowResult r = service.advance(1L, null, "进核准");
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("可"));
        assertEquals(1, row.getStage().intValue());
    }

    @Test
    void 发标要两名核签岗签齐且记号写回本行() {
        row.setStage(2);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "可"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, "进核准"));
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));

        // 只有一岗、记号未写回：进不去
        GiprFlowResult r = service.advance(1L, null, "进发标");
        assertFalse(r.isOk());
        assertEquals(2, row.getStage().intValue());

        // 第二岗落笔，记号写回本行
        r = service.sign(1L, "乙岗");
        assertTrue(r.isOk());
        assertEquals(1, row.getCertFlag().intValue());
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "乙岗"));

        r = service.advance(1L, null, "进发标");
        assertTrue(r.isOk());
        assertEquals(3, row.getStage().intValue());
    }

    @Test
    void 走满末一格就此闭上() {
        row.setStage(3);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "一"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, "二"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 3, 1, "三"));
        GiprFlowResult r = service.advance(1L, null, "发标讫");
        assertTrue(r.isOk());
        assertEquals(2, row.getStatus().intValue());
    }

    // ---------- 回退 ----------

    @Test
    void 站在头一格再退没处可退() {
        GiprFlowResult r = service.rollback(1L, "退");
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("没处可退"));
    }

    @Test
    void 回退一次只退一格_按语原样挪到串末尾() {
        row.setStage(1);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "初审那句原样话"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审"));
        GiprFlowResult r = service.rollback(1L, "材料退补");
        assertTrue(r.isOk());
        assertEquals(0, row.getStage().intValue());

        ArgumentCaptor<TGiprCaseTrace> cap = ArgumentCaptor.forClass(TGiprCaseTrace.class);
        verify(giprCaseTraceMapper).insert(cap.capture());
        TGiprCaseTrace tail = cap.getValue();
        assertEquals(TGiprCaseTrace.ACTION_ROLLBACK, tail.getAction());
        assertEquals("初审那句原样话", tail.getContent());
        assertEquals(0, tail.getStage().intValue());
        // 再走回头一格按新起的一次重新累计
        assertEquals(2, tail.getAttempt().intValue());
    }

    // ---------- 按语 ----------

    @Test
    void 同一格第二次落字只认最早那句() {
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        GiprFlowResult r = service.annotate(1L, "最早那句");
        assertTrue(r.isOk());
        assertEquals("最早那句", row.getContent());
        // 库里的串随之多了一句（此处以手工补进模拟落库后的样子）
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 0, 1, "最早那句"));

        r = service.annotate(1L, "后写的那句");
        assertTrue(r.isOk());
        assertTrue(r.getMsg().contains("最早"));
        // 后写的顶不掉、也不另存一份：串里只落过一笔按语
        assertEquals("最早那句", row.getContent());
        verify(giprCaseTraceMapper, times(1)).insert(any());
    }

    // ---------- 三道闩 ----------

    @Test
    void 闭上的件补按语改内容抽走三样一概办不了() {
        row.setStatus(2);
        assertFalse(service.annotate(1L, "补一句").isOk());
        assertFalse(service.updateContent(1L, "改一处"));
        assertFalse(service.remove(1L));
        assertFalse(service.advance(1L, null, "再挪").isOk());
        assertFalse(service.rollback(1L, "再退").isOk());
        verify(giprCaseFlowMapper, never()).updateById(any(TGiprCaseFlow.class));
        verify(giprCaseFlowMapper, never()).deleteById(any(Long.class));
    }

    // ---------- 了结 ----------

    @Test
    void 撤回与并走了结后不再往前() {
        GiprFlowResult r = service.terminate(1L, 1);
        assertTrue(r.isOk());
        assertEquals(2, row.getStatus().intValue());
        assertEquals(1, row.getEndKind().intValue());
        assertFalse(service.advance(1L, null, "再挪").isOk());

        assertFalse(service.terminate(1L, 2).isOk());
    }

    // ---------- 回放 ----------

    @Test
    void 回放只出不回写_倒着给出整串() {
        row.setStage(2);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审"));
        events.add(ev(TGiprCaseTrace.ACTION_ANNOTATE, 1, 1, "可"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, "进核准"));
        GiprFlowResult r = service.replay(1L);
        assertTrue(r.isOk());
        assertEquals(4, r.getTrace().size());
        // 倒着：末一格的在前
        assertEquals(TGiprCaseTrace.ACTION_ADVANCE, r.getTrace().get(0).getAction());
        assertEquals(2, r.getTrace().get(0).getStage().intValue());
        assertEquals(2, r.getStage().intValue());
        // 不回写
        verify(giprCaseFlowMapper, never()).updateById(any(TGiprCaseFlow.class));
        verify(giprCaseTraceMapper, never()).insert(any());
    }

    @Test
    void 回放断带要点明哪一格缺哪一栏() {
        TGiprCaseTrace broken = ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审");
        broken.setHappenedAt(null);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(broken);
        GiprFlowResult r = service.replay(1L);
        assertFalse(r.isOk());
        assertTrue(r.getMsg().contains("第 1 格"));
        assertTrue(r.getMsg().contains("happened_at"));
    }

    // ---------- 核签 ----------

    @Test
    void 核签只在核准那一格收_同岗重复落笔幂等() {
        assertFalse(service.sign(1L, "甲岗").isOk());

        row.setStage(2);
        events.add(ev(TGiprCaseTrace.ACTION_CREATE, 0, 1, "起件"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 1, 1, "进初审"));
        events.add(ev(TGiprCaseTrace.ACTION_ADVANCE, 2, 1, "进核准"));

        GiprFlowResult r = service.sign(1L, "甲岗");
        assertTrue(r.isOk());
        assertEquals(0, row.getCertFlag().intValue());

        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));
        r = service.sign(1L, "甲岗");
        assertTrue(r.isOk());
        assertTrue(r.getMsg().contains("幂等"));
        assertEquals(0, row.getCertFlag().intValue());
    }

    @Test
    void 旧件串里没起件那笔照行内序号作底_两处口径一致() {
        // 早年立下的件：行内存着 stage=2，串里只有后来补的核签，没有起件那笔
        row.setStage(2);
        row.setCertFlag(1);
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "甲岗"));
        events.add(ev(TGiprCaseTrace.ACTION_SIGN, 2, 1, "乙岗"));
        GiprFlowResult r = service.replay(1L);
        assertTrue(r.isOk());
        assertEquals(2, r.getStage().intValue());
    }

    @Test
    void 了结方式只认撤回与并走() {
        GiprFlowResult r = service.terminate(1L, 9);
        assertFalse(r.isOk());
        assertEquals(0, row.getEndKind().intValue());
        assertEquals(1, row.getStatus().intValue());
    }
}
