package org.apache.felix.scr.integration.components.felix3680;

public class G
{
    void bindH(H h)
    {
    }
    void start()
    {
        Main._enabledLatch.countDown();
    }

    void stop()
    {
        Main._disabledLatch.countDown();
    }
}
