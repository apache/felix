/*
 * Copyright (c) OSGi Alliance (2011, 2012). All Rights Reserved.
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

package org.osgi.framework;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Version range. A version range is an interval describing a set of
 * {@link Version versions}.
 * 
 * <p>
 * A range has a left (lower) endpoint and a right (upper) endpoint. Each
 * endpoint can be open (excluded from the set) or closed (included in the set).
 * 
 * <p>
 * {@code VersionRange} objects are immutable.
 * 
 * @since 1.7
 * @Immutable
 * @version $Id: d0c21e6a5015a7fa0b33179a29122ea7d137145a $
 */

public class VersionRange {
	/**
	 * The left endpoint is open and is excluded from the range.
	 * <p>
	 * The value of {@code LEFT_OPEN} is {@code '('}.
	 */
	public static final char	LEFT_OPEN				= '(';
	/**
	 * The left endpoint is closed and is included in the range.
	 * <p>
	 * The value of {@code LEFT_CLOSED} is {@code '['}.
	 */
	public static final char	LEFT_CLOSED				= '[';
	/**
	 * The right endpoint is open and is excluded from the range.
	 * <p>
	 * The value of {@code RIGHT_OPEN} is {@code ')'}.
	 */
	public static final char	RIGHT_OPEN				= ')';
	/**
	 * The right endpoint is closed and is included in the range.
	 * <p>
	 * The value of {@code RIGHT_CLOSED} is {@code ']'}.
	 */
	public static final char	RIGHT_CLOSED			= ']';

	private final boolean		leftClosed;
	private final Version		left;
	private final Version		right;
	private final boolean		rightClosed;
	private final boolean		empty;

	private transient String	versionRangeString /* default to null */;
	private transient int		hash /* default to 0 */;

	private static final String	LEFT_OPEN_DELIMITER		= "(";
	private static final String	LEFT_CLOSED_DELIMITER	= "[";
	private static final String	LEFT_DELIMITERS			= LEFT_CLOSED_DELIMITER + LEFT_OPEN_DELIMITER;
	private static final String	RIGHT_OPEN_DELIMITER	= ")";
	private static final String	RIGHT_CLOSED_DELIMITER	= "]";
	private static final String	RIGHT_DELIMITERS		= RIGHT_OPEN_DELIMITER + RIGHT_CLOSED_DELIMITER;
	private static final String	ENDPOINT_DELIMITER		= ",";

	/**
	 * Creates a version range from the specified versions.
	 * 
	 * @param leftType Must be either {@link #LEFT_CLOSED} or {@link #LEFT_OPEN}
	 *        .
	 * @param leftEndpoint Left endpoint of range. Must not be {@code null}.
	 * @param rightEndpoint Right endpoint of range. May be {@code null} to
	 *        indicate the right endpoint is <i>Infinity</i>.
	 * @param rightType Must be either {@link #RIGHT_CLOSED} or
	 *        {@link #RIGHT_OPEN}.
	 * @throws IllegalArgumentException If the arguments are invalid.
	 */
	public VersionRange(char leftType, Version leftEndpoint, Version rightEndpoint, char rightType) {
		if ((leftType != LEFT_CLOSED) && (leftType != LEFT_OPEN)) {
			throw new IllegalArgumentException("invalid leftType \"" + leftType + "\"");
		}
		if ((rightType != RIGHT_OPEN) && (rightType != RIGHT_CLOSED)) {
			throw new IllegalArgumentException("invalid rightType \"" + rightType + "\"");
		}
		if (leftEndpoint == null) {
			throw new IllegalArgumentException("null leftEndpoint argument");
		}
		leftClosed = leftType == LEFT_CLOSED;
		rightClosed = rightType == RIGHT_CLOSED;
		left = leftEndpoint;
		right = rightEndpoint;
		empty = isEmpty0();
	}

