package helpers;

import java.util.ArrayList;

public class StringHelper {

	/**
	 * Duplicates the String contains(char) method, but will not return an index
	 * within an internal java string.
	 */
	public static boolean containsOutOfString(String string, char find) {
		return indexOfOutOfString(string, find) != -1;
	}

	/**
	 * Duplicates the String indexOf(char) function, but will not return results
	 * contained within a java string within the given string.
	 */
	public static int indexOfOutOfString(String string, char find) {
		boolean inJavaString = false;
		int numBackSlashes = 0;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (!inJavaString) {
				if (cur == find) {
					return i;
				}
				if (cur == '\"') {
					inJavaString = true;
					numBackSlashes = 0;
				}
			} else {
				if (cur == '\\') {
					numBackSlashes++;
				}
				if (cur == '\"' && numBackSlashes % 2 == 0) {
					inJavaString = false;
					if (cur == find) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * Duplicates the String split(char) method, but will not split on
	 * characters within an internal java string.
	 */
	public static String[] splitOutOfString(String string, char split) {
		ArrayList<String> ret = new ArrayList<String>();
		boolean inJavaString = false;
		int numBackSlashes = 0;
		int lastSplitIndex = 0;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (!inJavaString) {
				if (cur == split) {
					ret.add(string.substring(lastSplitIndex, i));
					lastSplitIndex = i + 1;
				}
				if (cur == '\"') {
					inJavaString = true;
					numBackSlashes = 0;
				}
			} else {
				if (cur == '\\') {
					numBackSlashes++;
				}
				if (cur == '\"' && numBackSlashes % 2 == 0) {
					inJavaString = false;
					if (cur == split) {
						ret.add(string.substring(lastSplitIndex, i));
						lastSplitIndex = i + 1;
					}
				}
			}
		}
		if (lastSplitIndex < string.length()) {
			ret.add(string.substring(lastSplitIndex, string.length()));
		}
		String[] retArray = new String[ret.size()];
		for (int i = 0; i < ret.size(); i++) {
			retArray[i] = ret.get(i);
		}
		return retArray;
	}

	public static boolean isInteger(String s) {
		for (int i = 0; i < s.length(); i++) {
			if ((i != 0 || s.charAt(i) != '-')
					&& !Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static String[] splitOutOfArrows(String string, char split) {
		ArrayList<String> ret = new ArrayList<String>();
		int lastSplitIndex = 0;
		int arrowDepth = 0;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (arrowDepth == 0) {
				if (cur == split) {
					ret.add(string.substring(lastSplitIndex, i));
					lastSplitIndex = i + 1;
				}
			} else {
				if (cur == '>') {
					arrowDepth--;
					if (cur == split) {
						ret.add(string.substring(lastSplitIndex, i));
						lastSplitIndex = i + 1;
					}
				}
			}
			if (cur == '<') {
				arrowDepth++;
			}
		}
		if (lastSplitIndex < string.length()) {
			ret.add(string.substring(lastSplitIndex, string.length()));
		}
		String[] retArray = new String[ret.size()];
		for (int i = 0; i < ret.size(); i++) {
			retArray[i] = ret.get(i);
		}
		return retArray;
	}

	public static String getIdentifier(String string, int startIndex) {
		int currentIndex = startIndex;
		char cur = string.charAt(currentIndex);
		if (!Character.isLetter(cur)) {
			return "";
		}
		while (Character.isLetter(cur) || Character.isDigit(cur) || cur == '_') {
			cur = string.charAt(++currentIndex);
		}
		return string.substring(startIndex, currentIndex);
	}

	public static String removeComments(String string) {
		boolean inSingleQuoteString = false;
		boolean inDoubleQuoteString = false;
		boolean inComment = false;
		String ret = "";
		int lastCut = 0;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (inComment) {
				if (cur == ')' && string.charAt(i - 1) == '*') {
					inComment = false;
					lastCut = i + 1;
				}
			} else {
				if (inSingleQuoteString) {
					if (cur == '\'') {
						inSingleQuoteString = false;
					}
				} else {
					if (inDoubleQuoteString) {
						if (cur == '\"') {
							inDoubleQuoteString = false;
						}
					} else {
						if (cur == '\'') {
							inSingleQuoteString = true;
						} else if (cur == '\"') {
							inDoubleQuoteString = true;
						}
						if (cur == '(' && i < string.length() - 1
								&& string.charAt(i + 1) == '*') {
							inComment = true;
							ret += string.substring(lastCut, i);
						}
					}
				}
			}
		}
		ret += string.substring(lastCut, string.length());
		return ret;
	}

	/**
	 * Performs the same functions as the String split() command, but will not
	 * split things within strings ("",'') or parentheses ([], (), {}). Note
	 * that these strings cannot be used as splitters for this reason.
	 */
	public static ArrayList<String> splitOutOfGroupings(String string,
			char split) {
		int depth = 0;
		ArrayList<String> ret = new ArrayList<String>();
		int lastSplitIndex = 0;
		boolean inSingleQuoteString = false;
		boolean inDoubleQuoteString = false;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (inSingleQuoteString) {
				if (cur == '\'') {
					inSingleQuoteString = false;
				}
			} else {
				if (inDoubleQuoteString) {
					if (cur == '\"') {
						inDoubleQuoteString = false;
					}
				} else {
					if (cur == '\'') {
						inSingleQuoteString = true;
					} else if (cur == '\"') {
						inDoubleQuoteString = true;
					} else {
						if (cur == '[' || cur == '{' || cur == '(') {
							depth++;
						} else if (cur == ']' || cur == '}' || cur == ')') {
							depth--;
						}
						if (depth == 0 && cur == split) {
							ret.add(string.substring(lastSplitIndex, i));
							lastSplitIndex = i + 1;
						}
					}
				}
			}
		}
		ret.add(string.substring(lastSplitIndex, string.length()));
		return ret;
	}

	public static String removeWhitespaceOutOfQuotes(String string) {
		boolean inSingleQuoteString = false;
		boolean inDoubleQuoteString = false;
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (inSingleQuoteString) {
				if (cur == '\'') {
					inSingleQuoteString = false;
				}
			} else {
				if (inDoubleQuoteString) {
					if (cur == '\"') {
						inDoubleQuoteString = false;
					}
				} else {
					if (cur == '\'') {
						inSingleQuoteString = true;
					} else if (cur == '\"') {
						inDoubleQuoteString = true;
					}
				}
			}
			if (inDoubleQuoteString
					|| inSingleQuoteString
					|| (cur != ' ' && cur != '\t' && cur != '\n' && cur != '\r')) {
				ret.append(cur);
			}
		}
		return ret.toString();
	}
}
