package aes.server.drools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.rule.FactHandle;
import org.drools.base.RuleNameMatchesAgendaFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.common.DefaultFactHandle;


class Session {


    ArrayList<Object> objects;

    ArrayList<BusinessRule> rules = null;

    BusinessProcess proc = null;

    String[] callbacks;
  
    StatefulKnowledgeSession ksession;

    ArrayList<FactHandle> fhandles;

	Logger logger = Logger.getLogger(DroolsServlet.class);

	
    public Session() {

            objects = new ArrayList<Object>();

            fhandles = new ArrayList<FactHandle>();

    }


    public Session(ArrayList<BusinessRule> incRules) {

           this();

           rules = incRules;

           ksession = KnowledgeAgent.getKnowledgePkg(rules.get(0).getPkgName()).getKnowledgeBase().newStatefulKnowledgeSession();

    }

    public void fire() {
    
         if (rules.size() == 0) {

             logger.info("Warning : Drools Knowledge Session attempted to fire without a rule defined.");
        
         }

         KnowledgeRuntimeLogger dlogger = KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
         
         for (BusinessRule brule : rules) {
             int u = ksession.fireAllRules( new RuleNameMatchesAgendaFilter(brule.getName()) );

             logger.info("dbg: " + brule.getName());
             logger.info("dbg: how many rules fired: " + u);
         }
         
         ksession.dispose();
    }


    public void fire(String rulename, Object target) {

        rules.add(KnowledgeAgent.getRule(rulename));
 
        fhandles.add(ksession.insert(target));

        fire();
    }

    public void fire(Object target) {

        setObject(target);

        fire();
    }

    public void fire(ArrayList<Object> targets) {

        setObjects(targets);

        fire();
    }


    public void setObject(Object target) {

        
        fhandles.add(ksession.insert(target));
        
    }

    public void setObjects(ArrayList<Object> targets) {


           for (Object t : targets)
               fhandles.add(ksession.insert(t));

    }


    public void setCallBacks(String[] icallbacks) {

         callbacks = icallbacks;

    }
            

    public ArrayList<Object> getResults() {

           ArrayList results = new ArrayList();

           for (FactHandle f : fhandles) {
               results.add(ksession.getObject(f));
           }


           return results;
    	   
    }


    public HashMap<String,Object> getResultStrings(String[] getters) {


           HashMap<String, Object> rjson = new HashMap<String, Object>();

           rjson.put("rulename", rules.get(0).getName());

           ArrayList<Object> rlist = new ArrayList<Object>();

           for (Object robj : getResults()) {

                  ArrayList<String> entry = new ArrayList<String>();

                  for (String g : getters) {

                	 try {
                        Method getter = robj.getClass().getMethod(g, null);
       	                entry.add(getter.invoke(robj, null).toString());
         
                        
                	 } catch (Exception e) { e.printStackTrace(); }

                   }
                   rlist.add(entry);

           }
         
           rjson.put("results", rlist);

           return rjson;

     }


     public HashMap<String,Object> getResultStrings() {

            if (callbacks != null)

                 return getResultStrings(callbacks);

            return null;
            		
     }
     
     public Object[] getClientResults() {


           return null;


     }

        	 
    }

