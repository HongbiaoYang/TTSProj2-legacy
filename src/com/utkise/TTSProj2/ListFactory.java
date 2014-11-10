package com.utkise.TTSProj2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bill on 9/8/14.
 */
public class ListFactory {
    private final List<ItemStruct> root;

    public ListFactory(List<ItemStruct> root) {
        this.root = root;
    }

    public String[] produceTitleArray() {
        return (produceTitleArray(0));
    }

    public String[] produceTextArray() {
       return produceTextArray(0);
    }

    public Integer[] produceImageArray() {
        return produceImageArray(0);
    }

    public Integer[] produceColorArray() {
        return produceColorArray(0);
    }

    public String[] produceTitleArray(int offset) {
        int i;

        ArrayList<String> titleList = new ArrayList<String>();
        for (i = offset; i < this.root.size(); i++) {
            titleList.add(root.get(i).getTitle(MyProperties.getInstance().Language));
        }
        return titleList.toArray(new String[0]);

    }

    private String[] produceTextArray(int offset) {
        int i;

        ArrayList<String> textList = new ArrayList<String>();

        for (i = offset; i < this.root.size(); i++) {
            textList.add(root.get(i).getText(MyProperties.getInstance().Language));
        }

        return textList.toArray(new String[0]);
    }

    public Integer[] produceImageArray(int offset) {
        int i;

        ArrayList<Integer> imageList = new ArrayList<Integer>();
        for (i = offset; i < this.root.size(); i++) {
            imageList.add(root.get(i).getImageID());
        }

        return imageList.toArray(new Integer[0]);
    }

    private Integer[] produceColorArray(int offset) {
        int i;

        // color list
        ArrayList<Integer> colorList = new ArrayList<Integer>();
        for (i = offset; i < this.root.size(); i++) {
            colorList.add(root.get(i).getColorCode());
        }

        return colorList.toArray(new Integer[0]);
    }
}
