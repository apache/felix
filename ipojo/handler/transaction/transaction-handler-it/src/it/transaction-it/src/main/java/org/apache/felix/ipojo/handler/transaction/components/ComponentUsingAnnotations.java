package org.apache.felix.ipojo.handler.transaction.components;

import org.apache.felix.ipojo.annotations.Component;

import javax.transaction.Transaction;

@Component
@org.apache.felix.ipojo.transaction.Transaction(field="transaction")
public class ComponentUsingAnnotations {

    Transaction transaction;


    @org.apache.felix.ipojo.transaction.Transactional
    public void doSomethingBad() throws NullPointerException {
    }

    @org.apache.felix.ipojo.transaction.Transactional(propagation="required")
    public void doSomethingBad2() throws UnsupportedOperationException {

    }

    @org.apache.felix.ipojo.transaction.Transactional(propagation="supported", norollbackfor= {"java.lang.Exception"})
    public void doSomethingGood() {

    }

    @org.apache.felix.ipojo.transaction.Transactional(timeout=1000, exceptiononrollback=true)
    public void doSomethingLong() {

    }


}
