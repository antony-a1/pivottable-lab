/*
 * 
 */
package org.zkoss.pivot.lab.olap;

/**
 * 
 * @author simonpai
 */
public class StringUtil {
	
	public static String space(int n) {
		switch (n) {
		case 0:
			return "";
		case 1:
			return " ";
		case 2:
			return "  ";
		case 3:
			return "   ";
		case 4:
			return "    ";
		case 5:
			return "     ";
		case 6:
			return "      ";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++)
			sb.append(' ');
		return sb.toString();
	}
	
}