	/**
	 * Creates a version range from the specified string.
	 * 
	 * <p>
	 * Version range string grammar:
	 * 
	 * <pre>
	 * range ::= interval | atleast
	 * interval ::= ( '[' | '(' ) left ',' right ( ']' | ')' )
	 * left ::= version
	 * right ::= version
	 * atleast ::= version
	 * </pre>
	 * 
	 * @param range String representation of the version range. The versions in
	 *        the range must contain no whitespace. Other whitespace in the
	 *        range string is ignored.
	 * @throws IllegalArgumentException If {@code range} is improperly
	 *         formatted.
	 */
	public VersionRange(String range) {
		boolean closedLeft;
		boolean closedRight;
		Version endpointLeft;
		Version endpointRight;

		try {
			StringTokenizer st = new StringTokenizer(range, LEFT_DELIMITERS, true);
			String token = st.nextToken().trim(); // whitespace or left delim
			if (token.length() == 0) { // leading whitespace
				token = st.nextToken(); // left delim
			}
			closedLeft = LEFT_CLOSED_DELIMITER.equals(token);
			if (!closedLeft && !LEFT_OPEN_DELIMITER.equals(token)) {
				// first token is not a delimiter, so it must be "atleast"
				if (st.hasMoreTokens()) { // there must be no more tokens
					throw new IllegalArgumentException("invalid range \"" + range + "\": invalid format");
				}
				leftClosed = true;
				rightClosed = false;
				left = parseVersion(token, range);
				right = null;
				empty = false;
				return;
			}
			String version = st.nextToken(ENDPOINT_DELIMITER);
			endpointLeft = parseVersion(version, range);
			token = st.nextToken(); // consume comma
			version = st.nextToken(RIGHT_DELIMITERS);
			token = st.nextToken(); // right delim
			closedRight = RIGHT_CLOSED_DELIMITER.equals(token);
			if (!closedRight && !RIGHT_OPEN_DELIMITER.equals(token)) {
				throw new IllegalArgumentException("invalid range \"" + range + "\": invalid format");
			}
			endpointRight = parseVersion(version, range);

			if (st.hasMoreTokens()) { // any more tokens have to be whitespace
				token = st.nextToken("").trim();
				if (token.length() != 0) { // trailing whitespace
					throw new IllegalArgumentException("invalid range \"" + range + "\": invalid format");
				}
			}
		} catch (NoSuchElementException e) {
			IllegalArgumentException iae = new IllegalArgumentException("invalid range \"" + range + "\": invalid format");
			iae.initCause(e);
			throw iae;
		}

		leftClosed = closedLeft;
		rightClosed = closedRight;
		left = endpointLeft;
		right = endpointRight;
		empty = isEmpty0();
	}

