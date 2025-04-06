/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.python;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.charting.dataset.IGroupsSeriesDataset;

import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.Unpickler;

// ??? rather, SeriesGroups?
public class GroupsSeriesDataset implements IGroupsSeriesDataset {

    private static class Series {
        String key;
        String title;
        double[] values;
    }

    // "columns", set of bars of the same color
    private ArrayList<Series> serieses = new ArrayList<Series>();

    // X axis labels, group names, df index
    private ArrayList<String> groupTitles = new ArrayList<String>();

    protected String generateUniqueKey() {
        int maxKey = 0;
        for (Series bs : serieses) {
            try {
                int key = Integer.parseInt(bs.key);
                if (key > maxKey)
                    maxKey = key;
            }
            catch (Exception e) {
                // ignore
            }
        }
        return Integer.toString(maxKey + 1);
    }


    public List<String> addValues(byte[] pickledData) {
        Unpickler unpickler = new Unpickler();
        List<String> keys = new ArrayList<String>();

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) unpickler.loads(pickledData);

            for (Map<String, Object> d : data) {
                Series barSeries = new Series();

                String key = (String) d.get("key");

                if (key == null)
                    key = generateUniqueKey();

                for (Series bs: serieses)
                    if (bs.key.equals(key))
                        System.out.println("WARNING: Series key '" + key + "' is not unique in GroupsSeriesDataset!");

                barSeries.key = key;
                keys.add(key);
                barSeries.title = (String) d.get("title");

                barSeries.values = ResultPicklingUtils.bytesToDoubles((byte[]) d.get("values"));

                serieses.add(barSeries);
            }
        }
        catch (PickleException | IOException e) {
            ScavePlugin.logError(e);
        }

        return keys;
    }

    @Override
    public int getSeriesCount() {
        return serieses.size();
    }

    @Override
    public String getSeriesKey(int series) {
        return serieses.get(series).key;
    }

    @Override
    public String getSeriesTitle(int series) {
        return serieses.get(series).title;
    }

    @Override
    public int getGroupCount() {
        int maxLength = 0;
        for (Series s : serieses)
            maxLength = Integer.max(maxLength, s.values.length);
        return !serieses.isEmpty() ? maxLength : groupTitles.size();
    }

    @Override
    public String getGroupTitle(int group) {
        return !groupTitles.isEmpty() ? groupTitles.get(group) : "";
    }

    @Override
    public void setGroupTitles(List<String> groupTitles) {
        // TODO assert length
        this.groupTitles.clear();
        this.groupTitles.addAll(groupTitles);
    }

    @Override
    public double getValue(int series, int group) {
        Series s = serieses.get(series);
        return group < s.values.length ? s.values[group] : Double.NaN;
    }

    @Override
    public String getValueAsString(int series, int group) {
        return String.format("%g", getValue(series, group));
    }

}
