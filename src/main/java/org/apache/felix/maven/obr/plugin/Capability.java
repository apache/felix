package org.apache.felix.maven.obr.plugin;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * this class describe and store capability node.
 * 
 * @author Maxime
 * 
 */
public class Capability
{

    /**
     * m_name: name of the capability.
     */
    private String m_name;

    /**
     * m_p: List of PElement.
     */
    private List m_p = new ArrayList();

    /**
     * get the name attribute.
     * 
     * @return name attribute
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * set the name attribute.
     * 
     * @param m_name new name value
     *            
     */
    public void setName(String m_name)
    {
        this.m_name = m_name;
    }

    /**
     * return the capabilities.
     * 
     * @return List of PElement
     */
    public List getP()
    {
        return m_p;
    }

    /**
     * set the capabilities.
     * 
     * @param m_p List of PElement
     *            
     */
    public void setP(List m_p)
    {
        this.m_p = m_p;
    }

    /**
     * add one element in List.
     * 
     * @param pelement PElement
     *            
     */
    public void addP(PElement pelement)
    {
        m_p.add(pelement);
    }

    /**
     * transform this object to Node.
     * 
     * @param father father document for create Node
     * @return node
     */
    public Node getNode(Document father)
    {
        Element capability = father.createElement("capability");
        capability.setAttribute("name", this.getName());
        for (int i = 0; i < this.getP().size(); i++)
        {
            capability.appendChild(((PElement) (this.getP().get(i))).getNode(father));
        }
        return capability;
    }

}
