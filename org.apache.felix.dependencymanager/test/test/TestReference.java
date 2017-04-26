package test;

import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

class TestReference implements ServiceReference {
	Properties props = new Properties();

	public TestReference() {
	}

	public void addProperty(String key, String value) {
		/*
		 * Property keys are case-insensitive. -> see @
		 * org.osgi.framework.ServiceReference
		 */
		props.put(key.toLowerCase(), value);
	}

	public void addProperty(String key, long value) {
		props.put(key, value);
	}

	public void addProperty(String key, int value) {
		props.put(key, value);
	}

	public void addProperty(String key, boolean value) {
		props.put(key, value);
	}

	public void addProperty(String key, String[] multiValue) {
		props.put(key, multiValue);
	}

	@Override
	public Object getProperty(String key) {
		return props.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return props.keySet().toArray(new String[] {});
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	@Override
	public int compareTo(Object reference) {
		// TODO Auto-generated method stub
		return 0;
	}

}