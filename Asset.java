package aes.server.drools;

import java.util.Date;

class Asset {

    String name = "";
    String uuid = "";
    String type = "";
    Date published;

    String packageName;


    public Asset() {}
    
    public Asset(String n, String u, String t, Date p, String k) {
          name = n;
          uuid = u;
          type = t;
          published = p;
          packageName = k;
    }

    public String getName() { return name; }

    public String getUuid() { return uuid; }

    public String getType() { return type; }
   
    public Date getPublished() { return published; }

    void setName(String n) { name = n; }

    void setUuid(String u) { uuid = u; }

    void setType(String t) { type = t; }

    String getPkgName() { return packageName; }

    void setPkgName(String kname) { packageName = kname; }

}
