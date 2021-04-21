/*
 * Copyright 2021 Geert Lorang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package be.lorang.nuplayer.model;

import java.util.ArrayList;
import java.util.List;

public class CategoryList {

    // Singleton instance
    private static CategoryList instance = null;
    private List<Category> categories = new ArrayList<Category>();

    private CategoryList() {}

    public static CategoryList getInstance() {
        if(instance == null) {
            instance = new CategoryList();
        }
        return instance;
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    public List<Category> getCategories() { return categories; }

    public int getCategoriesCount() { return categories.size(); }

}
