package com.debankar.featureflags.domain;

public class TargetingRule {
    private String attribute;
    private TargetingOperator operator;
    private String value;

    public TargetingRule() {}

    public TargetingRule(String attribute, TargetingOperator operator, String value) {
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
    }

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public TargetingOperator getOperator() { return operator; }
    public void setOperator(TargetingOperator operator) {
        this.operator = operator;
    }

    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TargetingRule{" +
                "attribute='" + attribute + '\'' +
                ", operator=" + operator +
                ", value='" + value + '\'' +
                '}';
    }
}