	/**
	 * Parse version component into a Version.
	 * 
	 * @param version version component string
	 * @param range Complete range string for exception message, if any
	 * @return Version
	 */
	private static Version parseVersion(String version, String range) {
		try {
			return Version.parseVersion(version);
		} catch (IllegalArgumentException e) {
			IllegalArgumentException iae = new IllegalArgumentException("invalid range \"" + range + "\": " + e.getMessage());
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Returns the left endpoint of this version range.
	 * 
	 * @return The left endpoint.
	 */
	public Version getLeft() {
		return left;
	}

	/**
	 * Returns the right endpoint of this version range.
	 * 
	 * @return The right endpoint. May be {@code null} which indicates the right
	 *         endpoint is <i>Infinity</i>.
	 */
	public Version getRight() {
		return right;
	}

	/**
	 * Returns the type of the left endpoint of this version range.
	 * 
	 * @return {@link #LEFT_CLOSED} if the left endpoint is closed or
	 *         {@link #LEFT_OPEN} if the left endpoint is open.
	 */
	public char getLeftType() {
		return leftClosed ? LEFT_CLOSED : LEFT_OPEN;
	}

	/**
	 * Returns the type of the right endpoint of this version range.
	 * 
	 * @return {@link #RIGHT_CLOSED} if the right endpoint is closed or
	 *         {@link #RIGHT_OPEN} if the right endpoint is open.
	 */
	public char getRightType() {
		return rightClosed ? RIGHT_CLOSED : RIGHT_OPEN;
	}

	/**
	 * Returns whether this version range includes the specified version.
	 * 
	 * @param version The version to test for inclusion in this version range.
	 * @return {@code true} if the specified version is included in this version
	 *         range; {@code false} otherwise.
	 */
	public boolean includes(Version version) {
		if (empty) {
			return false;
		}
		if (left.compareTo(version) >= (leftClosed ? 1 : 0)) {
			return false;
		}
		if (right == null) {
			return true;
		}
		return right.compareTo(version) >= (rightClosed ? 0 : 1);
	}

	/**
	 * Returns the intersection of this version range with the specified version
	 * ranges.
	 * 
	 * @param ranges The version ranges to intersect with this version range.
	 * @return A version range representing the intersection of this version
	 *         range and the specified version ranges. If no version ranges are
	 *         specified, then this version range is returned.
	 */
	public VersionRange intersection(VersionRange... ranges) {
		if ((ranges == null) || (ranges.length == 0)) {
			return this;
		}
		// prime with data from this version range
		boolean closedLeft = leftClosed;
		boolean closedRight = rightClosed;
		Version endpointLeft = left;
		Version endpointRight = right;

		for (VersionRange range : ranges) {
			int comparison = endpointLeft.compareTo(range.left);
			if (comparison == 0) {
				closedLeft = closedLeft && range.leftClosed;
			} else {
				if (comparison < 0) { // move endpointLeft to the right
					endpointLeft = range.left;
					closedLeft = range.leftClosed;
				}
			}
			if (range.right != null) {
				if (endpointRight == null) {
					endpointRight = range.right;
					closedRight = range.rightClosed;
				} else {
					comparison = endpointRight.compareTo(range.right);
					if (comparison == 0) {
						closedRight = closedRight && range.rightClosed;
					} else {
						if (comparison > 0) { // move endpointRight to the left
							endpointRight = range.right;
							closedRight = range.rightClosed;
						}
					}
				}
			}
		}

		return new VersionRange(closedLeft ? LEFT_CLOSED : LEFT_OPEN, endpointLeft, endpointRight, closedRight ? RIGHT_CLOSED : RIGHT_OPEN);
	}

	/**
	 * Returns whether this version range is empty. A version range is empty if
	 * the set of versions defined by the interval is empty.
	 * 
	 * @return {@code true} if this version range is empty; {@code false}
	 *         otherwise.
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Internal isEmpty behavior.
	 * 
	 * @return {@code true} if this version range is empty; {@code false}
	 *         otherwise.
	 */
	private boolean isEmpty0() {
		if (right == null) { // infinity
			return false;
		}
		int comparison = left.compareTo(right);
		if (comparison == 0) { // endpoints equal
			return !leftClosed || !rightClosed;
		}
		return comparison > 0; // true if left > right
	}

	/**
	 * Returns whether this version range contains only a single version.
	 * 
	 * @return {@code true} if this version range contains only a single
	 *         version; {@code false} otherwise.
	 */
	public boolean isExact() {
		if (empty || (right == null)) {
			return false;
		}
		if (leftClosed) {
			if (rightClosed) {
				// [l,r]: exact if l == r
				return left.equals(right);
			} else {
				// [l,r): exact if l++ >= r
				Version adjacent1 = new Version(left.getMajor(), left.getMinor(), left.getMicro(), left.getQualifier() + "-");
				return adjacent1.compareTo(right) >= 0;
			}
		} else {
			if (rightClosed) {
				// (l,r] is equivalent to [l++,r]: exact if l++ == r
				Version adjacent1 = new Version(left.getMajor(), left.getMinor(), left.getMicro(), left.getQualifier() + "-");
				return adjacent1.equals(right);
			} else {
				// (l,r) is equivalent to [l++,r): exact if (l++)++ >=r
				Version adjacent2 = new Version(left.getMajor(), left.getMinor(), left.getMicro(), left.getQualifier() + "--");
				return adjacent2.compareTo(right) >= 0;
			}
		}
	}

	/**
	 * Returns the string representation of this version range.
	 * 
	 * <p>
	 * The format of the version range string will be a version string if the
	 * right end point is <i>Infinity</i> ({@code null}) or an interval string.
	 * 
	 * @return The string representation of this version range.
	 */
	public String toString() {
		if (versionRangeString != null) {
			return versionRangeString;
		}
		String leftVersion = left.toString();
		if (right == null) {
			StringBuffer result = new StringBuffer(leftVersion.length() + 1);
			result.append(left.toString0());
			return versionRangeString = result.toString();
		}
		String rightVerion = right.toString();
		StringBuffer result = new StringBuffer(leftVersion.length() + rightVerion.length() + 5);
		result.append(leftClosed ? LEFT_CLOSED : LEFT_OPEN);
		result.append(left.toString0());
		result.append(ENDPOINT_DELIMITER);
		result.append(right.toString0());
		result.append(rightClosed ? RIGHT_CLOSED : RIGHT_OPEN);
		return versionRangeString = result.toString();
	}

	/**
	 * Returns a hash code value for the object.
	 * 
	 * @return An integer which is a hash code value for this object.
	 */
	public int hashCode() {
		if (hash != 0) {
			return hash;
		}
		if (empty) {
			return hash = 31;
		}
		int h = 31 + (leftClosed ? 7 : 5);
		h = 31 * h + left.hashCode();
		if (right != null) {
			h = 31 * h + right.hashCode();
			h = 31 * h + (rightClosed ? 7 : 5);
		}
		return hash = h;
	}

	/**
	 * Compares this {@code VersionRange} object to another object.
	 * 
	 * <p>
	 * A version range is considered to be <b>equal to </b> another version
	 * range if both the endpoints and their types are equal or if both version
	 * ranges are {@link #isEmpty() empty}.
	 * 
	 * @param object The {@code VersionRange} object to be compared.
	 * @return {@code true} if {@code object} is a {@code VersionRange} and is
	 *         equal to this object; {@code false} otherwise.
	 */
	public boolean equals(Object object) {
		if (object == this) { // quicktest
			return true;
		}
		if (!(object instanceof VersionRange)) {
			return false;
		}
		VersionRange other = (VersionRange) object;
		if (empty && other.empty) {
			return true;
		}
		if (right == null) {
			return (leftClosed == other.leftClosed) && (other.right == null) && left.equals(other.left);
		}
		return (leftClosed == other.leftClosed) && (rightClosed == other.rightClosed) && left.equals(other.left) && right.equals(other.right);
	}

	/**
	 * Returns the filter string for this version range using the specified
	 * attribute name.
	 * 
	 * @param attributeName The attribute name to use in the returned filter
	 *        string.
	 * @return A filter string for this version range using the specified
	 *         attribute name.
	 * @throws IllegalArgumentException If the specified attribute name is not a
	 *         valid attribute name.
	 * 
	 * @see "Core Specification, Filters, for a description of the filter string syntax."
	 */
	public String toFilterString(String attributeName) {
		if (attributeName.length() == 0) {
			throw new IllegalArgumentException("invalid attributeName \"" + attributeName + "\"");
		}
		for (char ch : attributeName.toCharArray()) {
			if ((ch == '=') || (ch == '>') || (ch == '<') || (ch == '~') || (ch == '(') || (ch == ')')) {
				throw new IllegalArgumentException("invalid attributeName \"" + attributeName + "\"");
			}
		}

		StringBuffer result = new StringBuffer(128);
		if (right != null) {
			result.append("(&");
		}
		if (leftClosed) {
			result.append('(');
			result.append(attributeName);
			result.append(">=");
			result.append(left.toString0());
			result.append(')');
		} else {
			result.append("(!(");
			result.append(attributeName);
			result.append("<=");
			result.append(left.toString0());
			result.append("))");
		}
		if (right != null) {
			if (rightClosed) {
				result.append('(');
				result.append(attributeName);
				result.append("<=");
				result.append(right.toString0());
				result.append(')');
			} else {
				result.append("(!(");
				result.append(attributeName);
				result.append(">=");
				result.append(right.toString0());
				result.append("))");
			}
			result.append(')');
		}

		return result.toString();
	}
}
