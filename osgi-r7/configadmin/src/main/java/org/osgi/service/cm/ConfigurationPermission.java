/*
 * Copyright (c) OSGi Alliance (2004, 2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.cm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;

/**
 * Indicates a bundle's authority to configure bundles or be updated by
 * Configuration Admin.
 * 
 * @ThreadSafe
 * @author $Id: ConfigurationPermission.java 1750478 2016-06-28 11:34:40Z
 *         cziegeler $
 * @since 1.2
 */

public final class ConfigurationPermission extends BasicPermission {
	static final long				serialVersionUID	= 5716868734811965383L;
	/**
	 * Provides permission to create new configurations for other bundles as
	 * well as manipulate them. The action string {@value #CONFIGURE}.
	 */
	public final static String		CONFIGURE			= "configure";

	/**
	 * The permission to be updated, that is, act as a Managed Service or
	 * Managed Service Factory. The action string {@value #TARGET}.
	 *
	 * @since 1.4
	 */
	public final static String		TARGET				= "target";

    /**
     * Provides permission to set or remove an attribute on the configuration.
     * The action string {@value #ATTRIBUTE}.
     *
     * @since 1.6
     */
    public final static String      ATTRIBUTE              = "attribute";

    private final static int		ACTION_CONFIGURE	= 0x00000001;
	private final static int		ACTION_TARGET		= 0x00000002;
	private final static int		ACTION_ATTRIBUTE    = 0x00000004;
	private final static int		ACTION_ALL			= ACTION_CONFIGURE | ACTION_TARGET | ACTION_ATTRIBUTE;
	final static int				ACTION_NONE			= 0;

	/**
	 * The actions mask.
	 */
	transient int					action_mask;

	/**
	 * The actions in canonical form.
	 *
	 * @serial
	 */
	private volatile String			actions				= null;

	/**
	 * Parsed name if it includes wildcards: "*"
	 */
	private transient List<String>	substrings;

	/**
	 * Create a new ConfigurationPermission.
	 *
	 * @param name Name of the permission. Wildcards ({@code '*'}) are allowed
	 *        in the name. During {@link #implies(Permission)}, the name is
	 *        matched to the requested permission using the substring matching
	 *        rules used by {@link Filter}s.
	 * @param actions Comma separated list of {@link #CONFIGURE},
	 *        {@link #TARGET} (case insensitive).
	 */

	public ConfigurationPermission(String name, String actions) {
		this(name, parseActions(actions));
	}

	/**
	 * Package private constructor used by ConfigurationPermissionCollection.
	 *
	 * @param name location string
	 * @param mask action mask
	 */
	ConfigurationPermission(String name, int mask) {
		super(name);
		setTransients(mask);
	}

