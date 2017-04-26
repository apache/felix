package org.apache.felix.dm.impl.index.multiproperty;

import java.util.Arrays;

public class MultiPropertyKey {

	private MultiPropertyKeyPart[] properties;

	public MultiPropertyKey(int filterSize) {
		properties = new MultiPropertyKeyPart[0];
	}

	// Initial adding key values.
	public void add(String key, String value) {
		int newLength = properties.length + 1;
		MultiPropertyKeyPart[] tmpKeys = new MultiPropertyKeyPart[newLength];
		
		System.arraycopy(properties, 0, tmpKeys, 0, properties.length);
		tmpKeys[newLength-1] = new MultiPropertyKeyPart(key, value);
		
		this.properties = tmpKeys;
		Arrays.sort(properties);
	}

	public void append(MultiPropertyKey other) {
		int newLength = properties.length + other.properties.length;
		MultiPropertyKeyPart[] tmpKeys = new MultiPropertyKeyPart[newLength];
		
		System.arraycopy(properties, 0, tmpKeys, 0, properties.length);
		System.arraycopy(other.properties, 0, tmpKeys, properties.length, other.properties.length);
	
		this.properties = tmpKeys;
		Arrays.sort(properties);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(properties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof MultiPropertyKey)) {
			return false;
		}

		MultiPropertyKey other = (MultiPropertyKey) obj;

		if (properties.length != other.properties.length) {
			return false;
		}

		for (int i = 0; i < properties.length; i++) {
			if (!(properties[i].equals(other.properties[i]))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < properties.length; i++) {
			builder.append(properties[i].key);
			builder.append('=');
			builder.append(properties[i].value);
			if (i < properties.length - 1) {
				builder.append(';');
			}
		}
		return builder.toString();
	}

	// (key=value) part of a multi property index (&(key1=val1)(key2=val2) .. )
	private class MultiPropertyKeyPart implements Comparable<MultiPropertyKeyPart> {
		
		private final String key;
		private final String value;

		public MultiPropertyKeyPart(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			int h = 0;
			h += 31 * key.hashCode();
			h += value.hashCode();
			return h;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof MultiPropertyKeyPart))
				return false;

			MultiPropertyKeyPart object = (MultiPropertyKeyPart) obj;

			if (!(this.key.equals(object.key)))
				return false;
			if (!(this.value.equals(object.value)))
				return false;

			return true;
		}

		@Override
		public int compareTo(MultiPropertyKeyPart o) {
			if (this.key.compareTo(o.key) == 0) {
				return this.value.compareTo(o.value);
			}
			return this.key.compareTo(o.key);
		}
	}
}