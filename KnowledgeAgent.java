package aes.server.drools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.description.ClientUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
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
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
//import org.drools.definition.KnowledgePackage;
//import org.drools.definition.rule.Rule;
//import org.drools.definitions.rule.impl.RuleImpl;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.io.impl.InputStreamResource;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.resting.Resting;
import com.google.resting.component.impl.ServiceResponse;

import javax.xml.namespace.QName;

    /**
     * @author lyn.evans
     *
     */
    public class KnowledgeAgent
    {
    	static Logger logger = Logger.getLogger(KnowledgeAgent.class);
       
        private static HashMap<String, Date> latestVersions = new HashMap<String, Date>();
     
        private static HashMap<String, Date> tempVersions = new HashMap<String, Date>();

        private static SimpleDateFormat publishedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        private static String droolsServerURL;
 
        public static HashMap<String, KnowledgePackage> knowledgePkgs = new HashMap<String, KnowledgePackage>();

        private static Boolean buildingPkg = false;

        public static KnowledgeAgent getInstance() {
           
            return null;
        }   
       
        /**
         *
         * Initialize all local knowledge packages
         *
         */
        private void init() 
        {
        	
        }
       
        
        /** 
         * Acquire binary knowledge package from Drools repository
         *
         */


         private static void getBinary(KnowledgePackage pkg) {

      	    Properties properties = new Properties();
            properties.setProperty( "drools.dialect.java.compiler","JANINO" );

            KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(properties);
             

            try {
            KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);
            Resource urlResource = ResourceFactory.newUrlResource(droolsServerURL + "/rest/packages/" + pkg.getName() + "/binary" );
            kbuilder.add(urlResource, ResourceType.PKG );
            //kbuilder.add(new InputStreamResource(getBinaryResponse(droolsServerURL + "/rest/packages/" + pkgName + "/binary")), ResourceType.PKG);
            KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
            kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());


            kbuilder = null;
           
            pkg.setKnowledgeBase(kbase);
            
            logger.info("DBG: " + kbase.toString());
            
            } catch (Exception e) { e.printStackTrace(); }

         }

        /******************************************************************
         * Build local knowledge package - binary and assets' metadata
         ******************************************************************/
         
        private static void loadKnowledgePkg(String pkgName) {

              
            synchronized(KnowledgeAgent.class) {

            buildingPkg = true;
            //Properties properties = new Properties();
            //properties.setProperty( "drools.dialect.java.compiler","JANINO" );

            //KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(properties);
           

            KnowledgePackage pkg = new KnowledgePackage();
            pkg.setName(pkgName);
            

            getBinary(pkg);

            getMetaData(pkg);

            knowledgePkgs.remove(pkgName);
            knowledgePkgs.put(pkgName, pkg);
            
            buildingPkg = false;
            }
        }

        /***********************************************************
         * Refresh local knowledge package - binary and meta data
         ***********************************************************/
        
        private static void refreshKnowledgePkg(String pkgName) {
              
                 logger.info("dbg: refresh "  +  pkgName);
  
                 loadKnowledgePkg(pkgName);
        }


        /************************* 
         * Set droolsServerURL
         *************************/

        public static void setDroolsServerURL(String url) { droolsServerURL = url; }

        public static String getDroolsServerURL() { return droolsServerURL; }

        /**
         * Load or refresh local knowledge package 
         **/
       
        public static KnowledgePackage getKnowledgePkg(String pkgName)
        {
           if (knowledgePkgs.get(pkgName) == null) {
               loadKnowledgePkg(pkgName);

               logger.info("DBG: call create for " + pkgName + " since null");
           }
           else	if (!buildingPkg && assetsUpdated(pkgName, knowledgePkgs.get(pkgName).getAssetsXML())) {

              refreshKnowledgePkg(pkgName);
           }

           return knowledgePkgs.get(pkgName);
        }


      
      /********************************************************************************
       * Compare local package definition (rest text) with real-time package 
       * definition at Drools repository.
       *******************************************************************************/
        
       private synchronized static Boolean assetsUpdated(String pkgName, String assetsXML) {

           boolean pkgUpdate = false;

           String assetURL = droolsServerURL + "/rest/packages/MASS/assets";
           String line = "";
           String restText = new String();

           try {

             restText = getResponse(assetURL);
            

           } catch(Exception e) { e.printStackTrace(); }

           if (!assetsXML.equals(restText))
                   pkgUpdate = true;
           

           logger.info("DBG: Comparing assetsText " + pkgName + ": " + pkgUpdate);
    	   return pkgUpdate;
      } 
 

      /********************************************************************
       * Get Asset meta data for assets in knowledge package using REST
       *******************************************************************/
       
       private static void getMetaData(KnowledgePackage pkg) {

    	   String assetURL = droolsServerURL + "/rest/packages/" + pkg.getName() + "/assets";
           
    	   String line = "";
    	   String restText = new String();

    	   try {
        
    		    restText = getResponse(assetURL);
              
    		    if (restText.length() < 10) {
    		    	// Try Drools Rest request again - first attempt may get a SocketException read time out
    		    	
        		    restText = getResponse(assetURL);

    		    }

                   pkg.setAssetsXML(restText.toString());

    		       XMLStreamReader xmlStreamReader =
    	                StAXUtils.createXMLStreamReader(new StringReader(restText.toString()));
    	           StAXOMBuilder b = new StAXOMBuilder(xmlStreamReader);
    	           
    		       OMElement root = b.getDocumentElement();

    		       for (Iterator j = root.getChildrenWithName(new QName("entry")); j.hasNext();) {

                           Asset kasset = null;
 
                	   OMElement entry = (OMElement) j.next();
                	   
                	   String name = "";
                	   String uuid = "";
                	   String type = "";
                       Date pubdate;
                	   
      		       for (Iterator k = entry.getChildrenWithName(new QName("id")); k.hasNext();) {

                  	   OMElement t_asset = (OMElement) k.next();
                       
                   	   OMElement t_metadata  = entry.getFirstChildWithName(new QName("metadata"));
                   	   OMElement t_archived  = t_metadata.getFirstChildWithName(new QName("archived"));
                   	   OMElement t_format    = t_metadata.getFirstChildWithName(new QName("format"));
                   	   OMElement t_uuid      = t_metadata.getFirstChildWithName(new QName("uuid"));
                   	   Iterator  t_published = entry.getChildrenWithLocalName("published");

                       if (!t_archived.getFirstChildWithName(new QName("value")).getText().equals("true")) {

                   	      name = t_asset.getText().substring(t_asset.getText().lastIndexOf("/")+1).replaceAll("%20", " ");
                              type = t_format.getFirstChildWithName(new QName("value")).getText();
                              uuid = t_uuid.getFirstChildWithName(new QName("value")).getText();
                              pubdate = publishedFormat.parse(((OMElement)t_published.next()).getText().trim());

                              if (type.equals("drl") || type.equals("bpmn2")) {
                                  if (type.equals("drl")) {
                                     kasset = new BusinessRule(name, uuid, type, pubdate, pkg.getName());
                                     //((BusinessRule)kasset).setRuleText(getResponse(droolsServerURL + "/guvnor/rest/packages/" + pkg.getName() + "/" + name.replace(" ", "%20") + "/source")); 
                                  }
                                  else if (type.equals("bpmn2")) {
                                     kasset = new BusinessProcess(name, uuid, type, pubdate, pkg.getName());
                                     //((BusinessProcess)kasset).setProcessXML(getResponse(droolsServerURL + "/guvnor/rest/packages/" + pkg.getName() + "/" + name.replace(" ", "%20") + "/source")); 
                                  }
                                  pkg.Assets.put(name, kasset);
                                  pkg.Assets.put(uuid, kasset);
                              }
                       }
                   } 
    	      }
           } catch (Exception e) { 
                 logger.info("DBG: restText " + restText);
                 e.printStackTrace(); }

      } 

       /********************************************************************
        * Initiate Drools Knowledge Session 
        ********************************************************************/

       public synchronized static RuleSession getRuleSession(String ruleName) {

           RuleSession ruleSess = null;

           ArrayList<BusinessRule> rules = new ArrayList<BusinessRule>();
           
           ruleName = ruleName.replaceAll("%20", " ");

           for (String k : knowledgePkgs.keySet()) {

                   if (knowledgePkgs.get(k).Assets.get(ruleName) != null) {
                          
                	      rules.add((BusinessRule)knowledgePkgs.get(k).Assets.get(ruleName));

                	      ruleSess = new RuleSession(rules);
                	      
                          break;
                   }
           }

           return ruleSess;
    }

   /********************************************************************
    * Return a rule object from the local knowledge package 
    ********************************************************************/
       
    public synchronized static BusinessRule getRule(String ruleName) {


           BusinessRule rule = null;

           ruleName = ruleName.replaceAll("%20", " ");

           for (String k : knowledgePkgs.keySet()) {

                   if (knowledgePkgs.get(k).Assets.get(ruleName) != null) {
                          
                          rule = (BusinessRule)knowledgePkgs.get(k).Assets.get(ruleName);

                          break;

                   }
           }

           return rule;
    }

    /***************************************************************************
     * Load local knowledge package binary from Drools HTTP REST response
     ***************************************************************************/
        
   public static InputStream getBinaryResponse(String externalURL) {

       InputStream rstream = null;
       HttpURLConnection conn = null;
       
	   try {
           URL url = new URL(externalURL);

           conn = (HttpURLConnection) url.openConnection();
 
           conn.setRequestMethod("GET");

           conn.setRequestProperty("Accept", "application/octet-stream");

           conn.connect();

           rstream = conn.getInputStream();
       
	   } catch (Exception e) { e.printStackTrace(); }

	   finally {  conn.disconnect(); }

	   return rstream;


   }

   /********************************************************************
    * Issue HTTP GET request to Drools REST 
    ********************************************************************/
   
   public static synchronized String getResponse(String externalURL) {
             //throws ServletException {
	   
	   Future<String> future = null;
	   ExecutorService pool  = null;
   String responseXML = "";
        
        try {
        	
        	DoCall doCall = new DoCall(externalURL);
        	
        	pool = Executors.newFixedThreadPool(3);
        	
        	future = pool.submit(doCall); 
        
        	responseXML = future.get(10000, TimeUnit.SECONDS);
        	
       
        } catch (Exception e) { 
        	
        	e.printStackTrace(); 
        	
        } finally {
        	
        	pool.shutdown();
        }
        
        return responseXML;
  	} 
   
   
   /********************************************************************
    * Issue HTTP GET request to Drools REST for asset XML
    ********************************************************************/

    private static StringBuilder getXMLResponse(String hostURL) { 
                // throws ServletException {
         HttpClient client = new DefaultHttpClient();
         HttpGet httpGet = new HttpGet(hostURL);
         HttpResponse httpResponse = null;

         StringBuilder restText = new StringBuilder();
         String line = "";
         
         
         try {
                 httpResponse = client.execute(httpGet);
                 BufferedReader rd = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

                 // Read response until the end
                 while ((line = rd.readLine()) != null) {
                         restText.append(line);
                 }
                 
         } catch (Exception e) {
                 e.printStackTrace();
         }

         return restText;
   }

}
 
/****************************************************
 * Wrap Drools request in Java thread
 ****************************************************/

class DoCall implements Callable<String> {
 	   
        DefaultHttpClient httpclient = null;
        String externalURL;
        
        public DoCall(String url) { externalURL = url; }
        
    	    public String call() {
    	    	    
    		        HttpGet httpget = new HttpGet(externalURL);
    		 
    		        httpget.setHeader("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");

    		        HttpParams httpParameters = new BasicHttpParams();
    		        int timeoutConnection = 2500;
    		        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
    		        
    		        int timeoutSocket = 3000;
    		        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

    		        httpclient = new DefaultHttpClient(httpParameters);
    		        
    		        StringWriter writer = null;
    	    	    
    		        HttpResponse dresponse;

    	    	    try {
      		             dresponse = httpclient.execute(httpget);
    	    	    
    	    	         HttpEntity entity = dresponse.getEntity();
    	 	       
    		             writer = new StringWriter();
    		             IOUtils.copy(entity.getContent(), writer);
    		        
    	    	    } catch (Exception e) { e.printStackTrace(); }

                    finally { 

    		            httpclient.getConnectionManager().shutdown();  
                    }
    	    	    
    	    	    return writer.toString();
    	    }   
}


   