	/**
	 * Called by constructors and when deserialized.
	 *
	 * @param mask action mask
	 */
	private void setTransients(int mask) {
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}
		action_mask = mask;
		substrings = parseSubstring(getName());
	}

	/**
	 * Parse action string into action mask.
	 *
	 * @param actions Action string.
	 * @return action mask.
	 */
	private static int parseActions(String actions) {
		boolean seencomma = false;

		int mask = ACTION_NONE;

		if (actions == null) {
			return mask;
		}

		char[] a = actions.toCharArray();

		int i = a.length - 1;
		if (i < 0)
			return mask;

		while (i != -1) {
			char c;

			// skip whitespace
			while ((i != -1) && ((c = a[i]) == ' ' || c == '\r' || c == '\n' || c == '\f' || c == '\t'))
				i--;

			// check for the known strings
			int matchlen;

			if (i >= 5 && (a[i - 5] == 't' || a[i - 5] == 'T')
					&& (a[i - 4] == 'a' || a[i - 4] == 'A')
					&& (a[i - 3] == 'r' || a[i - 3] == 'R')
					&& (a[i - 2] == 'g' || a[i - 2] == 'G')
					&& (a[i - 1] == 'e' || a[i - 1] == 'E')
					&& (a[i] == 't' || a[i] == 'T')) {
				matchlen = 6;
				mask |= ACTION_TARGET;

			} else
				if (i >= 8 && (a[i - 8] == 'c' || a[i - 8] == 'C')
						&& (a[i - 7] == 'o' || a[i - 7] == 'O')
						&& (a[i - 6] == 'n' || a[i - 6] == 'N')
						&& (a[i - 5] == 'f' || a[i - 5] == 'F')
						&& (a[i - 4] == 'i' || a[i - 4] == 'I')
						&& (a[i - 3] == 'g' || a[i - 3] == 'G')
						&& (a[i - 2] == 'u' || a[i - 2] == 'U')
						&& (a[i - 1] == 'r' || a[i - 1] == 'R')
						&& (a[i] == 'e' || a[i] == 'E')) {
					matchlen = 9;
					mask |= ACTION_CONFIGURE;

    			} else
	    			if (i >= 8 && (a[i - 8] == 'a' || a[i - 8] == 'A')
		    				&& (a[i - 7] == 't' || a[i - 7] == 'T')
			    			&& (a[i - 6] == 't' || a[i - 6] == 'T')
				    		&& (a[i - 5] == 'r' || a[i - 5] == 'R')
					    	&& (a[i - 4] == 'i' || a[i - 4] == 'I')
						    && (a[i - 3] == 'b' || a[i - 3] == 'B')
						    && (a[i - 2] == 'u' || a[i - 2] == 'U')
    						&& (a[i - 1] == 't' || a[i - 1] == 'T')
	     					&& (a[i] == 'e' || a[i] == 'E')) {
		    			matchlen = 9;
			    		mask |= ACTION_ATTRIBUTE;
				    } else {
					   // parse error
    					throw new IllegalArgumentException("invalid actions: " + actions);
	    			}
	    		

			// make sure we didn't just match the tail of a word
			// like "ackbarftarget". Also, skip to the comma.
			seencomma = false;
			while (i >= matchlen && !seencomma) {
				switch (a[i - matchlen]) {
					case ',' :
						seencomma = true;
						/* FALLTHROUGH */
					case ' ' :
					case '\r' :
					case '\n' :
					case '\f' :
					case '\t' :
						break;
					default :
						throw new IllegalArgumentException("invalid permission: " + actions);
				}
				i--;
			}

			// point i at the location of the comma minus one (or -1).
			i -= matchlen;
		}

		if (seencomma) {
			throw new IllegalArgumentException("invalid actions: " + actions);
		}

		return mask;
	}

	/**
	 * Parse the name for wildcard processing.
	 *
	 * @param name The name of the permission.
	 * @return {@code null} is the name has no wildcards or a
	 *         {@code List<String>} where element is a substring to match or
	 *         null for {@code '*'}.
	 */
	private static List<String> parseSubstring(String name) {
		if (name.indexOf('*') < 0) {
			return null;
		}
		char[] chars = name.toCharArray();
		StringBuilder sb = new StringBuilder(chars.length);

		List<String> sub = new ArrayList<String>(10);

		for (int pos = 0; pos < chars.length; pos++) {
			char c = chars[pos];

			switch (c) {
				case '*' : {
					if (sb.length() > 0) {
						sub.add(sb.toString());
					}
					sb.setLength(0);
					sub.add(null);
					break;
				}

				case '\\' : {
					pos++;
					if (pos < chars.length) {
						c = chars[pos];
					}
					/* fall through into default */
				}

				default : {
					sb.append(c);
					break;
				}
			}
		}
		if (sb.length() > 0) {
			sub.add(sb.toString());
		}

		int size = sub.size();

		if (size == 0) {
			return null;
		}

		if (size == 1) {
			if (sub.get(0) != null) {
				return null;
			}
		}
		return sub;
	}

	/**
	 * Determines if a {@code ConfigurationPermission} object "implies" the
	 * specified permission.
	 *
	 * @param p The target permission to check.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */

	@Override
	public boolean implies(Permission p) {
		if (!(p instanceof ConfigurationPermission)) {
			return false;
		}
		ConfigurationPermission requested = (ConfigurationPermission) p;
		return implies0(requested, ACTION_NONE);
	}

	/**
	 * Internal implies method. Used by the implies and the permission
	 * collection implies methods.
	 *
	 * @param requested The requested ConfigurationPermission which has already
	 *        be validated as a proper argument.
	 * @param effective The effective actions with which to start.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	boolean implies0(ConfigurationPermission requested, int effective) {
		/* check actions first - much faster */
		effective |= action_mask;
		final int desired = requested.action_mask;
		if ((effective & desired) != desired) {
			return false;
		}
		String requestedName = requested.getName();
		if (substrings == null) {
			return getName().equals(requestedName);
		}
		for (int i = 0, pos = 0, size = substrings.size(); i < size; i++) {
			String substr = substrings.get(i);

			if (i + 1 < size) /* if this is not that last substr */{
				if (substr == null) /* * */{
					String substr2 = substrings.get(i + 1);

					if (substr2 == null) /* ** */
						continue; /* ignore first star */
					/* xxx */
					int index = requestedName.indexOf(substr2, pos);
					if (index == -1) {
						return false;
					}

					pos = index + substr2.length();
					if (i + 2 < size) // if there are more
						// substrings, increment
						// over the string we just
						// matched; otherwise need
						// to do the last substr
						// check
						i++;
				} else /* xxx */{
					int len = substr.length();
					if (requestedName.regionMatches(pos, substr, 0, len)) {
						pos += len;
					} else {
						return false;
					}
				}
			} else /* last substr */{
				if (substr == null) /* * */{
					return true;
				}
				/* xxx */
				return requestedName.endsWith(substr);
			}
		}

		return false;
	}

	/**
	 * Determines the equality of two {@code ConfigurationPermission} objects.
	 * <p>
	 * Two {@code ConfigurationPermission} objects are equal.
	 *
	 * @param obj The object being compared for equality with this object.
	 * @return {@code true} if {@code obj} is equivalent to this
	 *         {@code ConfigurationPermission}; {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ConfigurationPermission)) {
			return false;
		}

		ConfigurationPermission cp = (ConfigurationPermission) obj;

		return (action_mask == cp.action_mask) && getName().equals(cp.getName());
	}

	/**
	 * Returns the hash code value for this object.
	 *
	 * @return Hash code value for this object.
	 */

	@Override
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		return h;
	}

	/**
	 * Returns the canonical string representation of the
	 * {@code ConfigurationPermission} actions.
	 *
	 * <p>
	 * Always returns present {@code ConfigurationPermission} actions in the
	 * following order: {@value #CONFIGURE}, {@value #TARGET}
	 *
	 * @return Canonical string representation of the
	 *         {@code ConfigurationPermission} actions.
	 */
	@Override
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuilder sb = new StringBuilder();
			boolean comma = false;

			int mask = action_mask;
			if ((mask & ACTION_CONFIGURE) == ACTION_CONFIGURE) {
				sb.append(CONFIGURE);
				comma = true;
			}

			if ((mask & ACTION_TARGET) == ACTION_TARGET) {
				if (comma)
					sb.append(',');
				sb.append(TARGET);
				comma = true;
			}

			if ((mask & ACTION_ATTRIBUTE) == ACTION_ATTRIBUTE) {
				if (comma)
					sb.append(',');
				sb.append(ATTRIBUTE);
			}

			actions = result = sb.toString();
		}

		return result;
	}

	/**
	 * Returns a new {@code PermissionCollection} object suitable for storing
	 * {@code ConfigurationPermission}s.
	 *
	 * @return A new {@code PermissionCollection} object.
	 */
	@Override
	public PermissionCollection newPermissionCollection() {
		return new ConfigurationPermissionCollection();
	}

	/**
	 * WriteObject is called to save the state of this permission object to a
	 * stream. The actions are serialized, and the superclass takes care of the
	 * name.
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
		// Write out the actions. The superclass takes care of the name
		// call getActions to make sure actions field is initialized
		if (actions == null)
			getActions();
		s.defaultWriteObject();
	}

	/**
	 * readObject is called to restore the state of this permission from a
	 * stream.
	 */
	private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
		// Read in the data, then initialize the transients
		s.defaultReadObject();
		setTransients(parseActions(actions));
	}
}

