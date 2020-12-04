package com.proj.webservice.impl;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.1.12
 * 2018-04-19T16:35:26.631+08:00
 * Generated source version: 3.1.12
 * 
 */
@WebServiceClient(name = "syncData", 
                  wsdlLocation = "http://192.168.4.201:8081//webservice/syncData?wsdl",
                  targetNamespace = "http://impl.webservice.proj.com/") 
public class SyncData extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://impl.webservice.proj.com/", "syncData");
    public final static QName SyncDataServiceImplPort = new QName("http://impl.webservice.proj.com/", "SyncDataServiceImplPort");
    static {
        URL url = null;
        try {
            url = new URL("http://192.168.4.201:8081//webservice/syncData?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(SyncData.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "http://192.168.4.201:8081//webservice/syncData?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public SyncData(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public SyncData(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SyncData() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    public SyncData(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public SyncData(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public SyncData(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    




    /**
     *
     * @return
     *     returns SyncDataService
     */
    @WebEndpoint(name = "SyncDataServiceImplPort")
    public SyncDataService getSyncDataServiceImplPort() {
        return super.getPort(SyncDataServiceImplPort, SyncDataService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns SyncDataService
     */
    @WebEndpoint(name = "SyncDataServiceImplPort")
    public SyncDataService getSyncDataServiceImplPort(WebServiceFeature... features) {
        return super.getPort(SyncDataServiceImplPort, SyncDataService.class, features);
    }

}