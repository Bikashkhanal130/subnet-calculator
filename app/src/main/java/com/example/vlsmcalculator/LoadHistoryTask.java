package com.example.vlsmcalculator;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

/**
 * AsyncTask for loading history from storage in background
 */
public class LoadHistoryTask extends AsyncTask<Void, Void, List<SubnetHistoryItem>> {
    private Context context;
    private HistoryLoadCallback callback;
    
    public interface HistoryLoadCallback {
        void onHistoryLoaded(List<SubnetHistoryItem> historyList);
    }
    
    public LoadHistoryTask(Context context, HistoryLoadCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }
    
    @Override
    protected List<SubnetHistoryItem> doInBackground(Void... voids) {
        return HistoryStorage.loadHistory(context);
    }
    
    @Override
    protected void onPostExecute(List<SubnetHistoryItem> historyList) {
        if (callback != null) {
            callback.onHistoryLoaded(historyList);
        }
    }
}

