package com.reclamation.woodlands.woodlandsreclamation.Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.reclamation.woodlands.woodlandsreclamation.DB.FT_DataSource;
import com.reclamation.woodlands.woodlandsreclamation.DB.FacilityType;
import com.reclamation.woodlands.woodlandsreclamation.DB.RS_DataSource;
import com.reclamation.woodlands.woodlandsreclamation.DB.ReviewSite;
import com.reclamation.woodlands.woodlandsreclamation.DB.UI_DataSource;
import com.reclamation.woodlands.woodlandsreclamation.DB.UserInfo;
import com.reclamation.woodlands.woodlandsreclamation.R;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Jimmy on 5/11/2015.
 */
public class SyncFragment  extends Fragment implements View.OnClickListener{

    private static AsyncHttpClient client;
    private static final String url = "http://wntest.azurewebsites.net/rdata/all";
    private static UI_DataSource daoUI;
    private Activity mActivity;

    private static final String DEBUG = "debug";

    private ProgressDialog progressDialog;
    private static RS_DataSource daoRS;
    private static FT_DataSource daoFT;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sync, container, false);

        Button syncBtn =  (Button) v.findViewById(R.id.sync);
        syncBtn.setOnClickListener(this);

        Button testBtn =  (Button) v.findViewById(R.id.test);
        testBtn.setOnClickListener(this);

        Button rsBtn =  (Button) v.findViewById(R.id.rs);
        rsBtn.setOnClickListener(this);

        mActivity = this.getActivity();

        daoUI = new UI_DataSource(mActivity);
        daoRS = new RS_DataSource(mActivity);
        daoFT = new FT_DataSource(mActivity);

        client = new AsyncHttpClient();


        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.sync:
                getData();
                break;

            case R.id.test:
                daoFT.open();
                List<FacilityType> fts = daoFT.getAll();
                daoFT.close();
                if(fts != null){
                    int i = 1;
                    for(FacilityType f : fts){
                        Log.i(DEBUG, i + "..." + f.FacilityTypeName);
                        i++;
                    }
                }else{
                    Log.i(DEBUG, "No fts");
                }


                break;

            case R.id.rs:
                daoRS.open();
                List<ReviewSite> reviewSites = daoRS.getAll();
                daoRS.close();

                if(reviewSites != null){
                    for(ReviewSite r : reviewSites){
                        Log.i(DEBUG, r.ReviewSiteID);
                    }
                }else{
                    Log.i(DEBUG, "no sites");
                }
                break;
        }
    }

    private void getData(){
        daoUI.open();
        UserInfo ui = daoUI.getUserInfo();
        daoUI.close();

        if(ui != null){
            // User is logged in.
            RequestParams params = new RequestParams();
            params.put("email", ui.username);
            params.put("password", ui.password);

            progressDialog = ProgressDialog.show(mActivity, "", "Loading", true);

            client.get(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    processResult(bytes);

                    progressDialog.dismiss();
                    Toast.makeText(mActivity, "Done", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    Toast.makeText(mActivity, "Failed", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });




        }

    }

    private void processResult(byte[] bytes){
        try{
            String json = new String(bytes);

            JSONObject whole = new JSONObject(json);
            JSONArray RS = whole.getJSONArray("RS");
            JSONArray FT = whole.getJSONArray("FT");

            syncRS(RS);
            syncFT(FT);




        }catch (Exception e){
            Log.i(DEBUG, "Parsing JSON errors");
        }
    }

    private void syncRS(JSONArray rs){
        try{
            Gson gson = new Gson();
            daoRS.open();
            daoRS.deleteAll();

            for(int i=0;i<rs.length();i++){
                JSONObject site = rs.getJSONObject(i);
                ReviewSite reviewSite = gson.fromJson(site.toString(), ReviewSite.class);
//                Log.i(DEBUG, reviewSite.ReviewSiteID);
                daoRS.create(reviewSite);
            }

            daoRS.close();


        }catch (Exception e){
            Log.i(DEBUG, "Parsing RS errors");
        }
    }

    /**
     * Synchronize local FacilityType db
     * @param ft
     */
    private void syncFT(JSONArray ft){
        try {
            if (ft != null && ft.length() > 0) {
                // clear DB
                daoFT.open();
                daoFT.deleteAll();

                for (int i = 0; i < ft.length(); i++) {

                    // Add to DB
                    FacilityType facilityType = new FacilityType();
                    facilityType.FacilityTypeName = ft
                            .getJSONObject(i)
                            .getString("FacilityTypeName");

                    daoFT.create(facilityType);

                }

                daoFT.close();


            }
        }catch (Exception e){
            Log.i(DEBUG, "parsing FT errors");
        }
    }
}
