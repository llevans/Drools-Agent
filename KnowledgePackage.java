package aes.server.drools;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;

public class KnowledgePackage extends Asset {


   KnowledgeBase kbase = null;

   HashMap<String, ? super Asset> Assets = null;

   String assetsXML = ""; 

   Logger logger = Logger.getLogger(KnowledgePackage.class);

   public KnowledgePackage() {

         Assets = new HashMap<String, Asset>();
   }

   
   public KnowledgeBase getKnowledgeBase() { return kbase; }

   void setKnowledgeBase(KnowledgeBase knowledgeBase) { kbase = knowledgeBase; }

   public String getAssetsXML() { return assetsXML; }

   void setAssetsXML(String xml) { assetsXML = xml; }

   public List<BusinessRule> getRules() {

          List<BusinessRule> tlist = new ArrayList<BusinessRule>();


          for (String k : Assets.keySet()) {
 

              if (((Asset)Assets.get(k)).getType().equals("drl")) {
                   BusinessRule a = (BusinessRule) Assets.get(k);
                   if (!tlist.contains(a)) {
                      tlist.add(a);
          logger.info("DBG2: " + a.getName() + " " + a.getUuid());
              }
              }

          }

          return tlist;

   }

   public List<BusinessProcess> getProcesses() {

          List<BusinessProcess> tlist = new ArrayList<BusinessProcess>();

          for (String k : Assets.keySet()) {
 
        	  
              if (((Asset)Assets.get(k)).getType().equals("bpmn2")) {

        	   BusinessProcess a = (BusinessProcess) Assets.get(k);
                   if (!tlist.contains(a)) {
                       tlist.add(a);
          logger.info("DBG2: " + a.getName() + " " + a.getUuid());
                   }
              }

          }

          return tlist;

   }

}


