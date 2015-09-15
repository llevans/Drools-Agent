
package aes.server.drools;

import java.util.Date;

class BusinessProcess extends Asset {

    String processXML = "";

    public BusinessProcess(String n, String u, String t, Date p, String k) {
           super(n, u, t, p, k);
    }

    public String getProcessXML() { return processXML; }

    void setProcessXML(String xml) { processXML = xml; }

}
