
package com.proj.webservice.impl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.proj.webservice.impl package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SyncMaterial_QNAME = new QName("http://impl.webservice.proj.com/", "syncMaterial");
    private final static QName _SyncMaterialResponse_QNAME = new QName("http://impl.webservice.proj.com/", "syncMaterialResponse");
    private final static QName _SyncProductionCode_QNAME = new QName("http://impl.webservice.proj.com/", "syncProductionCode");
    private final static QName _SyncProductionCodeResponse_QNAME = new QName("http://impl.webservice.proj.com/", "syncProductionCodeResponse");
    private final static QName _SyncSeries_QNAME = new QName("http://impl.webservice.proj.com/", "syncSeries");
    private final static QName _SyncSeriesResponse_QNAME = new QName("http://impl.webservice.proj.com/", "syncSeriesResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.proj.webservice.impl
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SyncMaterial }
     * 
     */
    public SyncMaterial createSyncMaterial() {
        return new SyncMaterial();
    }

    /**
     * Create an instance of {@link SyncMaterialResponse }
     * 
     */
    public SyncMaterialResponse createSyncMaterialResponse() {
        return new SyncMaterialResponse();
    }

    /**
     * Create an instance of {@link SyncProductionCode }
     * 
     */
    public SyncProductionCode createSyncProductionCode() {
        return new SyncProductionCode();
    }

    /**
     * Create an instance of {@link SyncProductionCodeResponse }
     * 
     */
    public SyncProductionCodeResponse createSyncProductionCodeResponse() {
        return new SyncProductionCodeResponse();
    }

    /**
     * Create an instance of {@link SyncSeries }
     * 
     */
    public SyncSeries createSyncSeries() {
        return new SyncSeries();
    }

    /**
     * Create an instance of {@link SyncSeriesResponse }
     * 
     */
    public SyncSeriesResponse createSyncSeriesResponse() {
        return new SyncSeriesResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncMaterial }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncMaterial")
    public JAXBElement<SyncMaterial> createSyncMaterial(SyncMaterial value) {
        return new JAXBElement<SyncMaterial>(_SyncMaterial_QNAME, SyncMaterial.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncMaterialResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncMaterialResponse")
    public JAXBElement<SyncMaterialResponse> createSyncMaterialResponse(SyncMaterialResponse value) {
        return new JAXBElement<SyncMaterialResponse>(_SyncMaterialResponse_QNAME, SyncMaterialResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncProductionCode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncProductionCode")
    public JAXBElement<SyncProductionCode> createSyncProductionCode(SyncProductionCode value) {
        return new JAXBElement<SyncProductionCode>(_SyncProductionCode_QNAME, SyncProductionCode.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncProductionCodeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncProductionCodeResponse")
    public JAXBElement<SyncProductionCodeResponse> createSyncProductionCodeResponse(SyncProductionCodeResponse value) {
        return new JAXBElement<SyncProductionCodeResponse>(_SyncProductionCodeResponse_QNAME, SyncProductionCodeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncSeries }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncSeries")
    public JAXBElement<SyncSeries> createSyncSeries(SyncSeries value) {
        return new JAXBElement<SyncSeries>(_SyncSeries_QNAME, SyncSeries.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SyncSeriesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.webservice.proj.com/", name = "syncSeriesResponse")
    public JAXBElement<SyncSeriesResponse> createSyncSeriesResponse(SyncSeriesResponse value) {
        return new JAXBElement<SyncSeriesResponse>(_SyncSeriesResponse_QNAME, SyncSeriesResponse.class, null, value);
    }

}
