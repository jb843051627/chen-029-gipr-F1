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
 * 专用标志使用核准办件按语追溯串对象 t_gipr_case_trace
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_gipr_case_trace")
@ApiModel(value = "TGiprCaseTrace", description = "专用标志使用核准办件按语追溯串")
public class TGiprCaseTrace implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落话动作：起件 */
    public static final String ACTION_CREATE = "CREATE";
    /** 落话动作：按语 */
    public static final String ACTION_ANNOTATE = "ANNOTATE";
    /** 落话动作：往前挪一格 */
    public static final String ACTION_ADVANCE = "ADVANCE";
    /** 落话动作：往回收一格 */
    public static final String ACTION_ROLLBACK = "ROLLBACK";
    /** 落话动作：核签岗落一笔 */
    public static final String ACTION_SIGN = "SIGN";
    /** 落话动作：走满末一格闭上 */
    public static final String ACTION_CLOSE = "CLOSE";
    /** 落话动作：就此了结（撤回／名录并走） */
    public static final String ACTION_TERMINATE = "TERMINATE";

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 挂在哪一件身后（件次） */
    @TableField("biz_no")
    @ApiModelProperty(value = "挂在哪一件身后（件次）")
    private String bizNo;

    /** 落话时件站在哪一格 0..3 */
    @TableField("stage")
    @ApiModelProperty(value = "落话时件站在哪一格 0..3")
    private Integer stage;

    /** 这一格第几次进入（退回去再走回按新起的一次重新累计） */
    @TableField("attempt")
    @ApiModelProperty(value = "这一格第几次进入")
    private Integer attempt;

    /** 落话动作 CREATE/ANNOTATE/ADVANCE/ROLLBACK/SIGN/CLOSE/TERMINATE */
    @TableField("action")
    @ApiModelProperty(value = "落话动作")
    private String action;

    /** 那一格留下的按语（回退时原样挪到串末尾；核签岗记签名） */
    @TableField("content")
    @ApiModelProperty(value = "那一格留下的按语")
    private String content;

    /** 这句按语落在哪一刻 */
    @TableField("happened_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "这句按语落在哪一刻")
    private Date happenedAt;

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

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(Date happenedAt) {
        this.happenedAt = happenedAt;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
