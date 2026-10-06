package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 专用标志使用核准办件流转单对象 t_gipr_case_flow
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_gipr_case_flow")
@ApiModel(value = "TGiprCaseFlow", description = "专用标志使用核准办件流转单")
public class TGiprCaseFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 专用标志使用核准办件的件次 */
    @TableField("biz_no")
    @ApiModelProperty(value = "专用标志使用核准办件的件次")
    private String bizNo;

    /** 当前关次 0..3（申请受理/材料初审/标志核准/备案发标） */
    @TableField("stage")
    @ApiModelProperty(value = "当前关次 0..3（申请受理/材料初审/标志核准/备案发标）")
    private Integer stage;

    /** 单子走到哪一关 0未起 1在办 2已封住 */
    @TableField("status")
    @ApiModelProperty(value = "单子走到哪一关 0未起 1在办 2已封住")
    private Integer status;

    /** 一关一记 */
    @TableField("content")
    @ApiModelProperty(value = "一关一记")
    private String content;

    /** 最近一次挪关动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次挪关动作")
    private String lastAction;

    /** 哪一家企业（授权册上的代号） */
    @TableField("holder_no")
    @ApiModelProperty(value = "哪一家企业（授权册上的代号）")
    private String holderNo;

    /** 挂哪一个名录产品 */
    @TableField("prod_no")
    @ApiModelProperty(value = "挂哪一个名录产品")
    private String prodNo;

    /** 拟贴在哪一类货上 */
    @TableField("goods_kind")
    @ApiModelProperty(value = "拟贴在哪一类货上")
    private String goodsKind;

    /** 递件那一刻 */
    @TableField("submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "递件那一刻")
    private Date submitTime;

    /** 标志核发记号 0未核发 1已核发（两名核签岗签齐后写回本行） */
    @TableField("cert_flag")
    @ApiModelProperty(value = "标志核发记号 0未核发 1已核发")
    private Integer certFlag;

    /** 了结方式 0未了结 1递件方撤回 2挂靠名录产品整段并走 */
    @TableField("end_kind")
    @ApiModelProperty(value = "了结方式 0未了结 1递件方撤回 2名录产品整段并走")
    private Integer endKind;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public String getHolderNo() {
        return holderNo;
    }

    public void setHolderNo(String holderNo) {
        this.holderNo = holderNo;
    }

    public String getProdNo() {
        return prodNo;
    }

    public void setProdNo(String prodNo) {
        this.prodNo = prodNo;
    }

    public String getGoodsKind() {
        return goodsKind;
    }

    public void setGoodsKind(String goodsKind) {
        this.goodsKind = goodsKind;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Integer getCertFlag() {
        return certFlag;
    }

    public void setCertFlag(Integer certFlag) {
        this.certFlag = certFlag;
    }

    public Integer getEndKind() {
        return endKind;
    }

    public void setEndKind(Integer endKind) {
        this.endKind = endKind;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
