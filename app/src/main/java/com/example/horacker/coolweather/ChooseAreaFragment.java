package com.example.horacker.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.horacker.coolweather.db.City;
import com.example.horacker.coolweather.db.County;
import com.example.horacker.coolweather.db.Province;
import com.example.horacker.coolweather.util.HttpUtil;
import com.example.horacker.coolweather.util.Utility;
import com.google.gson.annotations.Until;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by horacker on 2017/7/4.
 */

public class ChooseAreaFragment extends Fragment{
    public static final int level_province = 0;
    public static final int level_city = 1;
    public static final int level_county = 2;

    private ProgressDialog progressDialog;
    private TextView textView;
    private Button backBtn;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        textView = (TextView) view.findViewById(R.id.title_text);
        backBtn = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == level_province){
                 selectedProvince = provinceList.get(position);
                    queryCity();
                }else if(currentLevel == level_city){
                    selectedCity = cityList.get(position);
                    queryCounty();
                }else if(currentLevel == level_county){
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity)getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }

                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == level_county){
                    queryCity();
                }else if(currentLevel == level_city){
                    queryProvince();
                }
            }
        });
        queryProvince();
    }

    private void queryProvince() {
        textView.setText("中国");
        backBtn.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province province:
                 provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = level_province;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCity() {
       textView.setText(selectedProvince.getProvinceName());
        backBtn.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0){
            dataList.clear();
            for (City city :
                    cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = level_city;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");

        }
    }

    private void queryCounty() {
        textView.setText(selectedCity.getCityName());
        backBtn.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0){
            dataList.clear();
            for (County county:
                 countyList) {
                adapter.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = level_county;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type){
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               String resonseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(resonseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(resonseText, selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(resonseText, selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvince();
                            }else if ("city".equals(type)){
                                queryCity();
                            }else if("county".equals(type)){
                                queryCounty();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("加载中");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    protected void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
