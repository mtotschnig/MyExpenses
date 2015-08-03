/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export;

import org.totschnig.myexpenses.model.Category;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/29/12 2:29 PM
 */
public class CategoryInfo {

    public static final String SEPARATOR = ":";

//    public static String buildName(Category c) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(c.label);
//        for (Category p = c.parent; p != null; p = p.parent) {
//            sb.insert(0, SEPARATOR);
//            sb.insert(0, p.label);
//        }
//        return sb.toString();
//    }

    public String name;
    public boolean isIncome;

    private int countInserted;

    public CategoryInfo() {}

    public CategoryInfo(String name, boolean income) {
        this.name = name;
        isIncome = income;
    }

    public CategoryInfo(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CategoryInfo that = (CategoryInfo) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "{"+name+"("+(isIncome?"I":"E")+"}";
    }

    /**
     * inserts the category to the database if needed
     * @param categoryToId a map which caches the relation between the category name and the database
     *                     id, both the root and the child category are place in this map
     * @return the number of new elements added to the database
     */
    public int insert(Map<String, Long> categoryToId) {
        countInserted = 0;
        insertCategory(extractCategoryName(this.name),categoryToId);
        return countInserted;
    }

    private String extractCategoryName(String name) {
        int i = name.indexOf('/');
        if (i != -1) {
            name = name.substring(0, i);
        }
        return name;
    }

    private void insertCategory(String name, Map<String, Long> categoryToId) {
        if (isChildCategory(name)) {
            insertChildCategory(name,categoryToId);
        } else {
            insertRootCategory(name,categoryToId);
        }
    }

    private boolean isChildCategory(String name) {
        return name.contains(":");
    }

    private Long insertRootCategory(String name, Map<String, Long> categoryToId) {
        Long id = categoryToId.get(name);
        if (id == null) {
            id = maybeWriteCategory(name, null);
            if (id != -1)
                categoryToId.put(name, id);
        }
        return id;
    }
    private Long maybeWriteCategory(String name,Long parentId) {
        Long id = Category.find(name, parentId);
        if (id == -1) {
            id = Category.write(0L, name, parentId);
            if (id != -1)
                countInserted++;
        }
        return id;
    }

    private Long insertChildCategory(String name, Map<String, Long> categoryToId) {
        Long id = categoryToId.get(name);
        if (id == null) {
            int i = name.lastIndexOf(':');
            String parentCategoryName = name.substring(0, i);
            String childCategoryName = name.substring(i + 1);
            Long main = insertRootCategory(parentCategoryName, categoryToId);
            if (main != -1) {
                id = maybeWriteCategory(childCategoryName, main);
                if (id != -1)
                    categoryToId.put(name, id);
            }
        }
        return id;
    }
}
