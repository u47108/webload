
package cl.clarochile.companyusermanagement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import cl.clarochile.webservices.schemas.CompanyUserType;


/**
 * <p>Clase Java para anonymous complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="companyUser" type="{http://webservices.clarochile.cl/schemas}CompanyUserType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "companyUser"
})
@XmlRootElement(name = "AddUserRequest")
public class AddUserRequest {

    @XmlElement(required = true)
    protected CompanyUserType companyUser;

    /**
     * Obtiene el valor de la propiedad companyUser.
     * 
     * @return
     *     possible object is
     *     {@link CompanyUserType }
     *     
     */
    public CompanyUserType getCompanyUser() {
        return companyUser;
    }

    /**
     * Define el valor de la propiedad companyUser.
     * 
     * @param value
     *     allowed object is
     *     {@link CompanyUserType }
     *     
     */
    public void setCompanyUser(CompanyUserType value) {
        this.companyUser = value;
    }

}
