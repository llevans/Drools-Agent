package aes.server.drools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.rule.FactHandle;
import org.drools.base.RuleNameMatchesAgendaFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.common.DefaultFactHandle;


class RuleSession extends Session {


    public RuleSession() {

            super();

    }


    public RuleSession(ArrayList<BusinessRule> incRules) {

           super(incRules);
    }

    public void setFact(Object target) {

        setObject(target);

    }

    public void setFacts(ArrayList<Object> targets) {

        setObjects(targets);

    }


}

