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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a list of Programs, together they form the Catalog
 * This is a Singleton class that will keep the entire catalog in memory
 */
public class ProgramList {

    // Singleton instance
    private static ProgramList instance = null;
    private int seriesCount = 0;
    private int favoritesCount = 0;
    private List<Program> mPrograms = new ArrayList<Program>();

    private ProgramList() {}

    public static ProgramList getInstance() {
        if(instance == null) {
            instance = new ProgramList();
        }
        return instance;
    }

    public List<Program> getPrograms() { return mPrograms; }
    public void addProgram(Program p) {
        mPrograms.add(p);
    }

    public void clear() {
        mPrograms.clear();
        seriesCount = 0;
        favoritesCount = 0;
    }

    public void sort() {
        mPrograms.sort(Comparator.comparing(Program::getTitle));
    }

    public List<Program> getFavorites() {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.isFavorite()) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

    public void setIsFavorite(String programTitle, boolean isFavorite) {
        for(Program program : mPrograms) {
            if(program.getTitle().equals(programTitle)) {
                program.setIsFavorite(isFavorite);
                favoritesCount++;
            }
        }
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setIsSerie(String programName) {
        for(Program program : mPrograms) {
            if(program.getProgramName().equals(programName)) {
                program.setIsSerie(true);
                seriesCount++;
            }
        }
    }

    public int getSeriesCount() {
        return seriesCount;
    }

    public List<Program> getSeries() {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.isSerie()) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

    public List<Program> search(String searchText) {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.getTitle().toLowerCase().contains(searchText.toLowerCase())
                    || program.getDescription().toLowerCase().contains(searchText.toLowerCase())) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

    public Program getProgram(String title) {
        for(Program program : mPrograms) {
            if(program.getTitle().toLowerCase().equals(title.toLowerCase())) {
                return program;
            }
        }
        return null;
    }

    public List<String> getBrands() {
        List<String> result = new ArrayList<>();
        for(Program program : mPrograms) {
            String brand = program.getBrand();
            if(!result.contains(brand) && brand.length() > 0) {
                result.add(brand);
            }
        }

        Collections.sort(result);

        return result;
    }

}