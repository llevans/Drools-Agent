package aes.server.drools;

import java.util.Date;

class BusinessRule extends Asset {

    String ruleText = "";

    public BusinessRule(String n, String u, String t, Date p, String k) {
            super(n, u, t, p, k);
    }

    public String getRuleText() { return ruleText; }

    void setRuleText(String text) { ruleText = text; }

}
