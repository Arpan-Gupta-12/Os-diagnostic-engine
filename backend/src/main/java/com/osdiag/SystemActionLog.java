package com.osdiag;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** A change this app really made to the PC (closing a program, switching the power plan) and how it turned out. */
@Entity
@Table(name = "system_action_logs", indexes = @Index(name = "idx_system_action_logs_execution_time", columnList = "executionTime"))
public class SystemActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 30)
    private String actionType;
    @Column(length = 255)
    private String target;
    @Column(length = 255)
    private String previousValue;
    @Column(length = 255)
    private String newValue;
    private Instant executionTime;
    private boolean success;
    @Column(length = 500)
    private String resultMessage;

    public Long getId() { return id; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public Instant getExecutionTime() { return executionTime; }
    public void setExecutionTime(Instant executionTime) { this.executionTime = executionTime; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
}
