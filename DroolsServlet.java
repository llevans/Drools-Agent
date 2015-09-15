
package aes.server.drools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.base.RuleNameMatchesAgendaFilter;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.common.DefaultFactHandle;
import org.drools.definition.KnowledgePackage;
import org.drools.definitions.rule.impl.RuleImpl;
import org.drools.definition.process.Process;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import aes.server.Format;
import aes.server.models.DemoDate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class DroolsServlet extends HttpServlet {

	private static final long serialVersionUID = 200L;
	Logger logger = Logger.getLogger(DroolsServlet.class);

        KnowledgeAgent kagent;

        class RuleResult {
         
                  public String name;
                  public String uuid;
                  public String result;
                  public String firedAt;
        }

        HashMap<String, String> masterList = new HashMap<String, String>();
        
        String droolsServerURL;

       /************************************
        * Initiate Servlet
        ************************************/
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

                if (KnowledgeAgent.getDroolsServerURL() == null) {

                        droolsServerURL = getServletContext().getInitParameter("guvnor-server");
                        KnowledgeAgent.setDroolsServerURL(droolsServerURL);

                } else {
        
                        droolsServerURL = KnowledgeAgent.getDroolsServerURL();
                }

	}


        /********************************** 
         * Process GET request
         **********************************/

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

                PrintWriter out = resp.getWriter();

		// Parse URL
		List<String> parts = new ArrayList<String>(Arrays.asList(req.getRequestURI().split("/")));

		while (!parts.isEmpty() && !parts.get(0).equalsIgnoreCase("rules")) {
			parts.remove(0);
		}
		parts.remove(0); 
		
		if (!parts.isEmpty()) {
			String token = parts.remove(0).toLowerCase();

			resp.setContentType(Format.JSON.getEncoding());
			Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
			
			if (token.equals("ruleslist")) {
                         String pkg = parts.remove(0);
                         System.out.println("DBG1: for ruleslist " + pkg);
                if (pkg.toUpperCase().equals("MASS"))
                {

                	List<BusinessRule> rlist = KnowledgeAgent.getKnowledgePkg(pkg.toUpperCase()).getRules();
                             
			out.print(gson.toJson(rlist));

                } else { 
                   List<BusinessRule> rlist = getDemoList();

	           out.print(gson.toJson(rlist));

                }
			} else if (token.equals("proclist")) { logger.info("DBG : calling proclist");
                                List<BusinessProcess> rlist = KnowledgeAgent.getKnowledgePkg("MASS").getProcesses();

				out.print(gson.toJson(rlist));

			} else if (token.equals("droolsserver")) {
				out.print(droolsServerURL);
				
			} else if (token.equals("firerule")) {
				out.print(gson.toJson(getRuleResult(parts.remove(0))));
			}
		} else {
                       printmethods(out, resp);			
		}
	} 
	

        /**************************************
         * Process POST request
         **************************************/

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp);
	}
	
 
       /**************************************************************
        * Apply rule to DemoDate object using the Knowledge Session
        **************************************************************/

       private Object getRuleResult(String rulename) {
    	   
               rulename = rulename.replaceAll("%20", " " );

               RuleSession rule =  KnowledgeAgent.getRuleSession(rulename);
                           
               rule.setFact(new DemoDate());

               rule.fire();
                           
             
               if (rulename.equals("Greenwich Mean Time Offset")) 
            	   rule.setCallBacks(new String[] {"getGmtOffset"});
            	   
                else if (rulename.equals("In 2nd Quarter")) 
            	   rule.setCallBacks(new String[] {"getYearlyQuarter"} );
            
                else if (rulename.equals("Is Julian Day greater than X"))  {
            	   //Boolean rbool =  new Boolean(((DemoDate)(rule.getResults().get(0))).getJulianFlag());
            	   rule.setCallBacks(new String[] {"getJulianFlag"});
                } 
   			Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

                logger.info("DBG: " + gson.toJson(rule.getResultStrings()));

               /*Calendar timestamp = GregorianCalendar.getInstance();
               Date now = new Date();
               timestamp.setTime(now);
    
               RuleResult rr = new RuleResult();
               rr.name = rulename;
               rr.firedAt = ( new Integer(timestamp.get(Calendar.HOUR))).toString() + ":";
               rr.firedAt += ( new Integer(timestamp.get(Calendar.MINUTE))).toString() + ":";
               rr.firedAt += ( new Integer(timestamp.get(Calendar.SECOND))).toString() + ":";
               rr.firedAt += ( new Integer(timestamp.get(Calendar.MILLISECOND))).toString();
               
               if (rulename.equals("Greenwich Mean Time Offset")) 
            	   rr.result = new Integer(((DemoDate)(((DefaultFactHandle)fhandle).getObject())).getGmtOffset()).toString();
            	   
                else if (rulename.equals("In 2nd Quarter")) 
            	   rr.result = new Integer(((DemoDate)(((DefaultFactHandle)fhandle).getObject())).getYearlyQuarter()).toString();
            
                else if (rulename.equals("Is Julian Day greater than X"))  {
            	   Boolean rbool =  new Boolean(((DemoDate)(((DefaultFactHandle)fhandle).getObject())).getJulianFlag());
            	   logger.debug("dbg fhandle: " + rbool);
            	   rr.result = rbool.toString();
                }

               logger.debug("dbg fhandle: " + rr.result);
            
               ksession.dispose(); */
               
               return rule.getResultStrings();


       }
       
       
       /*******************************
        * Get Drools Asset List
        *******************************/

     /*  private List<Asset> getAssetList(String token, String pkgResource) { 
           

           if (token.equals("ruleslist")) {
        	    List<BusinessRule> thelist = new ArrayList<BusinessRule>();
        	    thelist = KnowledgeAgent.getKnowledgePkg(pkgResource).getRules();
        	    return thelist;

           } else if (token.equals("proclist")) {
        	    List<BusinessProcess> thelist = new ArrayList<BusinessProcess>();
        	    thelist = KnowledgeAgent.getKnowledgePkg(pkgResource).getProcesses();
        	    return thelist;
        	    //thelist = getBPMN2Assets();
           }
           
           
       }  */

       
       /*************************************
        * Get BPMN2 processes from REST
        *************************************/
       private List<RuleResult> getBPMN2Assets() {


    	   String assetURL = droolsServerURL + "/rest/packages/MASS/assets";
    	   String line = "";
    	   StringBuilder restText = new StringBuilder();
           
           List<RuleResult> bpmn2list = new ArrayList<RuleResult>();

    	   try {
        
    		   //HttpResponse proxyResponse = KnowledgeAgent.getResponse(assetURL);
    		   restText = new StringBuilder(KnowledgeAgent.getResponse(assetURL));

                   /*logger.info("DBG: return proxy");
    		   // Wrap a BufferedReader around the InputStream
    		   BufferedReader rd = new BufferedReader(new InputStreamReader(proxyResponse.getEntity().getContent()));

    		    //Read response until the end
    		   while ((line = rd.readLine()) != null) { 
    		   restText.append(line); 
    		   } */ 
    		       XMLStreamReader xmlStreamReader =
    	                StAXUtils.createXMLStreamReader(new StringReader(restText.toString()));
    	           StAXOMBuilder b = new StAXOMBuilder(xmlStreamReader);
    	           
    		       OMElement root = b.getDocumentElement();

    		       for (Iterator j = root.getChildrenWithName(new QName("entry")); j.hasNext();) {
                	   OMElement entry = (OMElement) j.next();
                	   
                	   String name = "";
                	   String uuid = "";
                	   
        		       for (Iterator k = entry.getChildrenWithName(new QName("id")); k.hasNext();) {

                  	   OMElement asset = (OMElement) k.next();
                   	   name = asset.getText().substring(asset.getText().lastIndexOf("/")+1).replaceAll("%20", " ");

                       
                   	   OMElement metadata = entry.getFirstChildWithName(new QName("metadata"));
                   	   OMElement format = metadata.getFirstChildWithName(new QName("format"));
                   	   OMElement archived = metadata.getFirstChildWithName(new QName("archived"));

                   	if (!archived.getFirstChildWithName(new QName("value")).getText().equals("true")) {
                   	   if (format.getFirstChildWithName(new QName("value")).getText().equals("bpmn2")) {
                        	   RuleResult rr = new RuleResult();
                        	   rr.name = name;
                        	   OMElement tuuid = metadata.getFirstChildWithName(new QName("uuid"));
                               rr.uuid = tuuid.getFirstChildWithName(new QName("value")).getText();
                               
                               bpmn2list.add(rr);
                        		   
                       }
                   	}   
                   } 
    		       }
           } catch (Exception e) { e.printStackTrace(); }


    	   return bpmn2list;
    	   


      } 

       /*************************************
        * Get Rules from REST
        *************************************/
       private List<RuleResult> getRuleAssets(String pkgResource) {
           logger.info("dbg: incoming okgResource " + pkgResource);

    	   String assetURL = droolsServerURL + "/rest/packages/" + pkgResource + "/assets";
    	   String line = "";
    	   StringBuilder restText = new StringBuilder();
           
           List<RuleResult> rulelist = new ArrayList<RuleResult>();

    	   try {
    		   //HttpResponse proxyResponse = KnowledgeAgent.getResponse(assetURL);
    		   restText = new StringBuilder(KnowledgeAgent.getResponse(assetURL));
                   /*logger.info("DBG: return proxy");
    		   // Wrap a BufferedReader around the InputStream
    		   BufferedReader rd = new BufferedReader(new InputStreamReader(proxyResponse.getEntity().getContent()));

    		    //Read response until the end
    		   while ((line = rd.readLine()) != null) { 
    		   restText.append(line); 
    		   } */ 
    		       XMLStreamReader xmlStreamReader =
    	                StAXUtils.createXMLStreamReader(new StringReader(restText.toString()));
    	           StAXOMBuilder b = new StAXOMBuilder(xmlStreamReader);
    	           
    		       OMElement root = b.getDocumentElement();

    		       for (Iterator j = root.getChildrenWithName(new QName("entry")); j.hasNext();) {
                	   OMElement entry = (OMElement) j.next();
                	   
                	   String name = "";
                	   String uuid = "";
                	   
        		       for (Iterator k = entry.getChildrenWithName(new QName("id")); k.hasNext();) {

                  	   OMElement asset = (OMElement) k.next();
                   	   name = asset.getText().substring(asset.getText().lastIndexOf("/")+1).replaceAll("%20", " ");

                       
                   	   OMElement metadata = entry.getFirstChildWithName(new QName("metadata"));
                   	   OMElement format = metadata.getFirstChildWithName(new QName("format"));
                   	   OMElement archived = metadata.getFirstChildWithName(new QName("archived"));

                   	if (!archived.getFirstChildWithName(new QName("value")).getText().equals("true")) {
                   	   if (format.getFirstChildWithName(new QName("value")).getText().equals("drl")) {
                        	   RuleResult rr = new RuleResult();
                        	   rr.name = name;
                        	   OMElement tuuid = metadata.getFirstChildWithName(new QName("uuid"));
                               rr.uuid = tuuid.getFirstChildWithName(new QName("value")).getText();
                               
                               rulelist.add(rr);
                        		   
                       }
                   	}   
                   } 
    		       }
           } catch (Exception e) { e.printStackTrace(); }


    	   return rulelist;

      } 
       /*******************************
        * Get rule uuid via REST
        *******************************/

       private String getAssetUUId(String pkgResource, RuleResult rule) {

    	   String assetURL = droolsServerURL + "/rest/packages/" + pkgResource + "/assets/" + rule.name.replace(" ", "%20");
    	   String line = "";
    	   StringBuilder restText = new StringBuilder();
           String uuid = "";
           
    	   try {
    		   //HttpResponse proxyResponse = KnowledgeAgent.getResponse(assetURL);
    		   restText = new StringBuilder(KnowledgeAgent.getResponse(assetURL));
                   /*logger.info("DBG: return proxy");
    		   // Wrap a BufferedReader around the InputStream
    		   BufferedReader rd = new BufferedReader(new InputStreamReader(proxyResponse.getEntity().getContent()));

    		   // Read response until the end
    		   while ((line = rd.readLine()) != null) { 
    			   restText.append(line); 
    		   }  */
    		   int index = restText.indexOf("<uuid><value>");
    		   uuid = restText.substring(index+13);
    		   uuid = uuid.substring(0, uuid.indexOf("</value>"));
    	   }catch (Exception e) {
    		   logger.debug("dbg: " + e.getMessage());
    	   }

    	   return uuid;
       }
       
       

       /****************************************************
        * Print all methods available from DroolsServlet
        ****************************************************/
       private void printmethods(PrintWriter out, HttpServletResponse resp) {
               resp.setContentType(Format.TEXT.getEncoding());
               out.println("No matches");

               // Print all available methods
               out.println("The following methods are available:");
               out.println("getgreenwichoffset\t- Get GWT offset for US/Central timezone");
               out.println("indaylightsavings\t - Return true/false if in Daylight Savings");
       } 

       
       /*******************************
        * Get Demo Rules List
        *******************************/

       private List<BusinessRule> getDemoList() {

           List<BusinessRule> demolist = new ArrayList<BusinessRule>();

           demolist = KnowledgeAgent.getKnowledgePkg("demoDate").getRules();

    	   return demolist;
    	   
      } 
}
