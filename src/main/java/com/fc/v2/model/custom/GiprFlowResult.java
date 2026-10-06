package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TGiprCaseFlow;
import com.fc.v2.model.auto.TGiprCaseTrace;

import java.io.Serializable;
import java.util.List;

/**
 * 办件流转的回话。
 *
 * <p>被拒的回话必须点名缘故：起件缺项点名缺哪一样；回放断带点名断在哪一格、
 * 缺哪一栏；光秃秃一句「材料不齐」不算回答。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
public class GiprFlowResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 这一笔成没成 */
    private boolean ok;

    /** 回话（成了说落点，被拒说清缘故） */
    private String msg;

    /** 回话这一刻件站在哪一格（由追溯串数出来的那个数） */
    private Integer stage;

    /** 件本身 */
    private TGiprCaseFlow data;

    /** 回放时倒着排的那一串（末一格在前） */
    private List<TGiprCaseTrace> trace;

    public static GiprFlowResult ok(String msg, TGiprCaseFlow data) {
        GiprFlowResult r = new GiprFlowResult();
        r.setOk(true);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    public static GiprFlowResult fail(String msg) {
        GiprFlowResult r = new GiprFlowResult();
        r.setOk(false);
        r.setMsg(msg);
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public TGiprCaseFlow getData() {
        return data;
    }

    public void setData(TGiprCaseFlow data) {
        this.data = data;
    }

    public List<TGiprCaseTrace> getTrace() {
        return trace;
    }

    public void setTrace(List<TGiprCaseTrace> trace) {
        this.trace = trace;
    }
}
