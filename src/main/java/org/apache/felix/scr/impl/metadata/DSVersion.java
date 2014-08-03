package org.apache.felix.scr.impl.metadata;

public enum DSVersion
{
    DSnone(-1),
    DS10(0),
    DS11(1),
    DS11Felix(2),
    DS12(3),
    DS12Felix(4),
    DS13(5);
    
    private final int version;
    
    DSVersion(int version) 
    {
        this.version = version;
    }
    
    public boolean isDS10()
    {
        return version >=DS10.version;
    }

    public boolean isDS11()
    {
        return version >=DS11.version;
    }

    public boolean isDS12()
    {
        return version >=DS12.version;
    }

    public boolean isDS13()
    {
        return version >=DS13.version;
    }

}
