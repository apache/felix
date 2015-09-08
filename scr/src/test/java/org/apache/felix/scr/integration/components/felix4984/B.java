package org.apache.felix.scr.integration.components.felix4984;

import java.util.ArrayList;
import java.util.List;

public class B
{
    private List<A> as = new ArrayList<A>();

    private void setA(A a)
    {
        as.add( a );
    }

    private void unsetA(A a)
    {
        as.remove( a );
    }

    public List<A> getAs()
    {
        return as;
    }
}
