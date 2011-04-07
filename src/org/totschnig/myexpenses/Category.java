package org.totschnig.myexpenses;

import java.util.*;

public class Category {
	public String name;
	private ArrayList<Category> children;
	
	public Category (String name) {
		this.name = name;
		children = new ArrayList<Category>();
	}
	public void add (Category sub) {
		children.add(sub);
	}
	public String toString() {
		return name;
	}
}
