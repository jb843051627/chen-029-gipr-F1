package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.fc.v2.service.ITGiprCaseFlowService;
import com.fc.v2.util.GiprCaseTraceSupport;

/**
 * 专用标志使用核准办件流转单 Service业务层处理（state-machine 形状：单据流转）
 *
 * <p>四格钉死：0收件 → 1初审 → 2核准 → 3备案发标。件站在哪一格由追溯串数出来，
 * 落库、取用都按数出来的那个；报面序号只供人眼对着看。每落一句按语，紧跟着记下
 * 落在哪一刻，按先后串在件身后，供倒着回放与年底审计取数。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TGiprCaseFlowServiceImpl implements ITGiprCaseFlowService {

    /** 末一格（备案发标） */
    private static final int MAX_STAGE = 3;
    /** 格次：0收件 1初审 2核准 3备案发标 */
    private static final int STAGE_INTAKE = 0;
    private static final int STAGE_CITY_REVIEW = 1;
    private static final int STAGE_PROV_APPROVE = 2;
    /** 单子情形：1在办 2已封住（闭上／了结都算，未闭的件一企至多一条） */
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_CLOSED = 2;
    /** 标志核发记号 */
    private static final int CERT_NONE = 0;
    private static final int CERT_ISSUED = 1;
    /** 了结方式 */
    private static final int END_NONE = 0;
    private static final int END_WITHDRAW = 1;
    private static final int END_MERGED = 2;
    /** 名录情形：1已移出（并走同论） */
    private static final int PROD_REMOVED = 1;
    /** 初审那句记成「可」，核准这道门才开 */
    private static final int NEED_SIGNERS = 2;
    private static final String PASS_WORD = "可";

    @javax.annotation.Resource
    private TGiprCaseFlowMapper giprCaseFlowMapper;

    @javax.annotation.Resource
    private TGiprCaseTraceMapper giprCaseTraceMapper;

    @javax.annotation.Resource
    private TGiprHolderBookMapper giprHolderBookMapper;

    @javax.annotation.Resource
    private TGiprProductBookMapper giprProductBookMapper;

    @Override
    public TGiprCaseFlow selectTGiprCaseFlowById(Long id) {
        return this.giprCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TGiprCaseFlow> selectTGiprCaseFlowList(QueryWrapper<TGiprCaseFlow> queryWrapper) {
        return this.giprCaseFlowMapper.selectList(queryWrapper);
    }

    /**
     * 起一条件。同一刻两路各递一笔的，在此分开排队、各自留痕；
     * 同一件次并着进两回的，幂等返回原件，不另立。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized GiprFlowResult start(String bizNo, String holderNo, String prodNo,
                                             String goodsKind, Date submitTime) {
        // 四样少一样就起不来，回话点名少了哪一样
        if (isBlank(holderNo)) {
            return GiprFlowResult.fail("起件缺项：哪一家企业（授权册上的代号）没填");
        }
        if (isBlank(prodNo)) {
            return GiprFlowResult.fail("起件缺项：挂哪一个名录产品没填");
        }
        if (isBlank(goodsKind)) {
            return GiprFlowResult.fail("起件缺项：拟贴在哪一类货上没填");
        }
        if (submitTime == null) {
            return GiprFlowResult.fail("起件缺项：递件那一刻没填");
        }
        if (isBlank(bizNo)) {
            return GiprFlowResult.fail("起件缺项：件次没排上");
        }

        // 同一件次并着进两回：幂等，返回原件
        TGiprCaseFlow existed = findByBizNo(bizNo);
        if (existed != null) {
            GiprFlowResult r = GiprFlowResult.ok("同一件次已立过，幂等返回原件", existed);
            r.setStage(existed.getStage());
            return r;
        }

        // 授权册上得有这一个代号
        TGiprHolderBook holder = findHolder(holderNo);
        if (holder == null) {
            return GiprFlowResult.fail("授权册上查无此企业代号：" + holderNo);
        }
        // 名录底册里得有这一个产品
        if (findProduct(prodNo) == null) {
            return GiprFlowResult.fail("名录底册里查无此产品代号：" + prodNo);
        }
        // 同一企业手上没闭上的件至多一条（正在办的、半道歇下的都算）
        TGiprCaseFlow openCase = findOpenCaseOfHolder(holderNo);
        if (openCase != null) {
            return GiprFlowResult.fail("该企业手上还有没闭上的件（件次 "
                    + openCase.getBizNo() + "），下一条起不来");
        }

        TGiprCaseFlow row = new TGiprCaseFlow();
        row.setBizNo(bizNo);
        row.setStage(STAGE_INTAKE);
        row.setStatus(STATUS_ACTIVE);
        row.setHolderNo(holderNo);
        row.setProdNo(prodNo);
        row.setGoodsKind(goodsKind);
        row.setSubmitTime(submitTime);
        row.setCertFlag(CERT_NONE);
        row.setEndKind(END_NONE);
        row.setLastAction("起件");
        this.giprCaseFlowMapper.insert(row);

        append(bizNo, STAGE_INTAKE, 1, TGiprCaseTrace.ACTION_CREATE, "起件收件");

        GiprFlowResult r = GiprFlowResult.ok("件已立，站在收件一格", row);
        r.setStage(STAGE_INTAKE);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GiprFlowResult advance(Long id, Integer formStage, String remark) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        if (isClosed(row)) {
            return GiprFlowResult.fail("这一件已闭上（或已了结），动不得了");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());

        // 报面序号只供对着看：比数出来的多冲两格以上的，这一笔不认，件仍停在原先那一格
        if (formStage != null && formStage > cur + 1) {
            return GiprFlowResult.fail("一笔想跨两格（报面序号 " + formStage + "，件此刻数出来在第 "
                    + cur + " 格），系统不认，件仍停在原先那一格");
        }

        // 走满末一格，这一件就此闭上
        if (cur >= MAX_STAGE) {
            row.setStatus(STATUS_CLOSED);
            row.setStage(MAX_STAGE);
            row.setLastAction("结案");
            this.giprCaseFlowMapper.updateById(row);
            append(row.getBizNo(), MAX_STAGE, GiprCaseTraceSupport.currentAttempt(events, MAX_STAGE),
                    TGiprCaseTrace.ACTION_CLOSE, remark);
            GiprFlowResult r = GiprFlowResult.ok("走满末一格，这一件就此闭上", row);
            r.setStage(MAX_STAGE);
            return r;
        }

        int target = cur + 1;
        if (target == STAGE_CITY_REVIEW) {
            // 进初审：先瞅这家企业在授权册上还算不算挂着名
            TGiprHolderBook holder = findHolder(row.getHolderNo());
            if (holder == null) {
                return GiprFlowResult.fail("授权册上已查无此企业代号：" + row.getHolderNo() + "，初审这一格进不去");
            }
            TGiprProductBook prod = findProduct(row.getProdNo());
            if (prod == null || (prod.getStatus() != null && prod.getStatus() == PROD_REMOVED)) {
                return GiprFlowResult.fail("挂靠的名录产品已移出或并走：" + row.getProdNo() + "，初审这一格进不去");
            }
        } else if (target == STAGE_PROV_APPROVE) {
            // 进核准：先瞅初审那一句有没有记成「可」
            int attempt = GiprCaseTraceSupport.currentAttempt(events, STAGE_CITY_REVIEW);
            TGiprCaseTrace note = GiprCaseTraceSupport.firstAnnotation(events, STAGE_CITY_REVIEW, attempt);
            if (note == null || !PASS_WORD.equals(note.getContent() == null ? null : note.getContent().trim())) {
                return GiprFlowResult.fail("初审那一句还没记成「可」，核准这道门不开");
            }
        } else if (target == MAX_STAGE) {
            // 进发标：两名核签岗都得各落一笔，且标志核发记号已写回本行
            int attempt = GiprCaseTraceSupport.currentAttempt(events, STAGE_PROV_APPROVE);
            Set<String> signers = GiprCaseTraceSupport.signers(events, STAGE_PROV_APPROVE, attempt);
            if (signers.size() < NEED_SIGNERS
                    || row.getCertFlag() == null || row.getCertFlag() != CERT_ISSUED) {
                return GiprFlowResult.fail("两名核签岗未各落一笔、或标志核发记号未写回本行，发标这一格进不去");
            }
        }

        row.setStage(target);
        row.setStatus(STATUS_ACTIVE);
        row.setLastAction(remark);
        if (!isBlank(remark)) {
            row.setContent(remark);
        }
        this.giprCaseFlowMapper.updateById(row);
        append(row.getBizNo(), target, GiprCaseTraceSupport.currentAttempt(events, target) + 1,
                TGiprCaseTrace.ACTION_ADVANCE, remark);

        GiprFlowResult r = GiprFlowResult.ok("已往前挪一格，现站在第 " + target + " 格", row);
        r.setStage(target);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GiprFlowResult rollback(Long id, String remark) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        if (isClosed(row)) {
            return GiprFlowResult.fail("这一件已闭上（或已了结），动不得了");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());
        if (cur <= STAGE_INTAKE) {
            return GiprFlowResult.fail("本来就站在头一格（收件），再退没处可退");
        }

        // 退掉那一格原先留的那句按语，原样挪进追溯串末尾
        int attempt = GiprCaseTraceSupport.currentAttempt(events, cur);
        TGiprCaseTrace note = GiprCaseTraceSupport.firstAnnotation(events, cur, attempt);
        String moved = note == null ? null : note.getContent();

        int back = cur - 1;
        row.setStage(back);
        row.setStatus(STATUS_ACTIVE);
        row.setLastAction(isBlank(remark) ? "回退" : remark);
        this.giprCaseFlowMapper.updateById(row);
        // 再走回那一格时按新起的一次重新累计
        append(row.getBizNo(), back, GiprCaseTraceSupport.currentAttempt(events, back) + 1,
                TGiprCaseTrace.ACTION_ROLLBACK, moved);

        GiprFlowResult r = GiprFlowResult.ok("已往回收一格，现站在第 " + back + " 格", row);
        r.setStage(back);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GiprFlowResult annotate(Long id, String content) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        if (isClosed(row)) {
            return GiprFlowResult.fail("这一件已闭上（或已了结），补不了按语");
        }
        if (isBlank(content)) {
            return GiprFlowResult.fail("按语是空的，没东西可落");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());
        int attempt = GiprCaseTraceSupport.currentAttempt(events, cur);

        // 同一格第二次往按语栏写字：只认最早那一句，后写的顶不掉，也不另存一份
        TGiprCaseTrace first = GiprCaseTraceSupport.firstAnnotation(events, cur, attempt);
        if (first != null) {
            GiprFlowResult r = GiprFlowResult.ok("这一格早有按语，只认最早那一句，后写的不另存", row);
            r.setStage(cur);
            return r;
        }

        append(row.getBizNo(), cur, attempt, TGiprCaseTrace.ACTION_ANNOTATE, content);
        row.setContent(content);
        this.giprCaseFlowMapper.updateById(row);

        GiprFlowResult r = GiprFlowResult.ok("按语已落在第 " + cur + " 格", row);
        r.setStage(cur);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GiprFlowResult sign(Long id, String signer) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        if (isClosed(row)) {
            return GiprFlowResult.fail("这一件已闭上（或已了结），动不得了");
        }
        if (isBlank(signer)) {
            return GiprFlowResult.fail("核签岗名号是空的，落不了笔");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());
        if (cur != STAGE_PROV_APPROVE) {
            return GiprFlowResult.fail("核签只在核准那一格收，件此刻数出来在第 " + cur + " 格");
        }
        int attempt = GiprCaseTraceSupport.currentAttempt(events, cur);
        Set<String> signers = GiprCaseTraceSupport.signers(events, cur, attempt);
        String name = signer.trim();
        if (signers.contains(name)) {
            GiprFlowResult r = GiprFlowResult.ok("该岗已落过一笔，幂等不重复记", row);
            r.setStage(cur);
            return r;
        }

        append(row.getBizNo(), cur, attempt, TGiprCaseTrace.ACTION_SIGN, name);
        signers.add(name);
        // 两名核签岗各落一笔才算签齐，签齐同时把标志核发记号写回本行
        if (signers.size() >= NEED_SIGNERS) {
            row.setCertFlag(CERT_ISSUED);
            this.giprCaseFlowMapper.updateById(row);
        }

        GiprFlowResult r = GiprFlowResult.ok("核签岗已落笔（本格已到 " + signers.size() + " 岗）", row);
        r.setStage(cur);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GiprFlowResult terminate(Long id, Integer endKind) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        if (isClosed(row)) {
            return GiprFlowResult.fail("这一件早已闭上（或已了结）");
        }
        if (endKind == null || (endKind != END_WITHDRAW && endKind != END_MERGED)) {
            return GiprFlowResult.fail("了结方式只认两样：1 递件方撤回、2 名录产品整段并走");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());

        row.setStatus(STATUS_CLOSED);
        row.setEndKind(endKind);
        row.setStage(cur);
        row.setLastAction(endKind == END_WITHDRAW ? "递件方撤回" : "名录产品并走");
        this.giprCaseFlowMapper.updateById(row);
        append(row.getBizNo(), cur, GiprCaseTraceSupport.currentAttempt(events, cur),
                TGiprCaseTrace.ACTION_TERMINATE, row.getLastAction());

        GiprFlowResult r = GiprFlowResult.ok("这一件就此了结，不必再往前", row);
        r.setStage(cur);
        return r;
    }

    /**
     * 倒着回放一遍。只出结果、不回写——当年图省事整件重演落库的走样，不许再有。
     */
    @Override
    @Transactional(readOnly = true)
    public GiprFlowResult replay(Long id) {
        TGiprCaseFlow row = this.giprCaseFlowMapper.selectById(id);
        if (row == null) {
            return GiprFlowResult.fail("查无此件");
        }
        List<TGiprCaseTrace> events = eventsOf(row.getBizNo());
        // 某一格那一句不全的，说清断在哪一格、缺哪一栏
        for (TGiprCaseTrace e : events) {
            String at = e.getStage() == null ? "未知格（主键 " + e.getId() + "）" : "第 " + e.getStage() + " 格";
            if (e.getStage() == null) {
                return GiprFlowResult.fail("回放断在" + at + "，缺 stage（哪一格）一栏");
            }
            if (e.getHappenedAt() == null) {
                return GiprFlowResult.fail("回放断在" + at + "，缺 happened_at（落在哪一刻）一栏");
            }
            if ((TGiprCaseTrace.ACTION_ANNOTATE.equals(e.getAction())
                    || TGiprCaseTrace.ACTION_ADVANCE.equals(e.getAction())
                    || TGiprCaseTrace.ACTION_ROLLBACK.equals(e.getAction())
                    || TGiprCaseTrace.ACTION_SIGN.equals(e.getAction()))
                    && isBlank(e.getContent())) {
                return GiprFlowResult.fail("回放断在" + at + "，缺 content（那句按语）一栏");
            }
        }
        int cur = GiprCaseTraceSupport.deriveStage(events, row.getStage());
        List<TGiprCaseTrace> backwards = new ArrayList<TGiprCaseTrace>(events);
        Collections.reverse(backwards);

        GiprFlowResult r = GiprFlowResult.ok("回放就绪：自末一格往回共 " + backwards.size() + " 句，只出不回写", row);
        r.setStage(cur);
        r.setTrace(backwards);
        return r;
    }

    /** 三道闩之二：已闭上的件，一处内容都改不得 */
    @Override
    public boolean updateContent(Long id, String remark) {
        TGiprCaseFlow r = this.giprCaseFlowMapper.selectById(id);
        if (r == null || isClosed(r)) {
            return false;
        }
        r.setContent(remark);
        return this.giprCaseFlowMapper.updateById(r) > 0;
    }

    /** 三道闩之三：已闭上的件，整条抽不走 */
    @Override
    public boolean remove(Long id) {
        TGiprCaseFlow r = this.giprCaseFlowMapper.selectById(id);
        if (r == null || isClosed(r)) {
            return false;
        }
        return this.giprCaseFlowMapper.deleteById(id) > 0;
    }

    /** 闭上（走满末一格）或了结（撤回／并走）都算封住，三道闩一齐落下 */
    private boolean isClosed(TGiprCaseFlow row) {
        return row.getStatus() != null && row.getStatus() == STATUS_CLOSED;
    }

    /** 取这一件身后的追溯串，按落话先后排好（同一刻的按主键分先后，不并不丢） */
    private List<TGiprCaseTrace> eventsOf(String bizNo) {
        if (isBlank(bizNo)) {
            return new ArrayList<TGiprCaseTrace>();
        }
        QueryWrapper<TGiprCaseTrace> qw = new QueryWrapper<TGiprCaseTrace>();
        qw.eq("biz_no", bizNo).eq("del_flag", 0).orderByAsc("happened_at").orderByAsc("id");
        return this.giprCaseTraceMapper.selectList(qw);
    }

    /** 一句按语落下去，紧跟着记下它落在哪一刻，串到件身后 */
    private void append(String bizNo, int stage, int attempt, String action, String content) {
        if (isBlank(bizNo)) {
            return;
        }
        TGiprCaseTrace e = new TGiprCaseTrace();
        e.setBizNo(bizNo);
        e.setStage(stage);
        e.setAttempt(attempt);
        e.setAction(action);
        e.setContent(content);
        e.setHappenedAt(new Date());
        this.giprCaseTraceMapper.insert(e);
    }

    private TGiprCaseFlow findByBizNo(String bizNo) {
        QueryWrapper<TGiprCaseFlow> qw = new QueryWrapper<TGiprCaseFlow>();
        qw.eq("biz_no", bizNo).eq("del_flag", 0);
        return this.giprCaseFlowMapper.selectOne(qw);
    }

    private TGiprCaseFlow findOpenCaseOfHolder(String holderNo) {
        QueryWrapper<TGiprCaseFlow> qw = new QueryWrapper<TGiprCaseFlow>();
        qw.eq("holder_no", holderNo).eq("del_flag", 0).ne("status", STATUS_CLOSED);
        qw.orderByDesc("id").last("limit 1");
        return this.giprCaseFlowMapper.selectOne(qw);
    }

    private TGiprHolderBook findHolder(String holderNo) {
        if (isBlank(holderNo)) {
            return null;
        }
        QueryWrapper<TGiprHolderBook> qw = new QueryWrapper<TGiprHolderBook>();
        qw.eq("holder_no", holderNo).eq("del_flag", 0);
        return this.giprHolderBookMapper.selectOne(qw);
    }

    private TGiprProductBook findProduct(String prodNo) {
        if (isBlank(prodNo)) {
            return null;
        }
        QueryWrapper<TGiprProductBook> qw = new QueryWrapper<TGiprProductBook>();
        qw.eq("prod_no", prodNo).eq("del_flag", 0);
        return this.giprProductBookMapper.selectOne(qw);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
