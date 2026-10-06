package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TGiprCaseFlow;
import com.fc.v2.model.custom.GiprFlowResult;

/**
 * 专用标志使用核准办件流转单 Service接口（state-machine 形状：单据流转，不提供增删改查全套）
 *
 * <p>四格先后钉死：0收件 → 1初审 → 2核准 → 3备案发标，中间没有近路。
 * 件站在哪一格由后端顺着追溯串数出来，报面上自带的序号只供人眼对着看。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITGiprCaseFlowService {

    /** 按主键查询单据 */
    TGiprCaseFlow selectTGiprCaseFlowById(Long id);

    /** 列表查询（流转台账；已闭上的件照旧在名册里数得着） */
    List<TGiprCaseFlow> selectTGiprCaseFlowList(QueryWrapper<TGiprCaseFlow> queryWrapper);

    /**
     * 起一条件。四样少一样就起不来，回话点名少了哪一样；
     * 同一企业手上没闭上的件至多一条；同一件次并着进两回幂等，只立一条。
     *
     * @param bizNo      件次（幂等口子）
     * @param holderNo   哪一家企业（授权册上的代号）
     * @param prodNo     挂哪一个名录产品
     * @param goodsKind  拟贴在哪一类货上
     * @param submitTime 递件那一刻
     */
    GiprFlowResult start(String bizNo, String holderNo, String prodNo, String goodsKind, Date submitTime);

    /**
     * 往前挪一格。一次只挪一格；报面序号比数出来的多冲两格以上的，这一笔不认，
     * 件仍停在原先那一格。进初审先瞅企业还在不在册；进核准先瞅初审那句记没记成「可」；
     * 进发标先瞅两名核签岗签没签齐、核发记号写没写回本行。走满末一格就此闭上。
     *
     * @param id        件主键
     * @param formStage 报面上自带的序号（只供对着看；跨两格的递件凭它认出来）
     * @param remark    这一格留下的按语
     */
    GiprFlowResult advance(Long id, Integer formStage, String remark);

    /**
     * 往回收一格。一次只退一格；本来就站在头一格的没处可退。
     * 退掉那一格原先留的那句按语原样挪进追溯串末尾；再走回那一格按新起的一次重新累计。
     */
    GiprFlowResult rollback(Long id, String remark);

    /**
     * 往当前这一格的按语栏落一句。同一格第二次落字，只认最早那一句，
     * 后写的顶不掉它，也不会另存一份。已闭上的件补不了。
     */
    GiprFlowResult annotate(Long id, String content);

    /**
     * 核签岗落一笔（只在核准那一格收）。两名核签岗各落一笔才算签齐，
     * 签齐同时把标志核发记号写回本行。同一岗重复落笔幂等。
     */
    GiprFlowResult sign(Long id, String signer);

    /**
     * 就此了结、不必再往前：1 递件方自己反悔把话取了回；2 挂靠的名录产品整段并走。
     */
    GiprFlowResult terminate(Long id, Integer endKind);

    /**
     * 倒着回放一遍：从末一格往回一格一格演，只出结果、不回写。
     * 某一格那一句不全的，回话说清断在哪一格、缺哪一栏。
     */
    GiprFlowResult replay(Long id);

    /** 修改内容：已闭上的件不得再改，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 删除单据：已闭上的件不得抽走，被拒返回 false */
    boolean remove(Long id);
}