/**
 * Stores a set of {@code ConfigurationPermission} permissions.
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
final class ConfigurationPermissionCollection extends PermissionCollection {
	static final long								serialVersionUID	= -6917638867081695839L;
	/**
	 * Collection of permissions.
	 *
	 * @serial
	 * @GuardedBy this
	 */
	private Map<String, ConfigurationPermission>	permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 *
	 * @serial
	 * @GuardedBy this
	 */
	private boolean									all_allowed;

	/**
	 * Creates an empty {@code ConfigurationPermissionCollection} object.
	 *
	 */
	public ConfigurationPermissionCollection() {
		permissions = new HashMap<String, ConfigurationPermission>();
		all_allowed = false;
	}

	/**
	 * Adds the specified permission to the
	 * {@code ConfigurationPermissionCollection}. The key for the hash is the
	 * interface name of the service.
	 *
	 * @param permission The {@code Permission} object to add.
	 *
	 * @exception IllegalArgumentException If the permission is not an
	 *            {@code ConfigurationPermission}.
	 *
	 * @exception SecurityException If this ConfigurationPermissionCollection
	 *            object has been marked read-only.
	 */

	@Override
	public void add(Permission permission) {
		if (!(permission instanceof ConfigurationPermission)) {
			throw new IllegalArgumentException("invalid permission: " + permission);
		}

		if (isReadOnly())
			throw new SecurityException("attempt to add a Permission to a " + "readonly PermissionCollection");

		final ConfigurationPermission cp = (ConfigurationPermission) permission;
		final String name = cp.getName();
		synchronized (this) {
			Map<String, ConfigurationPermission> pc = permissions;
			final ConfigurationPermission existing = pc.get(name);
			if (existing != null) {
				final int oldMask = existing.action_mask;
				final int newMask = cp.action_mask;
				if (oldMask != newMask) {
					pc.put(name, new ConfigurationPermission(name, oldMask | newMask));
				}
			} else {
				pc.put(name, cp);
			}

			if (!all_allowed) {
				if (name.equals("*")) {
					all_allowed = true;
				}
			}
		}
	}

	/**
	 * Determines if the specified permissions implies the permissions expressed
	 * in {@code permission}.
	 *
	 * @param permission The Permission object to compare with this
	 *        {@code ConfigurationPermission} object.
	 * @return {@code true} if {@code permission} is a proper subset of a
	 *         permission in the set; {@code false} otherwise.
	 */
	@Override
	public boolean implies(Permission permission) {
		if (!(permission instanceof ConfigurationPermission)) {
			return false;
		}
		final ConfigurationPermission requested = (ConfigurationPermission) permission;
		int effective = ConfigurationPermission.ACTION_NONE;

		Collection<ConfigurationPermission> perms;
		synchronized (this) {
			Map<String, ConfigurationPermission> pc = permissions;
			/* short circuit if the "*" Permission was added */
			if (all_allowed) {
				ConfigurationPermission cp = pc.get("*");
				if (cp != null) {
					effective |= cp.action_mask;
					final int desired = requested.action_mask;
					if ((effective & desired) == desired) {
						return true;
					}
				}
			}
			perms = pc.values();
		}
		/* iterate one by one over permissions */
		for (ConfigurationPermission perm : perms) {
			if (perm.implies0(requested, effective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an enumeration of all {@code ConfigurationPermission} objects in
	 * the container.
	 *
	 * @return Enumeration of all {@code ConfigurationPermission} objects.
	 */
	@Override
	public synchronized Enumeration<Permission> elements() {
		List<Permission> all = new ArrayList<Permission>(permissions.values());
		return Collections.enumeration(all);
	}

	/* serialization logic */
	private static final ObjectStreamField[]	serialPersistentFields	= {new ObjectStreamField("hasElement", Boolean.TYPE), new ObjectStreamField("permissions", HashMap.class),
			new ObjectStreamField("all_allowed", Boolean.TYPE)			};

	private synchronized void writeObject(ObjectOutputStream out) throws IOException {
		ObjectOutputStream.PutField pfields = out.putFields();
		pfields.put("hasElement", false);
		pfields.put("permissions", permissions);
		pfields.put("all_allowed", all_allowed);
		out.writeFields();
	}

	private synchronized void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream.GetField gfields = in.readFields();
		boolean hasElement = gfields.get("hasElement", false);
		if (hasElement) { // old format
			permissions = new HashMap<String, ConfigurationPermission>();
			permissions.put("*", new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE));
			all_allowed = true;
		} else {
			@SuppressWarnings("unchecked")
			HashMap<String, ConfigurationPermission> p = (HashMap<String, ConfigurationPermission>) gfields.get("permissions", new HashMap<String, ConfigurationPermission>());
			permissions = p;
			all_allowed = gfields.get("all_allowed", false);
		}
	}
}
