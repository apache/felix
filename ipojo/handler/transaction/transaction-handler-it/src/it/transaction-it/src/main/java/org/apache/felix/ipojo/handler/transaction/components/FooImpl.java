package org.apache.felix.ipojo.handler.transaction.components;

import org.apache.felix.ipojo.handler.transaction.services.Foo;

public class FooImpl implements Foo {

    public void doSomethingBad() throws NullPointerException {
        throw new NullPointerException("NULL");
    }

    public void doSomethingBad2() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Expected exception");

    }

    public void doSomethingGood() {
        // Good...
    }

    public void doSomethingLong() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
